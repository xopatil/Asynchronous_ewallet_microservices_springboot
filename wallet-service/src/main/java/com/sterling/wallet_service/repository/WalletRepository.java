package com.sterling.wallet_service.repository;

import com.sterling.wallet_service.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Find wallet by userId → used when Transaction Service
    // needs to check/update a user's balance
    Optional<Wallet> findByUserId(Long userId);

    // Check if wallet already exists for this user →
    // prevents creating duplicate wallets
    boolean existsByUserId(Long userId);

    // Find by username → useful for display purposes
    Optional<Wallet> findByUsername(String username);
}