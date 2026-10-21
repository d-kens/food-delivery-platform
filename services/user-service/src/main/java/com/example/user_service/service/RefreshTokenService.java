package com.example.user_service.service;


import com.example.user_service.entity.RefreshToken;
import com.example.user_service.entity.User;
import com.example.user_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.refresh-token.ttl}")
    private Duration refreshTokeTtl;

    public String createFor(User user) {
        String rawToken = refreshTokenGenerator.generate();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenGenerator.hash(rawToken))
                .expiresAt(Instant.now().plus(refreshTokeTtl))
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }
}
