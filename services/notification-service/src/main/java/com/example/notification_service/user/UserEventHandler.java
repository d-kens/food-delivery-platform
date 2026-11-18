package com.example.notification_service.user;

import com.example.notification_service.novu.NovuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {

    private final NovuClient novuClient;

    public void handle(UserEvent event) {
        switch (event.eventType()) {
            case "UserCreated", "UserUpdated" -> novuClient.upsertSubscriber(event);
            case "UserDeleted" -> novuClient.deleteSubscriber(event.userId());
            default -> log.debug("Ignoring {} eventId={}", event.eventType(), event.eventId());
        }
    }
}
