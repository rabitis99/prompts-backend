package org.example.sharedprompts.infra.messaging;

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

    // Publish Failure Queue 이름 (발행 실패 시 메시지 보존)
    public static final String NOTIFICATION_PUBLISH_FAILURE_QUEUE = "notification.publish.failure.queue";

    // Publish Failure Exchange 이름
    public static final String NOTIFICATION_PUBLISH_FAILURE_EXCHANGE = "notification.publish.failure.exchange";

    // SSE Exchange 이름
    public static final String SSE_EXCHANGE = "sse.exchange";

    // SSE Queue 이름 (프롬프트 생성 SSE 알림용)
    public static final String SSE_PROMPT_CREATED_QUEUE = "sse.prompt.created.queue";

    // SSE Routing Key (프롬프트 생성)
    public static final String SSE_PROMPT_CREATED_ROUTING_KEY = "sse.prompt.created";

    // SSE Dead Letter Exchange 이름
    public static final String SSE_DLX = "sse.dlx";

    // SSE Dead Letter Queue 이름
    public static final String SSE_DLQ = "sse.dlq";

    // =========================
    // Job Processing Queue 설정
    // =========================
    // Job Exchange 이름
    public static final String JOB_EXCHANGE = "job.exchange";

    // Job Queue 이름
    public static final String JOB_QUEUE = "job.queue";

    // Job Routing Key
    public static final String JOB_ROUTING_KEY = "job.routing.key";

    // Job Dead Letter Exchange 이름
    public static final String JOB_DLX = "job.dlx";

    // Job Dead Letter Queue 이름
    public static final String JOB_DLQ = "job.dlq";

    /**
     * Topic Exchange 생성
     * - 알림 메시지를 라우팅하기 위한 Exchange
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * SSE Topic Exchange 생성
     * - SSE 알림 메시지를 라우팅하기 위한 Exchange
     */
    @Bean
    public TopicExchange sseExchange() {
        return new TopicExchange(SSE_EXCHANGE, true, false);
    }

    /**
     * SSE Dead Letter Exchange 생성
     */
    @Bean
    public DirectExchange sseDlx() {
        return new DirectExchange(SSE_DLX, true, false);
    }

    /**
     * SSE Dead Letter Queue 생성
     */
    @Bean
    public Queue sseDlq() {
        return QueueBuilder.durable(SSE_DLQ).build();
    }

    /**
     * SSE Dead Letter Queue Binding
     */
    @Bean
    public Binding sseDlqBinding() {
        return BindingBuilder
                .bind(sseDlq())
                .to(sseDlx())
                .with(SSE_DLQ);
    }

    /**
     * SSE 프롬프트 생성 Queue 생성
     * - 프롬프트 생성 시 팔로워들에게 SSE 알림을 전송하기 위한 Queue
     */
    @Bean
    public Queue ssePromptCreatedQueue() {
        return QueueBuilder.durable(SSE_PROMPT_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", SSE_DLX)
                .withArgument("x-dead-letter-routing-key", SSE_DLQ)
                .withArgument("x-message-ttl", 86400000L) // 24시간
                .build();
    }

    /**
     * SSE 프롬프트 생성 Binding
     */
    @Bean
    public Binding ssePromptCreatedBinding() {
        return BindingBuilder
                .bind(ssePromptCreatedQueue())
                .to(sseExchange())
                .with(SSE_PROMPT_CREATED_ROUTING_KEY);
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
     * Publish Failure Exchange 생성
     * - 발행 실패한 메시지를 보존하기 위한 Exchange
     */
    @Bean
    public DirectExchange notificationPublishFailureExchange() {
        return new DirectExchange(NOTIFICATION_PUBLISH_FAILURE_EXCHANGE, true, false);
    }

    /**
     * Publish Failure Queue 생성
     * - 발행 실패한 메시지를 저장하는 Queue
     * - 나중에 재시도하거나 분석할 수 있도록 보존
     */
    @Bean
    public Queue notificationPublishFailureQueue() {
        return QueueBuilder.durable(NOTIFICATION_PUBLISH_FAILURE_QUEUE).build();
    }

    /**
     * Publish Failure Queue Binding
     */
    @Bean
    public Binding notificationPublishFailureQueueBinding() {
        return BindingBuilder
                .bind(notificationPublishFailureQueue())
                .to(notificationPublishFailureExchange())
                .with(NOTIFICATION_PUBLISH_FAILURE_QUEUE);
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
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public TopicExchange jobExchange() {
        return new TopicExchange(JOB_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange jobDlx() {
        return new DirectExchange(JOB_DLX, true, false);
    }

    @Bean
    public Queue jobDlq() {
        return QueueBuilder.durable(JOB_DLQ).build();
    }

    @Bean
    public Binding jobDlqBinding() {
        return BindingBuilder
                .bind(jobDlq())
                .to(jobDlx())
                .with(JOB_DLQ);
    }

    @Bean
    public Queue jobQueue() {
        return QueueBuilder.durable(JOB_QUEUE)
                .withArgument("x-dead-letter-exchange", JOB_DLX)
                .withArgument("x-dead-letter-routing-key", JOB_DLQ)
                .withArgument("x-message-ttl", 86400000L)
                .build();
    }

    @Bean
    public Binding jobBinding() {
        return BindingBuilder
                .bind(jobQueue())
                .to(jobExchange())
                .with(JOB_ROUTING_KEY);
    }

    /**
     * P1-1: Job Worker 전용 Container Factory
     * - prefetchCount=1: AI 호출이 블로킹되므로 한 번에 하나씩만 처리
     * - concurrentConsumers=3~10: AI executor의 thread pool 크기(aiCallTaskExecutor: core=10, max=50)와 조정 필요
     * - WebClient.block()으로 인해 consumer 스레드가 블로킹되므로, prefetchCount를 낮게 유지하여 스레드 고갈 방지
     * - TODO: WebClient.block() 제거 및 reactive pipeline 전환 시 prefetchCount 증가 가능 (P2-1)
     */
    @Bean("jobWorkerContainerFactory")
    public SimpleRabbitListenerContainerFactory jobWorkerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(1); // P1-1: AI 호출 블로킹으로 인해 낮게 유지
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10); // P1-1: aiCallTaskExecutor의 max(50)보다 낮게 설정하여 여유 확보
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}

