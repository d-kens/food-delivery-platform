package com.example.notification_service.kafka;

import com.example.notification_service.event.UserEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventsListener {

    private final JsonMapper jsonMapper;
    private final UserEventHandler userEventHandler;
    private final KafkaTopics topics;

    @KafkaListener(topics = "${kafka.topics.user-events}")
    public void onMessage(ConsumerRecord<String, String> record) {
        JsonNode event = jsonMapper.readTree(record.value());
        String eventType = required(event, "eventType");
        String eventId = required(event, "eventId");

        log.info("Received {} eventId={} topic={} key={} partition={} offset={}",
                eventType, eventId, record.topic(), record.key(), record.partition(), record.offset());

        if (record.topic().equals(topics.userEvents())) {
            userEventHandler.handle(eventType, event);
        } else {
            log.warn("No handler for topic {}", record.topic());
        }
    }

    private static String required(JsonNode event, String field) {
        JsonNode node = event.get(field);
        if (node == null || !node.isString() || node.asString().isBlank()) {
            throw new IllegalArgumentException("Event is missing " + field);
        }
        return node.asString();
    }
}
