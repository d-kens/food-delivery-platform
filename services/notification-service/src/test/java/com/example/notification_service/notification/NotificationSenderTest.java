package com.example.notification_service.notification;

import com.example.notification_service.novu.NovuClient;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Map;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class NotificationSenderTest {

    private final NovuClient novuClient = mock(NovuClient.class);
    private final NotificationSender sender = new NotificationSender(novuClient);

    @Test
    void send_upsertsRecipientThenTriggersWorkflow() {
        sender.send(new Notification("e1", NotificationType.WELCOME,
                new Recipient("u1", "Jane", "Doe", "jane@example.com", "+254700000000"), Map.of("firstName", "Jane")));

        InOrder order = inOrder(novuClient);
        order.verify(novuClient).upsertSubscriber(new Recipient("u1", "Jane", "Doe", "jane@example.com", "+254700000000"));
        order.verify(novuClient).trigger("welcome", "u1", Map.of("firstName", "Jane"), "e1");
    }
}
