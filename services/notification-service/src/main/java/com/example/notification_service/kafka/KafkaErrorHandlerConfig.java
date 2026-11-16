package com.example.notification_service.kafka;

import com.example.notification_service.notification.InvalidNotificationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.KafkaUtils;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.core.JacksonException;

@Slf4j
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaTemplate,
                                          @Value("${kafka.topics.notifications}.DLT") String deadLetterTopic) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(deadLetterTopic, -1));

        var handler = new DefaultErrorHandler(recoverer, retryBackOff());
        handler.addNotRetryableExceptions(JacksonException.class, InvalidNotificationException.class);
        handler.setRetryListeners(loggingListener(deadLetterTopic));
        return handler;
    }

    @Bean
    NewTopic notificationsDeadLetterTopic(@Value("${kafka.topics.notifications}.DLT") String deadLetterTopic) {
        return TopicBuilder.name(deadLetterTopic).partitions(1).build();
    }

    private static RetryListener loggingListener(String deadLetterTopic) {
        return new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                log.warn("Attempt {} failed for {}: {}", deliveryAttempt, KafkaUtils.format(record), ex.getMessage());
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.error("Dead-lettered {} to {}", KafkaUtils.format(record), deadLetterTopic, ex);
            }
        };
    }

    static BackOff retryBackOff() {
        var backOff = new ExponentialBackOff(1_000, 2.0);
        backOff.setJitter(500);
        backOff.setMaxAttempts(4);
        return backOff;
    }
}
