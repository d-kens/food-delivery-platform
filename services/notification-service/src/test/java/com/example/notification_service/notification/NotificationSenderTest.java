package com.example.notification_service.notification;

import com.example.notification_service.novu.NovuClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class NotificationSenderTest {

    private final NovuClient novuClient = mock(NovuClient.class);
    private final NotificationSender sender = new NotificationSender(novuClient);

    @Test
    void send_triggersWorkflowForRecipient() {
        var payload = Map.<String, Object>of("firstName", "Jane");

        sender.send(new Notification("e1", NotificationType.UPDATED_PASSWORD, "u1", payload));

        verify(novuClient).trigger("UPDATED_PASSWORD", "u1", payload, "e1");
        verifyNoMoreInteractions(novuClient);
    }
}
