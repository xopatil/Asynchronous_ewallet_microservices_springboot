package com.sterling.transaction_service.repository;

import com.sterling.transaction_service.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Get all transactions where this user was the sender
    List<Transaction> findBySenderUserId(Long senderUserId);

    // Get all transactions where this user was the receiver
    List<Transaction> findByReceiverUserId(Long receiverUserId);

    // Get complete transaction history for a user —
    // both sent and received combined
    // Spring Data JPA reads "Or" in the method name and generates:
    // SELECT * FROM transactions WHERE sender_user_id = ? OR receiver_user_id = ?
    List<Transaction> findBySenderUserIdOrReceiverUserId(
            Long senderUserId, Long receiverUserId);

    // Get transactions filtered by type ("TRANSFER" or "MERCHANT_PAYMENT")
    List<Transaction> findByType(String type);
}