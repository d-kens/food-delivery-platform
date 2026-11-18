package com.example.notification_service.novu;

import co.novu.Novu;
import co.novu.models.components.CreateSubscriberRequestDto;
import co.novu.models.components.TriggerEventRequestDto;
import co.novu.models.components.TriggerEventRequestDtoTo2;
import co.novu.models.errors.NovuException;
import com.example.notification_service.user.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NovuClient {

    private final Novu novu;

    public void upsertSubscriber(UserEvent user) {
        var request = CreateSubscriberRequestDto.builder()
                .subscriberId(user.userId())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .email(user.email())
                .phone(user.phone())
                .build();

        novu.subscribers().create()
                .failIfExists(false)
                .body(request)
                .call();
    }

    public void deleteSubscriber(String subscriberId) {
        try {
            novu.subscribers().delete(subscriberId);
        } catch (NovuException e) {
            if (e.code() != 404) {
                throw e;
            }
            log.info("Subscriber {} already deleted", subscriberId);
        }
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
