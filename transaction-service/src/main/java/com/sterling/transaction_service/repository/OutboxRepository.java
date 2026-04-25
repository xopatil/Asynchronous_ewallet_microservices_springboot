package com.sterling.transaction_service.repository;

import com.sterling.transaction_service.model.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    // BacklogProcessor uses this to find all unsent messages
    // SELECT * FROM outbox_messages WHERE status = 'PENDING'
    List<OutboxMessage> findByStatus(String status);

    // AckListener uses this to find which row to delete
    // when an ACK arrives with a transactionId
    Optional<OutboxMessage> findByTransactionId(Long transactionId);
}