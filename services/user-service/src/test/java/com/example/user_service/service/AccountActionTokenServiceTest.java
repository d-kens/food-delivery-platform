package com.example.user_service.service;

import com.example.user_service.common.AccountActionPurpose;
import com.example.user_service.common.UserStatus;
import com.example.user_service.config.AccountActionProperties;
import com.example.user_service.entity.AccountActionToken;
import com.example.user_service.entity.User;
import com.example.user_service.exception.InvalidAccountActionTokenException;
import com.example.user_service.repository.AccountActionTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.Mockito.when;

class AccountActionTokenServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final AccountActionPurpose PURPOSE = AccountActionPurpose.DELETE_ACCOUNT;

    private final AccountActionTokenRepository tokenRepository = mock(AccountActionTokenRepository.class);
    private final TokenGenerator tokenGenerator = mock(TokenGenerator.class);

    private final AccountActionTokenService service = new AccountActionTokenService(tokenRepository, tokenGenerator,
            new AccountActionProperties(Duration.ofMinutes(30), Duration.ofMinutes(1),
                    "http://localhost:4200/account/delete/confirm", "http://localhost:4200/account/email/confirm"));

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).email("jane.doe@gmail.com").firstName("Jane").lastName("Doe")
                .status(UserStatus.ACTIVE).build();
        when(tokenGenerator.generate()).thenReturn("raw-token");
        when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    }

    @Test
    void create_savesHashedTokenAndReturnsRawToken() {
        var rawToken = service.create(user, PURPOSE, null);

        assertThat(rawToken).contains("raw-token");
        verify(tokenRepository).invalidateUnusedForUser(eq(USER_ID), eq(PURPOSE), any());
        var captor = ArgumentCaptor.forClass(AccountActionToken.class);
        verify(tokenRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getPurpose()).isEqualTo(PURPOSE);
        assertThat(saved.getTokenHash()).isEqualTo("hashed-token");
        assertThat(saved.getNewEmail()).isNull();
        assertThat(saved.getExpiresAt()).isBetween(
                Instant.now().plus(Duration.ofMinutes(29)), Instant.now().plus(Duration.ofMinutes(31)));
    }

    @Test
    void create_returnsEmpty_whenALinkWasJustSent() {
        when(tokenRepository.existsByUserIdAndPurposeAndCreatedAtAfter(eq(USER_ID), eq(PURPOSE), any())).thenReturn(true);

        assertThat(service.create(user, PURPOSE, null)).isEmpty();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void consume_marksTokenUsedAndReturnsIt() {
        var token = token(Instant.now().plusSeconds(600), null);
        when(tokenRepository.findByTokenHashAndPurpose("hashed-token", PURPOSE)).thenReturn(Optional.of(token));

        assertThat(service.consume("raw-token", PURPOSE)).isSameAs(token);
        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void consume_rejectsUnknownToken() {
        when(tokenRepository.findByTokenHashAndPurpose("hashed-token", PURPOSE)).thenReturn(Optional.empty());

        assertRejected();
    }

    @Test
    void consume_rejectsExpiredToken() {
        when(tokenRepository.findByTokenHashAndPurpose("hashed-token", PURPOSE))
                .thenReturn(Optional.of(token(Instant.now().minusSeconds(1), null)));

        assertRejected();
    }

    @Test
    void consume_rejectsUsedToken() {
        var usedAt = Instant.now().minusSeconds(60);
        var token = token(Instant.now().plusSeconds(600), usedAt);
        when(tokenRepository.findByTokenHashAndPurpose("hashed-token", PURPOSE)).thenReturn(Optional.of(token));

        assertRejected();
        assertThat(token.getUsedAt()).isEqualTo(usedAt);
    }

    @Test
    void consume_rejectsToken_whenAccountIsNoLongerActive() {
        user.setStatus(UserStatus.SUSPENDED);
        var token = token(Instant.now().plusSeconds(600), null);
        when(tokenRepository.findByTokenHashAndPurpose("hashed-token", PURPOSE)).thenReturn(Optional.of(token));

        assertRejected();
        assertThat(token.getUsedAt()).isNull();
    }

    private void assertRejected() {
        assertThatThrownBy(() -> service.consume("raw-token", PURPOSE))
                .isInstanceOf(InvalidAccountActionTokenException.class)
                .hasMessage("This link is invalid or has expired");
    }

    private AccountActionToken token(Instant expiresAt, Instant usedAt) {
        return AccountActionToken.builder().user(user).purpose(PURPOSE).tokenHash("hashed-token")
                .expiresAt(expiresAt).usedAt(usedAt).build();
    }
}
