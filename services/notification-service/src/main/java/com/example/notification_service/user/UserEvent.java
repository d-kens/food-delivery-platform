package com.example.notification_service.user;

import com.example.notification_service.kafka.InvalidMessageException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserEvent(String eventId, String eventType, String userId,
                        String firstName, String lastName, String email, String phone) {

    public UserEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidMessageException("User event is missing eventId");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new InvalidMessageException("User event is missing eventType");
        }
        if (userId == null || userId.isBlank()) {
            throw new InvalidMessageException("User event is missing userId");
        }
    }
}
