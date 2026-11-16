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
                                          @Value("${spring.kafka.consumer.group-id}") String groupId) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(deadLetterTopic(record.topic(), groupId), -1));

        var handler = new DefaultErrorHandler(recoverer, retryBackOff());
        handler.addNotRetryableExceptions(JacksonException.class, InvalidNotificationException.class);
        handler.setRetryListeners(loggingListener(groupId));
        return handler;
    }

    @Bean
    NewTopic notificationsDeadLetterTopic(@Value("${kafka.topics.notifications}") String topic,
                                          @Value("${spring.kafka.consumer.group-id}") String groupId) {
        return TopicBuilder.name(deadLetterTopic(topic, groupId)).partitions(1).build();
    }

    private static RetryListener loggingListener(String groupId) {
        return new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                log.warn("Attempt {} failed for {}: {}", deliveryAttempt, KafkaUtils.format(record), ex.getMessage());
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.error("Dead-lettered {} to {}", KafkaUtils.format(record), deadLetterTopic(record.topic(), groupId), ex);
            }
        };
    }

    static BackOff retryBackOff() {
        var backOff = new ExponentialBackOff(1_000, 2.0);
        backOff.setJitter(500);
        backOff.setMaxAttempts(4);
        return backOff;
    }

    static String deadLetterTopic(String topic, String groupId) {
        return topic + "." + groupId + ".DLT";
    }
}
