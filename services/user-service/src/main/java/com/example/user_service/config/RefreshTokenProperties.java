package com.example.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.refresh-token")
public record RefreshTokenProperties(
        Duration ttl,
        boolean cookieSecure,
        String cookiePath
) {
}
