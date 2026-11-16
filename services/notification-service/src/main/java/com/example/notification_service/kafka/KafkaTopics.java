package com.example.notification_service.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("kafka.topics")
public record KafkaTopics(String userEvents) {

    public List<String> all() {
        return List.of(userEvents);
    }
}
