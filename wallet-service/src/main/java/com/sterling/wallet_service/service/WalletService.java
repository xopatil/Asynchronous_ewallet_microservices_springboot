package com.sterling.wallet_service.service;

import com.sterling.wallet_service.client.UserServiceClient;
import com.sterling.wallet_service.dto.TopUpRequest;
import com.sterling.wallet_service.dto.WalletResponse;
import com.sterling.wallet_service.model.Wallet;
import com.sterling.wallet_service.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// @Slf4j is a Lombok annotation.
// It generates this line automatically:
// private static final Logger log = LoggerFactory.getLogger(WalletService.class);
// So you can directly use log.info(), log.debug(), log.error() etc.
@Slf4j
@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    // createWallet: Called after a user registers.
    // Takes userId and username, creates a wallet with zero balance.
    public WalletResponse createWallet(Long userId, String username) {
        log.info("Attempting to create wallet for userId: {}, username: {}",
                userId, username);

        // Guard: one user should only ever have one wallet
        if (walletRepository.existsByUserId(userId)) {
            log.warn("Wallet already exists for userId: {}", userId);
            throw new RuntimeException("Wallet already exists for user: " + userId);
        }

        // Verify the user actually exists in User Service before creating wallet.
        // If User Service is down or user doesn't exist, Feign throws an exception
        // and we stop here — no orphan wallets created.
        try {
            log.debug("Verifying user exists in User Service for username: {}", username);
            userServiceClient.getUserByUsername(username);
            log.debug("User verified successfully: {}", username);
        } catch (Exception e) {
            log.error("Failed to verify user: {}. User Service may be down. Error: {}",
                    username, e.getMessage());
            throw new RuntimeException("Cannot verify user. Please try again later.");
        }

        // Build the new wallet object
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setUsername(username);

        // BigDecimal.ZERO = 0.00 — new wallets start empty
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());

        // Save to H2 database
        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet created successfully. WalletId: {}, UserId: {}",
                saved.getId(), saved.getUserId());

        return mapToResponse(saved);
    }

    // getWalletByUserId: Returns current balance and wallet info
    public WalletResponse getWalletByUserId(Long userId) {
        log.info("Fetching wallet for userId: {}", userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Wallet not found for userId: {}", userId);
                    return new RuntimeException("Wallet not found for userId: " + userId);
                });

        log.debug("Wallet found. Balance: {} for userId: {}",
                wallet.getBalance(), userId);
        return mapToResponse(wallet);
    }

    // topUp: Adds money to a wallet (like adding money via UPI/bank)
    public WalletResponse topUp(TopUpRequest request) {
        log.info("TopUp request received. UserId: {}, Amount: {}",
                request.getUserId(), request.getAmount());

        // Validate amount — cannot add zero or negative money
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid topUp amount: {} for userId: {}",
                    request.getAmount(), request.getUserId());
            throw new RuntimeException("Top-up amount must be greater than zero");
        }

        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> {
                    log.error("Wallet not found during topUp for userId: {}",
                            request.getUserId());
                    return new RuntimeException("Wallet not found");
                });

        BigDecimal oldBalance = wallet.getBalance();

        // add() returns a NEW BigDecimal — BigDecimal is immutable.
        // Never do wallet.getBalance() + amount — that's float math, wrong for money.
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        wallet.setUpdatedAt(LocalDateTime.now());

        Wallet updated = walletRepository.save(wallet);

        log.info("TopUp successful. UserId: {}, OldBalance: {}, AddedAmount: {}, NewBalance: {}",
                request.getUserId(), oldBalance, request.getAmount(), updated.getBalance());

        return mapToResponse(updated);
    }

    // deductBalance: Called by Transaction Service when money is sent.
    // This method is internal — not directly exposed to the client.
    public WalletResponse deductBalance(Long userId, BigDecimal amount) {
        log.info("Deducting {} from userId: {}", amount, userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Wallet not found during deduction for userId: {}", userId);
                    return new RuntimeException("Wallet not found");
                });

        // Check sufficient balance before deducting
        // compareTo returns -1 if balance < amount (insufficient funds)
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for userId: {}. Balance: {}, Required: {}",
                    userId, wallet.getBalance(), amount);
            throw new RuntimeException("Insufficient balance");
        }

        BigDecimal oldBalance = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setUpdatedAt(LocalDateTime.now());

        Wallet updated = walletRepository.save(wallet);

        log.info("Deduction successful. UserId: {}, OldBalance: {}, Deducted: {}, NewBalance: {}",
                userId, oldBalance, amount, updated.getBalance());

        return mapToResponse(updated);
    }

    // creditBalance: Called by Transaction Service when money is received.
    public WalletResponse creditBalance(Long userId, BigDecimal amount) {
        log.info("Crediting {} to userId: {}", amount, userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Wallet not found during credit for userId: {}", userId);
                    return new RuntimeException("Wallet not found");
                });

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());

        Wallet updated = walletRepository.save(wallet);

        log.info("Credit successful. UserId: {}, NewBalance: {}", userId, updated.getBalance());

        return mapToResponse(updated);
    }

    // mapToResponse: Converts internal Wallet entity → WalletResponse DTO.
    // Private because only this service uses it.
    // Keeps controller and service clean — they work with DTOs, not raw entities.
    private WalletResponse mapToResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getUsername(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}