package com.sterling.transaction_service.service;

import com.sterling.transaction_service.client.WalletServiceClient;
import com.sterling.transaction_service.dto.*;
import com.sterling.transaction_service.model.Transaction;
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

    // transfer: Moves money from one user's wallet to another.
    // This is the most critical method — it must be atomic in concept:
    // deduct from sender AND credit to receiver. If either fails,
    // we record a FAILED transaction so there is always an audit trail.
    public TransactionResponse transfer(TransferRequest request) {
        log.info("Transfer initiated. Sender: {}, Receiver: {}, Amount: {}",
                request.getSenderUserId(),
                request.getReceiverUserId(),
                request.getAmount());

        // Basic validation — can't transfer to yourself,
        // can't transfer zero or negative amount
        if (request.getSenderUserId().equals(request.getReceiverUserId())) {
            log.warn("Transfer rejected - sender and receiver are the same: {}",
                    request.getSenderUserId());
            throw new RuntimeException("Cannot transfer to yourself");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Transfer rejected - invalid amount: {}", request.getAmount());
            throw new RuntimeException("Transfer amount must be greater than zero");
        }

        // Create a transaction record upfront with FAILED status.
        // Why? If the app crashes midway, we still have a record.
        // We update it to SUCCESS only if everything completes.
        // This is called "pessimistic recording" — assume failure,
        // then update to success.
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
            // Step 1: Deduct from sender's wallet.
            // WalletService internally checks if balance is sufficient.
            // If insufficient → WalletService throws exception → caught below.
            log.debug("Deducting {} from senderUserId: {}",
                    request.getAmount(), request.getSenderUserId());
            walletServiceClient.deductBalance(
                    request.getSenderUserId(), request.getAmount());
            log.debug("Deduction successful for senderUserId: {}",
                    request.getSenderUserId());

            // Step 2: Credit to receiver's wallet.
            // If this fails after deduction — this is where distributed
            // transactions get complex. For your project, log the error.
            // In production, this would use a saga pattern or message queue.
            log.debug("Crediting {} to receiverUserId: {}",
                    request.getAmount(), request.getReceiverUserId());
            walletServiceClient.creditBalance(
                    request.getReceiverUserId(), request.getAmount());
            log.debug("Credit successful for receiverUserId: {}",
                    request.getReceiverUserId());

            // Both steps succeeded — mark transaction as SUCCESS
            transaction.setStatus("SUCCESS");
            Transaction saved = transactionRepository.save(transaction);

            log.info("Transfer SUCCESS. TransactionId: {}, Sender: {}, Receiver: {}, Amount: {}",
                    saved.getId(),
                    request.getSenderUserId(),
                    request.getReceiverUserId(),
                    request.getAmount());

            return mapToResponse(saved);

        } catch (Exception e) {
            // Something went wrong — save the FAILED record for audit trail
            Transaction saved = transactionRepository.save(transaction);
            log.error("Transfer FAILED. TransactionId: {}, Reason: {}",
                    saved.getId(), e.getMessage());
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }

    // merchantPayment: Customer pays a merchant.
    // Functionally identical to transfer but recorded as MERCHANT_PAYMENT type
    // so reporting and history can distinguish between the two.
    public TransactionResponse merchantPayment(MerchantPaymentRequest request) {
        log.info("Merchant payment initiated. Customer: {}, Merchant: {}, Amount: {}",
                request.getCustomerUserId(),
                request.getMerchantUserId(),
                request.getAmount());

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Merchant payment rejected - invalid amount: {}", request.getAmount());
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

            return mapToResponse(saved);

        } catch (Exception e) {
            Transaction saved = transactionRepository.save(transaction);
            log.error("Merchant payment FAILED. TransactionId: {}, Reason: {}",
                    saved.getId(), e.getMessage());
            throw new RuntimeException("Merchant payment failed: " + e.getMessage());
        }
    }

    // getTransactionHistory: Returns all transactions for a user
    // (both sent and received)
    public List<TransactionResponse> getTransactionHistory(Long userId) {
        log.info("Fetching transaction history for userId: {}", userId);

        List<Transaction> transactions = transactionRepository
                .findBySenderUserIdOrReceiverUserId(userId, userId);

        log.debug("Found {} transactions for userId: {}", transactions.size(), userId);

        // stream() + map() + collect() = Java's way of converting
        // a List<Transaction> into a List<TransactionResponse>
        // For each Transaction object, call mapToResponse() on it,
        // then collect all results into a new List.
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // getTransactionById: Fetch one specific transaction by its ID
    public TransactionResponse getTransactionById(Long transactionId) {
        log.info("Fetching transaction by id: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.error("Transaction not found with id: {}", transactionId);
                    return new RuntimeException("Transaction not found: " + transactionId);
                });

        return mapToResponse(transaction);
    }

    // mapToResponse: Converts Transaction entity → TransactionResponse DTO
    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getSenderUserId(),
                t.getReceiverUserId(),
                t.getAmount(),
                t.getType(),
                t.getStatus(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}