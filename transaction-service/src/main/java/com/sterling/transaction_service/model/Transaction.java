package com.sterling.transaction_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who is sending the money
    @Column(nullable = false)
    private Long senderUserId;

    // Who is receiving the money.
    // For merchant payments, this is the merchant's userId.
    @Column(nullable = false)
    private Long receiverUserId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // Type tells us what kind of transaction this was.
    // Possible values: "TRANSFER", "MERCHANT_PAYMENT"
    @Column(nullable = false)
    private String type;

    // Status tracks where the transaction is in its lifecycle.
    // Possible values: "SUCCESS", "FAILED"
    @Column(nullable = false)
    private String status;

    // Human readable description.
    // Example: "Transfer from john to jane"
    @Column
    private String description;

    // Exact timestamp of when this transaction was created.
    @Column(nullable = false)
    private LocalDateTime createdAt;
}