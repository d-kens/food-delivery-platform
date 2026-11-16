package com.example.notification_service.notification;

import com.example.notification_service.novu.NovuClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final NovuClient novuClient;

    public void send(Notification notification) {
        Recipient recipient = notification.recipient();
        novuClient.upsertSubscriber(recipient);
        novuClient.trigger(notification.type().getWorkflowId(), recipient.id(), notification.data(), notification.eventId());
    }
}
