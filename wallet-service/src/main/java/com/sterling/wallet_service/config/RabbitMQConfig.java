package com.sterling.wallet_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Same config as Transaction Service.
// Both services need to know about the queues and exchange
// so they can connect to the right places.
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

    @Bean
    public Queue walletUpdateQueue() {
        return new Queue(walletUpdateQueue, true);
    }

    @Bean
    public Queue ackQueue() {
        return new Queue(ackQueue, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Binding walletUpdateBinding() {
        return BindingBuilder
                .bind(walletUpdateQueue())
                .to(exchange())
                .with(walletUpdateRoutingKey);
    }

    @Bean
    public Binding ackBinding() {
        return BindingBuilder
                .bind(ackQueue())
                .to(exchange())
                .with(ackRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}