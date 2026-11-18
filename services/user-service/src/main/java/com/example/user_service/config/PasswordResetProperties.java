package com.example.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.password-reset")
public record PasswordResetProperties(
        Duration ttl,
        Duration requestInterval,
        String url
) {
}
