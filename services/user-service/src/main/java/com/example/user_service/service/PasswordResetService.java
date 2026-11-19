package com.example.user_service.service;

import com.example.user_service.common.Emails;
import com.example.user_service.common.UserStatus;
import com.example.user_service.config.PasswordResetProperties;
import com.example.user_service.entity.PasswordResetToken;
import com.example.user_service.event.NotificationEvent;
import com.example.user_service.exception.InvalidPasswordResetTokenException;
import com.example.user_service.outbox.OutboxService;
import com.example.user_service.repository.PasswordResetTokenRepository;
import com.example.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final TokenGenerator tokenGenerator;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;
    private final PasswordResetProperties properties;

    @Transactional
    public void requestReset(final String email) {
        var user = userRepository.findByEmail(Emails.normalize(email))
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElse(null);

        if (user == null) {
            return;
        }

        var now = Instant.now();
        if (tokenRepository.existsByUserIdAndCreatedAtAfter(user.getId(), now.minus(properties.requestInterval()))) {
            return;
        }

        tokenRepository.invalidateUnusedForUser(user.getId(), now);

        var rawToken = tokenGenerator.generate();
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenGenerator.hash(rawToken))
                .expiresAt(now.plus(properties.ttl()))
                .build());

        var resetUrl = UriComponentsBuilder.fromUriString(properties.url())
                .queryParam("token", rawToken)
                .toUriString();
        outboxService.saveNotification(
                NotificationEvent.passwordReset(user, resetUrl, properties.ttl().toMinutes()));
    }

    @Transactional
    public void resetPassword(final String rawToken, final String newPassword) {
        var token = tokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
                .filter(PasswordResetToken::isUsable)
                .filter(found -> found.getUser().getStatus() == UserStatus.ACTIVE)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        var user = token.getUser();
        var now = Instant.now();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(now);
        tokenRepository.invalidateUnusedForUser(user.getId(), now);
        refreshTokenService.revokeAllFor(user.getId());
        outboxService.saveNotification(NotificationEvent.updatedPassword(user));
    }
}
