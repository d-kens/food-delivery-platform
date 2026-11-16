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
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.KafkaUtils;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;
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
        handler.setBackOffFunction((record, ex) -> isPermanent(ex) ? new FixedBackOff(0, 0) : null);
        handler.setRetryListeners(loggingListener(groupId));
        return handler;
    }

    @Bean
    KafkaAdmin.NewTopics deadLetterTopics(KafkaTopics topics, @Value("${spring.kafka.consumer.group-id}") String groupId) {
        return new KafkaAdmin.NewTopics(topics.all().stream()
                .map(topic -> TopicBuilder.name(deadLetterTopic(topic, groupId)).partitions(1).build())
                .toArray(NewTopic[]::new));
    }

    private static RetryListener loggingListener(String groupId) {
        return new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                if (!isPermanent(ex)) {
                    log.warn("Attempt {} failed for {}: {}", deliveryAttempt, KafkaUtils.format(record), reason(ex));
                }
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.warn("Dead-lettered {} to {}: {}",
                        KafkaUtils.format(record), deadLetterTopic(record.topic(), groupId), reason(ex));
            }
        };
    }

    static String reason(Throwable ex) {
        Throwable root = ex;
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof NovuException novu) {
                return "Novu " + novu.code() + " " + novu.bodyAsString().orElse("");
            }
            root = t;
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
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
