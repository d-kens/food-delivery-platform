package com.example.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.account-action")
public record AccountActionProperties(
        Duration ttl,
        Duration requestInterval,
        String deleteConfirmUrl,
        String emailConfirmUrl
) {
}
