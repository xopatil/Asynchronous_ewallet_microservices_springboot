package com.sterling.transaction_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// The Outbox Table — every message that needs to go to RabbitMQ
// gets stored here FIRST before being sent.
// This guarantees zero message loss. Even if RabbitMQ is temporarily
// down, the row stays PENDING and BacklogProcessor retries.
// Only deleted after Wallet Service sends an ACK confirming
// it successfully processed the transfer.
@Entity
@Table(name = "outbox_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links to the transaction this message belongs to.
    // When ACK arrives with transactionId → find this row → delete it.
    @Column(nullable = false)
    private Long transactionId;

    // Full JSON of WalletUpdateMessage stored as a string.
    // BacklogProcessor reads this and publishes it to RabbitMQ.
    @Column(nullable = false, length = 2000)
    private String payload;

    // Status lifecycle:
    // PENDING → created, not yet sent to RabbitMQ
    // SENT    → published to RabbitMQ, waiting for ACK
    // ACKED   → Wallet Service confirmed processing → row gets deleted
    @Column(nullable = false)
    private String status;

    // How many times BacklogProcessor tried to send this.
    // After 5 retries with no success → log ERROR for manual review.
    @Column(nullable = false)
    private int retries = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}