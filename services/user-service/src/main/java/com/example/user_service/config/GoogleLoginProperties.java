package com.example.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.google")
public record GoogleLoginProperties(
        String successUrl,
        String failureUrl
) {
}
