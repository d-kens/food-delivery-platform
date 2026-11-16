package com.example.notification_service.novu;

import co.novu.Novu;
import com.example.notification_service.notification.Recipient;
import co.novu.models.components.CreateSubscriberRequestDto;
import co.novu.models.components.TriggerEventRequestDto;
import co.novu.models.operations.EventsControllerTriggerRequestBuilder;
import co.novu.models.operations.SubscribersControllerCreateSubscriberRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NovuClientTest {

    private Novu novu;
    private EventsControllerTriggerRequestBuilder triggerBuilder;
    private SubscribersControllerCreateSubscriberRequestBuilder createBuilder;
    private NovuClient client;

    @BeforeEach
    void setUp() {
        novu = mock(Novu.class, RETURNS_DEEP_STUBS);
        triggerBuilder = mock(EventsControllerTriggerRequestBuilder.class, RETURNS_DEEP_STUBS);
        createBuilder = mock(SubscribersControllerCreateSubscriberRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(novu.trigger()).thenReturn(triggerBuilder);
        when(novu.subscribers().create()).thenReturn(createBuilder);
        when(triggerBuilder.idempotencyKey(any())).thenReturn(triggerBuilder);
        when(triggerBuilder.body(any())).thenReturn(triggerBuilder);
        when(createBuilder.failIfExists(any())).thenReturn(createBuilder);
        when(createBuilder.body(any())).thenReturn(createBuilder);

        client = new NovuClient(novu);
    }

    @Test
    void trigger_sendsWorkflowToSubscriberWithPayloadAndIdempotencyKey() {
        var payload = Map.<String, Object>of("orderId", "99");

        client.trigger("password-changed", "7", payload, "event-1:7");

        var captor = ArgumentCaptor.forClass(TriggerEventRequestDto.class);
        verify(triggerBuilder).idempotencyKey("event-1:7");
        verify(triggerBuilder).body(captor.capture());
        verify(triggerBuilder).call();
        var request = captor.getValue();
        assertThat(request.workflowId()).isEqualTo("password-changed");
        assertThat(request.to().string()).contains("7");
        assertThat(request.payload()).contains(payload);
        assertThat(request.transactionId()).contains("event-1:7");
    }

    @Test
    void upsertSubscriber_createsOrUpdatesSubscriber() {
        client.upsertSubscriber(new Recipient("42", "Jane", "Doe", "jane@example.com", null));

        var captor = ArgumentCaptor.forClass(CreateSubscriberRequestDto.class);
        verify(createBuilder).failIfExists(false);
        verify(createBuilder).body(captor.capture());
        verify(createBuilder).call();
        var request = captor.getValue();
        assertThat(request.subscriberId()).isEqualTo("42");
        assertThat(request.firstName().get()).isEqualTo("Jane");
        assertThat(request.lastName().get()).isEqualTo("Doe");
        assertThat(request.email().get()).isEqualTo("jane@example.com");
        assertThat(request.phone().get()).isNull();
        assertThat(request.locale().isPresent()).isFalse();
        assertThat(request.timezone().isPresent()).isFalse();
    }
}
