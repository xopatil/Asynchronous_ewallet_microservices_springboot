package com.sterling.wallet_service.messaging;

import com.sterling.wallet_service.dto.WalletUpdateMessage;
import com.sterling.wallet_service.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// WalletUpdateListener listens on wallet.update.queue.
// When Transaction Service publishes a transfer message,
// this method is called AUTOMATICALLY by Spring.
// It calls the existing WalletService methods — no changes to WalletService.
// After processing → sends ACK back via AckPublisher.
@Slf4j
@Component
public class WalletUpdateListener {

    @Autowired
    private WalletService walletService;

    @Autowired
    private AckPublisher ackPublisher;

    // @RabbitListener: Spring creates a background thread that
    // continuously listens to wallet.update.queue.
    // When a message arrives → Spring converts JSON → WalletUpdateMessage
    // automatically (using our Jackson converter) → calls this method.
    // This is the ASYNCHRONOUS part — this runs completely separately
    // from the HTTP request that triggered the transfer.
    @RabbitListener(queues = "${rabbitmq.queue.wallet-update}")
    public void handleWalletUpdate(WalletUpdateMessage message) {
        log.info("RabbitMQ message received. TransactionId: {}, " +
                        "Sender: {}, Receiver: {}, Amount: {}, Type: {}",
                message.getTransactionId(),
                message.getSenderUserId(),
                message.getReceiverUserId(),
                message.getAmount(),
                message.getTransactionType());

        try {
            // Call existing WalletService.deductBalance() — UNCHANGED
            walletService.deductBalance(
                    message.getSenderUserId(), message.getAmount());

            // Call existing WalletService.creditBalance() — UNCHANGED
            walletService.creditBalance(
                    message.getReceiverUserId(), message.getAmount());

            log.info("Wallet update SUCCESS via RabbitMQ for transactionId: {}",
                    message.getTransactionId());

            // Send ACK back to Transaction Service
            // so it can delete the outbox row
            ackPublisher.sendAck(message.getTransactionId());

        } catch (Exception e) {
            // Log the error — don't throw because that would cause
            // RabbitMQ to re-queue the message and retry infinitely.
            // In production you'd send to a Dead Letter Queue (DLQ) here.
            log.error("Wallet update FAILED for transactionId: {}. Error: {}",
                    message.getTransactionId(), e.getMessage(), e);
        }
    }
}