package com.example.user_service.outbox;

import com.example.user_service.common.UserStatus;
import com.example.user_service.config.KafkaTopicsProperties;
import com.example.user_service.entity.OutboxMessage;
import com.example.user_service.entity.User;
import com.example.user_service.event.NotificationEvent;
import com.example.user_service.event.UserEvent;
import com.example.user_service.repository.OutboxMessageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private final OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final OutboxService outboxService = new OutboxService(repository,
            new KafkaTopicsProperties("user-events", "notifications"), jsonMapper);

    private final User user = User.builder()
            .id(USER_ID)
            .firstName("Jane")
            .lastName("Doe")
            .email("jane@example.com")
            .phoneNumber("+254700000000")
            .build();

    private OutboxMessage savedMessage() {
        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void saveUserEvent_storesEventOnUserEventsTopicKeyedByUserId() {
        var event = UserEvent.created(user);

        outboxService.saveUserEvent(event);

        var message = savedMessage();
        assertThat(message.getTopic()).isEqualTo("user-events");
        assertThat(message.getMessageKey()).isEqualTo(USER_ID.toString());
        JsonNode json = jsonMapper.readTree(message.getPayload());
        assertThat(json.get("eventId").asString()).isEqualTo(event.eventId().toString());
        assertThat(json.get("eventType").asString()).isEqualTo("UserCreated");
        assertThat(json.get("occurredAt").asString()).isEqualTo(event.occurredAt().toString());
        assertThat(json.get("userId").asString()).isEqualTo(USER_ID.toString());
        assertThat(json.get("firstName").asString()).isEqualTo("Jane");
        assertThat(json.get("lastName").asString()).isEqualTo("Doe");
        assertThat(json.get("email").asString()).isEqualTo("jane@example.com");
        assertThat(json.get("phone").asString()).isEqualTo("+254700000000");
    }

    @Test
    void saveUserEvent_leavesOutEmptyFieldsForUserDeleted() {
        outboxService.saveUserEvent(UserEvent.deleted(USER_ID));

        JsonNode json = jsonMapper.readTree(savedMessage().getPayload());
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("eventId", "eventType", "occurredAt", "userId");
        assertThat(json.get("eventType").asString()).isEqualTo("UserDeleted");
    }

    @Test
    void saveUserEvent_sendsOnlyTheStatusForUserStatusChanged() {
        outboxService.saveUserEvent(UserEvent.statusChanged(USER_ID, UserStatus.SUSPENDED));

        var message = savedMessage();
        assertThat(message.getTopic()).isEqualTo("user-events");
        assertThat(message.getMessageKey()).isEqualTo(USER_ID.toString());
        JsonNode json = jsonMapper.readTree(message.getPayload());
        assertThat(json.propertyNames())
                .containsExactlyInAnyOrder("eventId", "eventType", "occurredAt", "userId", "status");
        assertThat(json.get("eventType").asString()).isEqualTo("UserStatusChanged");
        assertThat(json.get("status").asString()).isEqualTo("SUSPENDED");
    }

    @Test
    void saveNotification_storesNotificationOnNotificationsTopicKeyedByRecipient() {
        var notification = NotificationEvent.updatedPassword(user);

        outboxService.saveNotification(notification);

        var message = savedMessage();
        assertThat(message.getTopic()).isEqualTo("notifications");
        assertThat(message.getMessageKey()).isEqualTo(USER_ID.toString());
        JsonNode json = jsonMapper.readTree(message.getPayload());
        assertThat(json.get("eventId").asString()).isEqualTo(notification.eventId().toString());
        assertThat(json.get("type").asString()).isEqualTo("UPDATED_PASSWORD");
        assertThat(json.get("recipientId").asString()).isEqualTo(USER_ID.toString());
        assertThat(json.get("payload").get("firstName").asString()).isEqualTo("Jane");
        assertThat(json.get("payload").has("changedAt")).isTrue();
    }
}
