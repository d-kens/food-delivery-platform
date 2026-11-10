package com.example.user_service.service;

import com.example.user_service.entity.RefreshToken;
import com.example.user_service.entity.User;
import com.example.user_service.exception.InvalidRefreshTokenException;
import com.example.user_service.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration TTL = Duration.ofDays(7);

    @Mock private RefreshTokenGenerator refreshTokenGenerator;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenTtl", TTL);
    }

    @Test
    void createFor_savesHashedTokenWithExpiryAndReturnsRawToken() {
        var user = User.builder().id(UUID.randomUUID()).build();

        when(refreshTokenGenerator.generate()).thenReturn("raw-token");
        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");

        Instant before = Instant.now();
        String result = refreshTokenService.createFor(user);
        Instant after = Instant.now();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(result).isEqualTo("raw-token");
        assertThat(saved.getTokenHash()).isEqualTo("hashed-token");
        assertThat(saved.getTokenHash()).isNotEqualTo(result);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getExpiresAt()).isBetween(before.plus(TTL), after.plus(TTL));
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    void validate_whenTokenIsValid_returnsRefreshToken() {
        var savedRefreshToken = createRefreshToken(User.builder().id(UUID.randomUUID()).build());

        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(savedRefreshToken));

        var result = refreshTokenService.validate("raw-token");

        assertThat(result).isSameAs(savedRefreshToken);
    }

    @Test
    void validate_whenTokenHashIsNotFound_throwsInvalidRefreshTokenException() {
        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    void validate_whenTokenIsExpired_throwsInvalidRefreshTokenException() {
        var expiredToken = createRefreshToken(User.builder().id(UUID.randomUUID()).build());
        expiredToken.setExpiresAt(Instant.now().minusSeconds(1));

        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.validate("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    void validate_whenTokenIsRevoked_throwsInvalidRefreshTokenException() {
        var revokedToken = createRefreshToken(User.builder().id(UUID.randomUUID()).build());
        revokedToken.setRevokedAt(Instant.now());

        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.validate("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    private RefreshToken createRefreshToken(User user) {
        return RefreshToken
                .builder()
                .user(user)
                .tokenHash("hashed-token")
                .expiresAt(Instant.now().plus(TTL))
                .build();
    }

    @Test
    void revoke_marksTokenAsRevoked() {
        var token = RefreshToken.builder().tokenHash("hashed").expiresAt(Instant.now().plus(TTL)).build();
        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));

        refreshTokenService.revoke("raw-token");

        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    void revoke_keepsOriginalRevocationTime_whenAlreadyRevoked() {
        var revokedAt = Instant.parse("2026-01-01T10:00:00Z");
        var token = RefreshToken.builder().tokenHash("hashed").revokedAt(revokedAt).build();
        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));

        refreshTokenService.revoke("raw-token");

        assertThat(token.getRevokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void revoke_doesNothing_whenTokenIsUnknown() {
        when(refreshTokenGenerator.hash("unknown")).thenReturn("hashed-unknown");
        when(refreshTokenRepository.findByTokenHash("hashed-unknown")).thenReturn(Optional.empty());

        refreshTokenService.revoke("unknown");

        verify(refreshTokenRepository).findByTokenHash("hashed-unknown");
    }
}
