package com.sterling.transaction_service.messaging;

import com.sterling.transaction_service.model.OutboxMessage;
import com.sterling.transaction_service.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

// AckListener listens on the ack.queue for acknowledgements
// sent by Wallet Service after it successfully processes a transfer.
// When an ACK arrives → find the outbox row → delete it.
// This confirms end-to-end delivery: message sent, processed, acknowledged.
@Slf4j
@Component
public class AckListener {

    @Autowired
    private OutboxRepository outboxRepository;

    // @RabbitListener: Spring automatically calls this method
    // when a message arrives in ack.queue.
    // The message is a String (just the transactionId number).
    // Spring deserializes it automatically.
    // This runs in a SEPARATE THREAD managed by Spring —
    // completely independent from HTTP request threads.
    @RabbitListener(queues = "${rabbitmq.queue.ack}")
    public void handleAck(String transactionIdStr) {
        log.info("ACK received from Wallet Service for transactionId: {}",
                transactionIdStr);

        try {
            Long transactionId = Long.parseLong(transactionIdStr.trim());

            // Find the outbox row for this transaction
            Optional<OutboxMessage> outboxOpt =
                    outboxRepository.findByTransactionId(transactionId);

            if (outboxOpt.isPresent()) {
                // DELETE the row — fully acknowledged and processed
                outboxRepository.delete(outboxOpt.get());
                log.info("Outbox row DELETED for transactionId: {} — " +
                        "fully acknowledged by Wallet Service", transactionId);
            } else {
                log.warn("ACK received but outbox row not found " +
                        "for transactionId: {}", transactionId);
            }

        } catch (NumberFormatException e) {
            log.error("Invalid ACK message format: {}. Expected a transactionId number",
                    transactionIdStr);
        }
    }
}