package com.sterling.transaction_service.messaging;

import com.sterling.transaction_service.dto.WalletUpdateMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// WalletUpdatePublisher is responsible for sending messages
// TO RabbitMQ's wallet.update.queue.
// BacklogProcessor calls this after reading PENDING outbox rows.
@Slf4j
@Component
public class WalletUpdatePublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.wallet-update}")
    private String walletUpdateRoutingKey;

    // publish: sends a WalletUpdateMessage to RabbitMQ.
    // RabbitTemplate.convertAndSend() does three things automatically:
    //   1. Converts WalletUpdateMessage object → JSON string
    //   2. Wraps it in a RabbitMQ message with headers
    //   3. Sends to the exchange with the routing key
    // The exchange then routes it to wallet.update.queue.
    public void publish(WalletUpdateMessage message) {
        log.info("Publishing to RabbitMQ. TransactionId: {}, Sender: {}, " +
                        "Receiver: {}, Amount: {}",
                message.getTransactionId(),
                message.getSenderUserId(),
                message.getReceiverUserId(),
                message.getAmount());

        rabbitTemplate.convertAndSend(exchange, walletUpdateRoutingKey, message);

        log.info("Message published to RabbitMQ for transactionId: {}",
                message.getTransactionId());
    }
}