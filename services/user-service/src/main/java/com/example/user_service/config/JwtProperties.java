package com.example.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String issuer,
        String keyId,
        Duration expiration,
        Resource privateKey,
        Resource publicKey
) {
}
