package com.sterling.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

// This object travels through RabbitMQ from Transaction Service
// to Wallet Service. It gets automatically converted to JSON
// by Jackson when sent, and back to Java object when received.
// IMPORTANT: Wallet Service has an identical copy of this class.
// Both must have exactly the same field names and types.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUpdateMessage {
    private Long transactionId;   // which transaction triggered this
    private Long senderUserId;    // who is sending money
    private Long receiverUserId;  // who is receiving money
    private BigDecimal amount;    // how much
    private String transactionType; // "TRANSFER" or "MERCHANT_PAYMENT"
}