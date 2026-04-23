package com.sterling.transaction_service.dto;

import lombok.Data;
import java.math.BigDecimal;

// Shape of request for transferring money between two users.
// Client sends:
// {
//   "senderUserId": 1,
//   "receiverUserId": 2,
//   "amount": 200.00,
//   "description": "Paying back john"
// }
@Data
public class TransferRequest {
    private Long senderUserId;
    private Long receiverUserId;
    private BigDecimal amount;
    private String description;
}