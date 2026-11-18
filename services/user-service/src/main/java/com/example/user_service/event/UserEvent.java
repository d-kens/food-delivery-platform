package com.example.user_service.event;

import com.example.user_service.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserEvent(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                        String firstName, String lastName, String email, String phone) {

    public static final String USER_CREATED = "UserCreated";
    public static final String USER_UPDATED = "UserUpdated";
    public static final String USER_DELETED = "UserDeleted";

    public static UserEvent created(User user) {
        return fromUser(USER_CREATED, user);
    }

    public static UserEvent updated(User user) {
        return fromUser(USER_UPDATED, user);
    }

    public static UserEvent deleted(UUID userId) {
        return new UserEvent(UUID.randomUUID(), USER_DELETED, Instant.now(), userId, null, null, null, null);
    }

    private static UserEvent fromUser(String eventType, User user) {
        return new UserEvent(UUID.randomUUID(), eventType, Instant.now(), user.getId(),
                user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhoneNumber());
    }
}
