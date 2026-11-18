package com.example.notification_service.user;

import com.example.notification_service.novu.NovuClient;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserEventHandlerTest {

    private final NovuClient novuClient = mock(NovuClient.class);
    private final UserEventHandler handler = new UserEventHandler(novuClient);

    private static UserEvent event(String type) {
        return new UserEvent("e1", type, "u1", "Jane", "Doe", "jane@example.com", "+254700000000");
    }

    @Test
    void userCreated_upsertsSubscriber() {
        var event = event("UserCreated");

        handler.handle(event);

        verify(novuClient).upsertSubscriber(event);
    }

    @Test
    void userUpdated_upsertsSubscriber() {
        var event = event("UserUpdated");

        handler.handle(event);

        verify(novuClient).upsertSubscriber(event);
    }

    @Test
    void userDeleted_deletesSubscriber() {
        handler.handle(event("UserDeleted"));

        verify(novuClient).deleteSubscriber("u1");
    }

    @Test
    void otherEventTypes_areIgnored() {
        handler.handle(event("UserRoleChanged"));

        verifyNoInteractions(novuClient);
    }
}
