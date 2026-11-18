package com.example.notification_service.notification;

import com.example.notification_service.novu.NovuClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final NovuClient novuClient;

    public void send(Notification notification) {
        novuClient.trigger(notification.type().getWorkflowId(), notification.recipientId(),
                notification.payload(), notification.eventId());
    }
}
