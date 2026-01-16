package org.example.sharedprompts.global.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 설정
 * - 알림 이벤트를 비동기로 처리하기 위한 메시지 큐 설정
 */
@Configuration
public class RabbitMQConfig {

    // Exchange 이름
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    // Queue 이름
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    // Routing Key
    public static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";

    // Dead Letter Exchange 이름
    public static final String NOTIFICATION_DLX = "notification.dlx";

    // Dead Letter Queue 이름
    public static final String NOTIFICATION_DLQ = "notification.dlq";

    /**
     * Topic Exchange 생성
     * - 알림 메시지를 라우팅하기 위한 Exchange
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * Dead Letter Exchange 생성
     * - 실패한 메시지를 받는 Exchange
     */
    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    /**
     * Dead Letter Queue 생성
     * - 실패한 메시지를 저장하는 Queue
     */
    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    /**
     * Dead Letter Queue Binding
     */
    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder
                .bind(notificationDlq())
                .to(notificationDlx())
                .with(NOTIFICATION_DLQ);
    }

    /**
     * Queue 생성
     * - 알림 메시지를 저장하는 Queue
     * - durable: true (서버 재시작 시에도 유지)
     * - Dead Letter Exchange 설정: 실패한 메시지는 DLQ로 전달
     * - 메시지 TTL 설정: 24시간 (86400000ms) - 오래된 메시지 자동 삭제
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .withArgument("x-message-ttl", 86400000L) // 24시간
                .build();
    }

    /**
     * Binding 생성
     * - Exchange와 Queue를 연결
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * JSON 메시지 컨버터
     * - 객체를 JSON으로 직렬화/역직렬화
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 설정
     * - 메시지 발행을 위한 템플릿
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * RabbitListener 컨테이너 팩토리 설정
     * - 메시지 수신을 위한 리스너 팩토리
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}

