package com.example.user_service.security;

import com.example.user_service.config.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AccessTokenCookieService {

    public static final String COOKIE_NAME = "access_token";

    private final Duration accessTokenTtl;
    private final boolean cookieSecure;
    private final String cookiePath;

    public AccessTokenCookieService(
            JwtProperties jwtProperties,
            @Value("${security.jwt.cookie-secure}") boolean cookieSecure,
            @Value("${security.jwt.cookie-path}") String cookiePath
    ) {
        this.accessTokenTtl = jwtProperties.expiration();
        this.cookieSecure = cookieSecure;
        this.cookiePath = cookiePath;
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return buildCookie(accessToken, accessTokenTtl);
    }

    public ResponseCookie clearAccessTokenCookie() {
        return buildCookie("", Duration.ZERO);
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie
                .from(COOKIE_NAME, value)
                .path(cookiePath)
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .build();
    }
}
