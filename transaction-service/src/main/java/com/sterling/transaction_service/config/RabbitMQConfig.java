package com.sterling.transaction_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// This class sets up all RabbitMQ infrastructure:
// queues, exchange, bindings, and message converter.
// Spring reads @Bean methods here at startup and creates everything
// in RabbitMQ automatically. You don't need to create queues manually
// in the dashboard — this code does it.
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.wallet-update}")
    private String walletUpdateQueue;

    @Value("${rabbitmq.queue.ack}")
    private String ackQueue;

    @Value("${rabbitmq.routing-key.wallet-update}")
    private String walletUpdateRoutingKey;

    @Value("${rabbitmq.routing-key.ack}")
    private String ackRoutingKey;

    // Queue 1: wallet.update.queue
    // durable=true means this queue SURVIVES RabbitMQ restarts.
    // If false, queue disappears when RabbitMQ is restarted — bad for production.
    @Bean
    public Queue walletUpdateQueue() {
        return new Queue(walletUpdateQueue, true);
    }

    // Queue 2: ack.queue
    // Transaction Service listens on this queue for ACKs from Wallet Service.
    @Bean
    public Queue ackQueue() {
        return new Queue(ackQueue, true);
    }

    // DirectExchange: routes messages based on exact routing key match.
    // "wallet.update" routing key → wallet.update.queue
    // "ack.response" routing key  → ack.queue
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    // Binding 1: connects wallet.update.queue to exchange
    // via routing key "wallet.update"
    @Bean
    public Binding walletUpdateBinding() {
        return BindingBuilder
                .bind(walletUpdateQueue())
                .to(exchange())
                .with(walletUpdateRoutingKey);
    }

    // Binding 2: connects ack.queue to exchange
    // via routing key "ack.response"
    @Bean
    public Binding ackBinding() {
        return BindingBuilder
                .bind(ackQueue())
                .to(exchange())
                .with(ackRoutingKey);
    }

    // Jackson2JsonMessageConverter: automatically converts
    // Java objects → JSON when sending (RabbitTemplate.convertAndSend)
    // JSON → Java objects when receiving (@RabbitListener)
    // Without this, you'd get raw bytes and have to serialize manually.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate: the main tool for SENDING messages to RabbitMQ.
    // We configure it with our JSON converter so objects auto-convert.
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}