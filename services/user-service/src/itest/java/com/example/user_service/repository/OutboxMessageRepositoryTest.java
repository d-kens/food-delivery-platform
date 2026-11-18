package com.example.user_service.repository;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.entity.OutboxMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxMessageRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxMessageRepository repository;

    @BeforeEach
    void clearOutbox() {
        repository.deleteAll();
    }

    private OutboxMessage save(String key) {
        return repository.saveAndFlush(OutboxMessage.builder().topic("user-events").messageKey(key).payload("{}").build());
    }

    @Test
    void findTop100_returnsMessagesOldestFirst() {
        save("first");
        save("second");
        save("third");

        assertThat(repository.findTop100ByOrderByCreatedAtAsc())
                .extracting(OutboxMessage::getMessageKey)
                .containsExactly("first", "second", "third");
    }

    @Test
    void tryAdvisoryXactLock_acquiresLock() {
        assertThat(repository.tryAdvisoryXactLock(42L)).isTrue();
    }
}
