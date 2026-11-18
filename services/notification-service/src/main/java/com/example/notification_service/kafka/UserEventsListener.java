package com.example.notification_service.kafka;

import com.example.notification_service.user.UserEvent;
import com.example.notification_service.user.UserEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventsListener {

    private final JsonMapper jsonMapper;
    private final UserEventHandler handler;

    @KafkaListener(topics = "${kafka.topics.user-events}")
    public void onMessage(ConsumerRecord<String, String> record) {
        UserEvent event = jsonMapper.readValue(record.value(), UserEvent.class);

        log.info("Received {} eventId={} userId={} partition={} offset={}",
                event.eventType(), event.eventId(), event.userId(), record.partition(), record.offset());

        handler.handle(event);
    }
}
