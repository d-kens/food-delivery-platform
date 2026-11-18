package com.example.user_service.outbox;

import com.example.user_service.entity.OutboxMessage;
import com.example.user_service.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    static final long LOCK_KEY = 5_001_001L;
    private static final long SEND_TIMEOUT_SECONDS = 15;

    private final OutboxMessageRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relay() {
        if (!repository.tryAdvisoryXactLock(LOCK_KEY)) {
            return;
        }

        for (OutboxMessage message : repository.findTop100ByOrderByCreatedAtAsc()) {
            if (!send(message)) {
                return;
            }
            repository.delete(message);
        }
    }

    private boolean send(OutboxMessage message) {
        try {
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to publish outbox message {} to {}, retrying on next run",
                    message.getId(), message.getTopic(), e);
            return false;
        }
    }
}
