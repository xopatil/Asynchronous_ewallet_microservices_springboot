package com.sterling.transaction_service.dto;

import lombok.Data;
import java.math.BigDecimal;

// Shape of request for paying a merchant.
// Functionally same as a transfer but kept separate
// because in future merchant payments may have
// extra fields like orderId, merchantCode etc.
@Data
public class MerchantPaymentRequest {
    private Long customerUserId;
    private Long merchantUserId;
    private BigDecimal amount;
    private String description;
}