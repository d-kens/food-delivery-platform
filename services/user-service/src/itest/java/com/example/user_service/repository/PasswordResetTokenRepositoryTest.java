package com.example.user_service.repository;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.common.Role;
import com.example.user_service.entity.PasswordResetToken;
import com.example.user_service.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PasswordResetTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void invalidateUnusedForUser_marksOnlyThatUsersUnusedTokensAsUsed() {
        var jane = persistUser("jane.doe@gmail.com");
        var john = persistUser("john.doe@gmail.com");
        var alreadyUsedAt = Instant.parse("2026-01-01T10:00:00Z");
        var janeUnused = persistToken(jane, "a".repeat(64), null);
        var janeUsed = persistToken(jane, "b".repeat(64), alreadyUsedAt);
        var johnUnused = persistToken(john, "c".repeat(64), null);
        var now = Instant.now();

        int updated = tokenRepository.invalidateUnusedForUser(jane.getId(), now);
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(entityManager.find(PasswordResetToken.class, janeUnused.getId()).getUsedAt()).isNotNull();
        assertThat(entityManager.find(PasswordResetToken.class, janeUsed.getId()).getUsedAt()).isEqualTo(alreadyUsedAt);
        assertThat(entityManager.find(PasswordResetToken.class, johnUnused.getId()).getUsedAt()).isNull();
    }

    @Test
    void existsByUserIdAndCreatedAtAfter_findsOnlyThatUsersTokensCreatedAfterTheCutoff() {
        var jane = persistUser("jane.doe@gmail.com");
        var john = persistUser("john.doe@gmail.com");
        persistToken(jane, "a".repeat(64), null);
        var now = Instant.now();

        assertThat(tokenRepository.existsByUserIdAndCreatedAtAfter(jane.getId(), now.minus(Duration.ofMinutes(1)))).isTrue();
        assertThat(tokenRepository.existsByUserIdAndCreatedAtAfter(jane.getId(), now.plus(Duration.ofMinutes(1)))).isFalse();
        assertThat(tokenRepository.existsByUserIdAndCreatedAtAfter(john.getId(), now.minus(Duration.ofMinutes(1)))).isFalse();
    }

    private User persistUser(String email) {
        return entityManager.persistAndFlush(User.builder()
                .email(email)
                .firstName("Jane")
                .lastName("Doe")
                .passwordHash("hashed")
                .roles(Set.of(Role.USER))
                .build());
    }

    private PasswordResetToken persistToken(User user, String hash, Instant usedAt) {
        return entityManager.persistAndFlush(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(30)))
                .usedAt(usedAt)
                .build());
    }
}
