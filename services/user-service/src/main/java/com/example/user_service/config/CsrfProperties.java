package com.example.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.csrf")
public record CsrfProperties(
        boolean cookieSecure
) {
}
