package com.sterling.transaction_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterling.transaction_service.dto.WalletUpdateMessage;
import com.sterling.transaction_service.messaging.WalletUpdatePublisher;
import com.sterling.transaction_service.model.OutboxMessage;
import com.sterling.transaction_service.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// BacklogProcessor is the engine of the Outbox Pattern.
// It runs automatically every 5 seconds (configurable).
// Each run: finds all PENDING outbox rows → publishes to RabbitMQ
//           → marks them SENT → waits for AckListener to delete them.
@Slf4j
@Component
public class BacklogProcessor {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private WalletUpdatePublisher publisher;

    // ObjectMapper: converts JSON string stored in outbox
    // back to a WalletUpdateMessage object for publishing.
    @Autowired
    private ObjectMapper objectMapper;

    // fixedDelayString: waits X ms AFTER last execution finished
    // before running again. This prevents overlapping runs.
    // ${rabbitmq.backlog.interval} = 5000ms from application.properties
    @Scheduled(fixedDelayString = "${rabbitmq.backlog.interval}")
    public void processOutbox() {

        // Find all rows not yet sent to RabbitMQ
        List<OutboxMessage> pendingMessages =
                outboxRepository.findByStatus("PENDING");

        if (pendingMessages.isEmpty()) {
            return; // nothing to do — don't log, would spam console
        }

        log.info("BacklogProcessor: found {} PENDING message(s)",
                pendingMessages.size());

        for (OutboxMessage outbox : pendingMessages) {
            try {
                // Deserialize stored JSON back to WalletUpdateMessage
                WalletUpdateMessage message = objectMapper.readValue(
                        outbox.getPayload(), WalletUpdateMessage.class);

                // Publish to RabbitMQ
                publisher.publish(message);

                // Update status to SENT — waiting for ACK now
                outbox.setStatus("SENT");
                outbox.setRetries(outbox.getRetries() + 1);
                outbox.setUpdatedAt(LocalDateTime.now());
                outboxRepository.save(outbox);

                log.info("Outbox row id={} published. Status=SENT. transactionId={}",
                        outbox.getId(), outbox.getTransactionId());

            } catch (Exception e) {
                // Keep status PENDING so it retries next cycle
                outbox.setRetries(outbox.getRetries() + 1);
                outbox.setUpdatedAt(LocalDateTime.now());
                outboxRepository.save(outbox);

                log.error("BacklogProcessor failed for outbox id={}. " +
                                "Retries={}. Error: {}",
                        outbox.getId(), outbox.getRetries(), e.getMessage());

                if (outbox.getRetries() >= 5) {
                    log.error("ALERT: outbox id={} failed {} times. " +
                                    "TransactionId={} needs manual review",
                            outbox.getId(), outbox.getRetries(),
                            outbox.getTransactionId());
                }
            }
        }
    }
}