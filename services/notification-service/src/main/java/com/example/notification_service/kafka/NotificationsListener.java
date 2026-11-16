package com.example.notification_service.kafka;

import com.example.notification_service.notification.Notification;
import com.example.notification_service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationsListener {

    private final JsonMapper jsonMapper;
    private final NotificationSender sender;

    @KafkaListener(topics = "${kafka.topics.notifications}")
    public void onMessage(ConsumerRecord<String, String> record) {
        Notification notification = jsonMapper.readValue(record.value(), Notification.class);

        log.info("Received {} eventId={} recipient={} partition={} offset={}",
                notification.type(), notification.eventId(), notification.recipient().id(),
                record.partition(), record.offset());

        sender.send(notification);
    }
}
