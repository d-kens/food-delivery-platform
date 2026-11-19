package com.example.user_service.service;

import com.example.user_service.common.UserStatus;
import com.example.user_service.config.PasswordResetProperties;
import com.example.user_service.entity.PasswordResetToken;
import com.example.user_service.entity.User;
import com.example.user_service.event.NotificationEvent;
import com.example.user_service.exception.InvalidPasswordResetTokenException;
import com.example.user_service.outbox.OutboxService;
import com.example.user_service.repository.PasswordResetTokenRepository;
import com.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane.doe@gmail.com";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final OutboxService outboxService = mock(OutboxService.class);

    private final PasswordResetService service = new PasswordResetService(userRepository, tokenRepository,
            tokenGenerator, refreshTokenService, passwordEncoder, outboxService,
            new PasswordResetProperties(Duration.ofMinutes(30), Duration.ofMinutes(1), "http://localhost:4200/reset-password"));

    private User user;

    @BeforeEach
    void setUp() {
        user = user(UserStatus.ACTIVE);
        when(tokenGenerator.generate()).thenReturn("raw-token");
        when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    }

    private static User user(UserStatus status) {
        return User.builder().id(USER_ID).email(EMAIL).firstName("Jane").lastName("Doe")
                .passwordHash("old-hash").status(status).build();
    }

    private PasswordResetToken token(Instant expiresAt, Instant usedAt) {
        return PasswordResetToken.builder().user(user).tokenHash("hashed-token")
                .expiresAt(expiresAt).usedAt(usedAt).build();
    }

    @Test
    void requestReset_savesHashedTokenAndQueuesResetEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.requestReset(" " + EMAIL + " ");

        var tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).invalidateUnusedForUser(eq(USER_ID), any());
        verify(tokenRepository).save(tokenCaptor.capture());
        var saved = tokenCaptor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo("hashed-token");
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getExpiresAt()).isBetween(
                Instant.now().plus(Duration.ofMinutes(29)), Instant.now().plus(Duration.ofMinutes(31)));

        var eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(outboxService).saveNotification(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(NotificationEvent.RESET_PASSWORD);
        assertThat(event.recipientId()).isEqualTo(USER_ID);
        assertThat(event.payload())
                .containsEntry("firstName", "Jane")
                .containsEntry("resetUrl", "http://localhost:4200/reset-password?token=raw-token")
                .containsEntry("expiresInMinutes", 30L);
    }

    @Test
    void requestReset_looksUpUserByNormalizedEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.requestReset(" " + EMAIL.toUpperCase() + " ");

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void requestReset_doesNothing_whenNoAccountHasTheEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.requestReset(EMAIL);

        verifyNoInteractions(tokenRepository, outboxService);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "DELETED"})
    void requestReset_doesNothing_whenAccountIsNotActive(UserStatus status) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(status)));

        service.requestReset(EMAIL);

        verifyNoInteractions(tokenRepository, outboxService);
    }

    @Test
    void requestReset_doesNothing_whenALinkWasJustSent() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserIdAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(true);

        service.requestReset(EMAIL);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void resetPassword_setsPasswordUsesTokenAndLogsOutEverywhere() {
        var token = token(Instant.now().plusSeconds(600), null);
        when(tokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassw0rd!")).thenReturn("new-hash");

        service.resetPassword("raw-token", "NewPassw0rd!");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(tokenRepository).invalidateUnusedForUser(eq(USER_ID), any());
        verify(refreshTokenService).revokeAllFor(USER_ID);
        var eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(outboxService).saveNotification(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(NotificationEvent.UPDATED_PASSWORD);
    }

    @Test
    void resetPassword_rejectsUnknownToken() {
        when(tokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.empty());

        assertRejected();
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        when(tokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(token(Instant.now().minusSeconds(1), null)));

        assertRejected();
    }

    @Test
    void resetPassword_rejectsUsedToken() {
        when(tokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(token(Instant.now().plusSeconds(600), Instant.now())));

        assertRejected();
    }

    @Test
    void resetPassword_rejectsToken_whenAccountIsNoLongerActive() {
        user.setStatus(UserStatus.SUSPENDED);
        when(tokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(token(Instant.now().plusSeconds(600), null)));

        assertRejected();
    }

    private void assertRejected() {
        assertThatThrownBy(() -> service.resetPassword("raw-token", "NewPassw0rd!"))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessage("This reset link is invalid or has expired");
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        verifyNoInteractions(refreshTokenService, outboxService);
    }
}
