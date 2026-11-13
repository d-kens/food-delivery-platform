package com.example.user_service.repository;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.common.Role;
import com.example.user_service.entity.RefreshToken;
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
class RefreshTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void revokeAllActiveForUser_revokesOnlyThatUsersActiveTokens() {
        var jane = persistUser("jane.doe@gmail.com");
        var john = persistUser("john.doe@gmail.com");
        var alreadyRevokedAt = Instant.parse("2026-01-01T10:00:00Z");
        var janeActive = persistToken(jane, "a".repeat(64), null);
        var janeRevoked = persistToken(jane, "b".repeat(64), alreadyRevokedAt);
        var johnActive = persistToken(john, "c".repeat(64), null);
        var now = Instant.now();

        int updated = refreshTokenRepository.revokeAllActiveForUser(jane.getId(), now);
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(entityManager.find(RefreshToken.class, janeActive.getId()).getRevokedAt()).isNotNull();
        assertThat(entityManager.find(RefreshToken.class, janeRevoked.getId()).getRevokedAt()).isEqualTo(alreadyRevokedAt);
        assertThat(entityManager.find(RefreshToken.class, johnActive.getId()).getRevokedAt()).isNull();
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

    private RefreshToken persistToken(User user, String hash, Instant revokedAt) {
        return entityManager.persistAndFlush(RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .revokedAt(revokedAt)
                .build());
    }
}
