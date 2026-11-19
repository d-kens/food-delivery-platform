package com.example.user_service.service;


import com.example.user_service.config.RefreshTokenProperties;
import com.example.user_service.entity.RefreshToken;
import com.example.user_service.entity.User;
import com.example.user_service.exception.InvalidRefreshTokenException;
import com.example.user_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final TokenGenerator tokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;

    public String createFor(final User user) {
        String rawToken = tokenGenerator.generate();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenGenerator.hash(rawToken))
                .expiresAt(Instant.now().plus(properties.ttl()))
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validate(final String rawToken) {
        String hashedToken = tokenGenerator.hash(rawToken);

        var refreshToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    @Transactional
    public void revoke(final String rawToken) {
        refreshTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
                .filter(refreshToken -> !refreshToken.isRevoked())
                .ifPresent(refreshToken -> refreshToken.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeAllFor(final UUID userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now());
    }
}
