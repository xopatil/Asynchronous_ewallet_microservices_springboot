package com.sterling.wallet_service.dto;

import lombok.Data;
import java.math.BigDecimal;

// Shape of request when user wants to add money to wallet.
// Client sends: { "userId": 1, "amount": 500.00 }
@Data
public class TopUpRequest {
    private Long userId;
    private BigDecimal amount;
}