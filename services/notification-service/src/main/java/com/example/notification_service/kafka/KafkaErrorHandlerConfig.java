package com.example.notification_service.kafka;

import co.novu.models.errors.NovuException;
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
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Slf4j
@Configuration
public class KafkaErrorHandlerConfig {

    // Retries failed records, then publishes them to <topic>.<group>.DLT instead of dropping them.
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaTemplate,
                                          @Value("${spring.kafka.consumer.group-id}") String groupId) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(deadLetterTopic(record.topic(), groupId), -1));

        var handler = new DefaultErrorHandler(recoverer, retryBackOff());
        // Permanent errors skip retries and go straight to the DLT.
        handler.setBackOffFunction((record, ex) -> isPermanent(ex) ? new FixedBackOff(0, 0) : null);
        handler.setRetryListeners(loggingListener(groupId));
        return handler;
    }

    @Bean
    NewTopic userEventsDeadLetterTopic(KafkaTopics topics, @Value("${spring.kafka.consumer.group-id}") String groupId) {
        return TopicBuilder.name(deadLetterTopic(topics.userEvents(), groupId)).partitions(1).build();
    }

    private static RetryListener loggingListener(String groupId) {
        return new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                if (!isPermanent(ex)) {
                    log.warn("Attempt {} failed for {}: {}", deliveryAttempt, location(record), reason(ex));
                }
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.warn("Dead-lettered {} to {}: {}",
                        location(record), deadLetterTopic(record.topic(), groupId), reason(ex));
            }
        };
    }

    static String location(ConsumerRecord<?, ?> record) {
        return record.topic() + "-" + record.partition() + "@" + record.offset();
    }

    static String reason(Throwable ex) {
        Throwable root = ex;
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof NovuException novu) {
                String body = novu.bodyAsString().orElse("");
                return "Novu " + novu.code() + " " + (body.length() > 300 ? body.substring(0, 300) + "..." : body);
            }
            root = t;
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    // Waits about 1s, 2s, 4s, 8s between retries, randomised so consumers don't retry in lockstep.
    static BackOff retryBackOff() {
        var backOff = new ExponentialBackOff(1_000, 2.0);
        backOff.setJitter(500);
        backOff.setMaxAttempts(4);
        return backOff;
    }

    static String deadLetterTopic(String topic, String groupId) {
        return topic + "." + groupId + ".DLT";
    }

    // Errors that will fail again on retry: bad JSON, missing fields, Novu 4xx (except 408/409/429).
    static boolean isPermanent(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof JacksonException || t instanceof IllegalArgumentException) {
                return true;
            }
            if (t instanceof NovuException novu) {
                int code = novu.code();
                return code >= 400 && code < 500 && code != 408 && code != 409 && code != 429;
            }
        }
        return false;
    }
}
