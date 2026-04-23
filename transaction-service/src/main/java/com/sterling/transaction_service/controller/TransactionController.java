package com.sterling.transaction_service.controller;

import com.sterling.transaction_service.dto.*;
import com.sterling.transaction_service.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    // POST /transactions/transfer
    // Body: { "senderUserId": 1, "receiverUserId": 2,
    //         "amount": 200.00, "description": "Paying back" }
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestBody TransferRequest request) {
        log.info("POST /transactions/transfer called. Sender: {}, Receiver: {}, Amount: {}",
                request.getSenderUserId(),
                request.getReceiverUserId(),
                request.getAmount());
        try {
            TransactionResponse response = transactionService.transfer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Transfer request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // POST /transactions/merchant-payment
    // Body: { "customerUserId": 1, "merchantUserId": 3,
    //         "amount": 150.00, "description": "Coffee shop" }
    @PostMapping("/merchant-payment")
    public ResponseEntity<TransactionResponse> merchantPayment(
            @RequestBody MerchantPaymentRequest request) {
        log.info("POST /transactions/merchant-payment called. Customer: {}, Merchant: {}, Amount: {}",
                request.getCustomerUserId(),
                request.getMerchantUserId(),
                request.getAmount());
        try {
            TransactionResponse response = transactionService.merchantPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Merchant payment request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // GET /transactions/history/{userId}
    // Returns all transactions (sent + received) for a user
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @PathVariable Long userId) {
        log.info("GET /transactions/history/{} called", userId);
        try {
            List<TransactionResponse> history =
                    transactionService.getTransactionHistory(userId);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            log.error("Failed to fetch history for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // GET /transactions/{transactionId}
    // Returns one specific transaction
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long transactionId) {
        log.info("GET /transactions/{} called", transactionId);
        try {
            TransactionResponse response =
                    transactionService.getTransactionById(transactionId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Transaction not found: {}", transactionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service is running");
    }
}