package com.example.user_service.outbox;

import com.example.user_service.entity.OutboxMessage;
import com.example.user_service.repository.OutboxMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    private OutboxMessageRepository repository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private OutboxRelay relay;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(OutboxMessageRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        relay = new OutboxRelay(repository, kafkaTemplate);
    }

    private static OutboxMessage message(String key) {
        return OutboxMessage.builder().id(UUID.randomUUID()).topic("user-events").messageKey(key).payload("{}").build();
    }

    @Test
    void doesNothing_whenAnotherInstanceHoldsTheLock() {
        when(repository.tryAdvisoryXactLock(OutboxRelay.LOCK_KEY)).thenReturn(false);

        relay.relay();

        verify(repository, never()).findTop100ByOrderByCreatedAtAsc();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendsMessagesInOrderAndDeletesEachAfterSending() {
        var first = message("u1");
        var second = message("u2");
        when(repository.tryAdvisoryXactLock(OutboxRelay.LOCK_KEY)).thenReturn(true);
        when(repository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        relay.relay();

        InOrder order = inOrder(kafkaTemplate, repository);
        order.verify(kafkaTemplate).send("user-events", "u1", "{}");
        order.verify(repository).delete(first);
        order.verify(kafkaTemplate).send("user-events", "u2", "{}");
        order.verify(repository).delete(second);
    }

    @Test
    void stopsAtFirstFailure_andKeepsThatMessageAndTheRest() {
        var first = message("u1");
        var second = message("u2");
        var third = message("u3");
        when(repository.tryAdvisoryXactLock(OutboxRelay.LOCK_KEY)).thenReturn(true);
        when(repository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(first, second, third));
        when(kafkaTemplate.send("user-events", "u1", "{}")).thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send("user-events", "u2", "{}")).thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        relay.relay();

        verify(repository).delete(first);
        verify(repository, never()).delete(second);
        verify(kafkaTemplate, never()).send("user-events", "u3", "{}");
        verify(repository, never()).delete(third);
    }

    @Test
    void sendFailure_doesNotThrow() {
        when(repository.tryAdvisoryXactLock(OutboxRelay.LOCK_KEY)).thenReturn(true);
        when(repository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(message("u1")));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        relay.relay();

        verify(repository, never()).delete(any());
    }
}
