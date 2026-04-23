package com.sterling.wallet_service.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId links this wallet to a user.
    // We store userId (not the full User object) because Wallet Service
    // has its OWN database — it cannot directly join with User Service's DB.
    // This is a core microservices rule: services don't share databases.
    @Column(unique = true, nullable = false)
    private Long userId;

    // username stored here for convenience — avoids calling User Service
    // every time we just need to display whose wallet this is
    @Column(nullable = false)
    private String username;

    // BigDecimal is used for money — NEVER use float or double for money.
    // float/double have precision errors: 0.1 + 0.2 = 0.30000000000000004
    // BigDecimal is exact. precision=19 digits total, scale=2 decimal places
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    // Tracks when this wallet was created
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Tracks last time balance changed
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}