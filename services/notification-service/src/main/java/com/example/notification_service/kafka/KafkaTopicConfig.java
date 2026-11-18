package com.example.notification_service.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    static final String DEAD_LETTER_SUFFIX = ".DLT";

    @Bean
    NewTopic notificationsTopic(@Value("${kafka.topics.notifications}") String topic) {
        return TopicBuilder.name(topic).partitions(3).build();
    }

    @Bean
    NewTopic notificationsDeadLetterTopic(@Value("${kafka.topics.notifications}") String topic) {
        return TopicBuilder.name(topic + DEAD_LETTER_SUFFIX).partitions(1).build();
    }

    @Bean
    NewTopic userEventsDeadLetterTopic(@Value("${kafka.topics.user-events}") String topic) {
        return TopicBuilder.name(topic + DEAD_LETTER_SUFFIX).partitions(1).build();
    }
}
