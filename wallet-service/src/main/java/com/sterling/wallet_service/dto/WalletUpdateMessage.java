package com.sterling.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

// Identical to the Transaction Service version.
// Must have exactly the same field names —
// Jackson uses field names to deserialize JSON.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUpdateMessage {
    private Long transactionId;
    private Long senderUserId;
    private Long receiverUserId;
    private BigDecimal amount;
    private String transactionType;
}