package com.example.notification_service.novu;

import co.novu.Novu;
import co.novu.models.components.CreateSubscriberRequestDto;
import co.novu.models.components.TriggerEventRequestDto;
import co.novu.models.components.TriggerEventRequestDtoTo2;
import com.example.notification_service.notification.Recipient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NovuClient {

    private final Novu novu;

    public void upsertSubscriber(Recipient recipient) {
        var request = CreateSubscriberRequestDto.builder()
                .subscriberId(recipient.id())
                .firstName(recipient.firstName())
                .lastName(recipient.lastName())
                .email(recipient.email())
                .phone(recipient.phone())
                .build();

        novu.subscribers().create()
                .failIfExists(false)
                .body(request)
                .call();
    }

    public void trigger(String workflowId, String subscriberId, Map<String, Object> payload, String transactionId) {
        var request = TriggerEventRequestDto.builder()
                .workflowId(workflowId)
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
