package com.example.notification_service.novu;

import co.novu.Novu;
import co.novu.models.components.CreateSubscriberRequestDto;
import co.novu.models.components.TriggerEventRequestDto;
import co.novu.models.components.TriggerEventRequestDtoTo2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NovuClient {

    private final Novu novu;

    public void upsertSubscriber(Subscriber subscriber) {
        var request = CreateSubscriberRequestDto.builder()
                .subscriberId(subscriber.id())
                .firstName(subscriber.firstName())
                .lastName(subscriber.lastName())
                .email(subscriber.email())
                .phone(subscriber.phone())
                .build();

        novu.subscribers().create()
                .failIfExists(false)
                .body(request)
                .call();
    }

    public void trigger(NotificationWorkflow workflow, String subscriberId, Map<String, Object> payload, String transactionId) {
        var request = TriggerEventRequestDto.builder()
                .workflowId(workflow.getWorkflowId())
                .to(TriggerEventRequestDtoTo2.of(subscriberId))
                .payload(payload)
                .transactionId(transactionId)
                .build();

        novu.trigger()
                .idempotencyKey(transactionId)
                .body(request)
                .call();
    }
}
