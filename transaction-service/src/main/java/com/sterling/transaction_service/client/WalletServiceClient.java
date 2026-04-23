package com.sterling.transaction_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Feign client pointing to wallet-service.
// Transaction service uses this to:
// 1. Deduct balance from sender's wallet
// 2. Credit balance to receiver's wallet
// The method signatures must EXACTLY match
// the endpoints in WalletController.
@FeignClient(name = "wallet-service")
public interface WalletServiceClient {

    // Matches: POST /wallet/deduct?userId=1&amount=200.00
    @PostMapping("/wallet/deduct")
    String deductBalance(@RequestParam Long userId,
                         @RequestParam java.math.BigDecimal amount);

    // Matches: POST /wallet/credit?userId=2&amount=200.00
    @PostMapping("/wallet/credit")
    String creditBalance(@RequestParam Long userId,
                         @RequestParam java.math.BigDecimal amount);

    // Matches: GET /wallet/{userId}
    // Used to check sender's balance before attempting transfer
    @org.springframework.web.bind.annotation.GetMapping("/wallet/{userId}")
    String getWallet(@PathVariable Long userId);
}