package com.sterling.wallet_service.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// AckPublisher sends acknowledgements to Transaction Service
// after Wallet Service successfully processes a transfer.
// The ACK tells Transaction Service: "I got your message and
// processed it — you can safely delete the outbox row."
@Slf4j
@Component
public class AckPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.ack}")
    private String ackRoutingKey;

    // sendAck: publishes the transactionId as a String to ack.queue.
    // Transaction Service's AckListener picks this up and
    // deletes the matching outbox row.
    public void sendAck(Long transactionId) {
        String ackMessage = String.valueOf(transactionId);

        rabbitTemplate.convertAndSend(exchange, ackRoutingKey, ackMessage);

        log.info("ACK published to RabbitMQ for transactionId: {}", transactionId);
    }
}