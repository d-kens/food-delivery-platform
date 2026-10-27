package com.example.user_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieServiceTest {

    private RefreshTokenCookieService refreshTokenCookieService;

    @BeforeEach
    void setUp() {
        refreshTokenCookieService = new RefreshTokenCookieService();
        ReflectionTestUtils.setField(refreshTokenCookieService, "refreshTokenTtl", Duration.ofDays(7));
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookieSecure", true);
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookiePath", "/api/auth");
    }

    @Test
    void createRefreshTokenCookie_returnsCookieWithTokenAndSecureAttributes() {
        ResponseCookie cookie = refreshTokenCookieService.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieService.COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void clearRefreshTokenCookie_returnsEmptyExpiredCookieWithSameAttributes() {
        ResponseCookie cookie = refreshTokenCookieService.clearRefreshTokenCookie();

        assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieService.COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void createRefreshTokenCookie_usesConfiguredSecureAndPath() {
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookieSecure", false);
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookiePath", "/auth");

        ResponseCookie cookie = refreshTokenCookieService.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/auth");
    }
}
