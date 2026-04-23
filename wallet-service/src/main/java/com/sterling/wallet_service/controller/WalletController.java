package com.sterling.wallet_service.controller;

import com.sterling.wallet_service.dto.TopUpRequest;
import com.sterling.wallet_service.dto.WalletResponse;
import com.sterling.wallet_service.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    // POST /wallet/create?userId=1&username=john
    // @RequestParam reads values from the URL query string (?key=value)
    // This is called by User Service after registration (or manually via Postman)
    @PostMapping("/create")
    public ResponseEntity<WalletResponse> createWallet(
            @RequestParam Long userId,
            @RequestParam String username) {

        log.info("POST /wallet/create called. UserId: {}, Username: {}", userId, username);
        try {
            WalletResponse response = walletService.createWallet(userId, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Failed to create wallet: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // GET /wallet/{userId}
    // Returns wallet info and current balance for a user
    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        log.info("GET /wallet/{} called", userId);
        try {
            WalletResponse response = walletService.getWalletByUserId(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Wallet not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // POST /wallet/topup
    // Body: { "userId": 1, "amount": 500.00 }
    @PostMapping("/topup")
    public ResponseEntity<WalletResponse> topUp(@RequestBody TopUpRequest request) {
        log.info("POST /wallet/topup called. UserId: {}, Amount: {}",
                request.getUserId(), request.getAmount());
        try {
            WalletResponse response = walletService.topUp(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("TopUp failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // POST /wallet/deduct
    // Called internally by Transaction Service — not meant for direct client use
    // @RequestParam for simple values instead of a full request body
    @PostMapping("/deduct")
    public ResponseEntity<WalletResponse> deduct(
            @RequestParam Long userId,
            @RequestParam java.math.BigDecimal amount) {

        log.info("POST /wallet/deduct called. UserId: {}, Amount: {}", userId, amount);
        try {
            WalletResponse response = walletService.deductBalance(userId, amount);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Deduction failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // POST /wallet/credit
    // Called internally by Transaction Service when receiver gets money
    @PostMapping("/credit")
    public ResponseEntity<WalletResponse> credit(
            @RequestParam Long userId,
            @RequestParam java.math.BigDecimal amount) {

        log.info("POST /wallet/credit called. UserId: {}, Amount: {}", userId, amount);
        try {
            WalletResponse response = walletService.creditBalance(userId, amount);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Credit failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Wallet Service is running");
    }
}