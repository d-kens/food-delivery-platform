package com.example.user_service.outbox;

import com.example.user_service.config.KafkaTopicsProperties;
import com.example.user_service.entity.OutboxMessage;
import com.example.user_service.event.NotificationEvent;
import com.example.user_service.event.UserEvent;
import com.example.user_service.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxMessageRepository repository;
    private final KafkaTopicsProperties topics;
    private final JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveUserEvent(UserEvent event) {
        save(topics.userEvents(), event.userId().toString(), event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveNotification(NotificationEvent notification) {
        save(topics.notifications(), notification.recipientId().toString(), notification);
    }

    private void save(String topic, String key, Object message) {
        repository.save(OutboxMessage.builder()
                .topic(topic)
                .messageKey(key)
                .payload(jsonMapper.writeValueAsString(message))
                .build());
    }
}
