package com.sterling.transaction_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterling.transaction_service.client.WalletServiceClient;
import com.sterling.transaction_service.dto.*;
import com.sterling.transaction_service.model.OutboxMessage;
import com.sterling.transaction_service.model.Transaction;
import com.sterling.transaction_service.repository.OutboxRepository;
import com.sterling.transaction_service.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletServiceClient walletServiceClient;

    // ---- NEW: inject outbox repository and ObjectMapper ----
    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;
    // ---- END NEW ----

    public TransactionResponse transfer(TransferRequest request) {
        log.info("Transfer initiated. Sender: {}, Receiver: {}, Amount: {}",
                request.getSenderUserId(),
                request.getReceiverUserId(),
                request.getAmount());

        if (request.getSenderUserId().equals(request.getReceiverUserId())) {
            log.warn("Transfer rejected - sender and receiver are the same: {}",
                    request.getSenderUserId());
            throw new RuntimeException("Cannot transfer to yourself");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Transfer rejected - invalid amount: {}", request.getAmount());
            throw new RuntimeException("Transfer amount must be greater than zero");
        }

        Transaction transaction = new Transaction();
        transaction.setSenderUserId(request.getSenderUserId());
        transaction.setReceiverUserId(request.getReceiverUserId());
        transaction.setAmount(request.getAmount());
        transaction.setType("TRANSFER");
        transaction.setStatus("FAILED");
        transaction.setDescription(request.getDescription() != null
                ? request.getDescription()
                : "Transfer from userId " + request.getSenderUserId()
                + " to userId " + request.getReceiverUserId());
        transaction.setCreatedAt(LocalDateTime.now());

        try {
            log.debug("Deducting {} from senderUserId: {}",
                    request.getAmount(), request.getSenderUserId());
            walletServiceClient.deductBalance(
                    request.getSenderUserId(), request.getAmount());
            log.debug("Deduction successful for senderUserId: {}",
                    request.getSenderUserId());

            log.debug("Crediting {} to receiverUserId: {}",
                    request.getAmount(), request.getReceiverUserId());
            walletServiceClient.creditBalance(
                    request.getReceiverUserId(), request.getAmount());
            log.debug("Credit successful for receiverUserId: {}",
                    request.getReceiverUserId());

            transaction.setStatus("SUCCESS");
            Transaction saved = transactionRepository.save(transaction);

            log.info("Transfer SUCCESS. TransactionId: {}, Sender: {}, " +
                            "Receiver: {}, Amount: {}",
                    saved.getId(), request.getSenderUserId(),
                    request.getReceiverUserId(), request.getAmount());

            // ---- NEW: write to outbox after successful transfer ----
            writeToOutbox(saved.getId(), request.getSenderUserId(),
                    request.getReceiverUserId(),
                    request.getAmount(), "TRANSFER");
            // ---- END NEW ----

            return mapToResponse(saved);

        } catch (Exception e) {
            Transaction saved = transactionRepository.save(transaction);
            log.error("Transfer FAILED. TransactionId: {}, Reason: {}",
                    saved.getId(), e.getMessage());
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }

    public TransactionResponse merchantPayment(MerchantPaymentRequest request) {
        log.info("Merchant payment initiated. Customer: {}, Merchant: {}, Amount: {}",
                request.getCustomerUserId(),
                request.getMerchantUserId(),
                request.getAmount());

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Merchant payment rejected - invalid amount: {}",
                    request.getAmount());
            throw new RuntimeException("Payment amount must be greater than zero");
        }

        Transaction transaction = new Transaction();
        transaction.setSenderUserId(request.getCustomerUserId());
        transaction.setReceiverUserId(request.getMerchantUserId());
        transaction.setAmount(request.getAmount());
        transaction.setType("MERCHANT_PAYMENT");
        transaction.setStatus("FAILED");
        transaction.setDescription(request.getDescription() != null
                ? request.getDescription()
                : "Merchant payment from userId " + request.getCustomerUserId());
        transaction.setCreatedAt(LocalDateTime.now());

        try {
            log.debug("Processing merchant payment deduction for customerId: {}",
                    request.getCustomerUserId());
            walletServiceClient.deductBalance(
                    request.getCustomerUserId(), request.getAmount());

            walletServiceClient.creditBalance(
                    request.getMerchantUserId(), request.getAmount());

            transaction.setStatus("SUCCESS");
            Transaction saved = transactionRepository.save(transaction);

            log.info("Merchant payment SUCCESS. TransactionId: {}, Amount: {}",
                    saved.getId(), request.getAmount());

            // ---- NEW: write to outbox ----
            writeToOutbox(saved.getId(), request.getCustomerUserId(),
                    request.getMerchantUserId(),
                    request.getAmount(), "MERCHANT_PAYMENT");
            // ---- END NEW ----

            return mapToResponse(saved);

        } catch (Exception e) {
            Transaction saved = transactionRepository.save(transaction);
            log.error("Merchant payment FAILED. TransactionId: {}, Reason: {}",
                    saved.getId(), e.getMessage());
            throw new RuntimeException("Merchant payment failed: " + e.getMessage());
        }
    }

    // ---- NEW: private helper to write outbox row ----
    private void writeToOutbox(Long transactionId, Long senderUserId,
                               Long receiverUserId, BigDecimal amount,
                               String type) {
        try {
            WalletUpdateMessage message = new WalletUpdateMessage(
                    transactionId, senderUserId, receiverUserId, amount, type);

            // Serialize to JSON for storage in outbox DB
            String payload = objectMapper.writeValueAsString(message);

            OutboxMessage outbox = new OutboxMessage();
            outbox.setTransactionId(transactionId);
            outbox.setPayload(payload);
            outbox.setStatus("PENDING");
            outbox.setRetries(0);
            outbox.setCreatedAt(LocalDateTime.now());
            outbox.setUpdatedAt(LocalDateTime.now());

            outboxRepository.save(outbox);

            log.info("Outbox row written. TransactionId: {} Status: PENDING",
                    transactionId);

        } catch (JsonProcessingException e) {
            // Transfer already succeeded — don't fail it because of outbox issue.
            // Log it so someone can investigate.
            log.error("Failed to write outbox for transactionId: {}. Error: {}",
                    transactionId, e.getMessage(), e);
        }
    }
    // ---- END NEW ----

    public List<TransactionResponse> getTransactionHistory(Long userId) {
        log.info("Fetching transaction history for userId: {}", userId);
        List<Transaction> transactions = transactionRepository
                .findBySenderUserIdOrReceiverUserId(userId, userId);
        log.debug("Found {} transactions for userId: {}", transactions.size(), userId);
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long transactionId) {
        log.info("Fetching transaction by id: {}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.error("Transaction not found with id: {}", transactionId);
                    return new RuntimeException("Transaction not found: " + transactionId);
                });
        return mapToResponse(transaction);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getSenderUserId(), t.getReceiverUserId(),
                t.getAmount(), t.getType(), t.getStatus(),
                t.getDescription(), t.getCreatedAt());
    }
}