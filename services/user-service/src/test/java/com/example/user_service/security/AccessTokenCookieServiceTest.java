package com.example.user_service.security;

import com.example.user_service.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenCookieServiceTest {

    private final AccessTokenCookieService accessTokenCookieService =
            new AccessTokenCookieService(jwtProperties(true, "/api"));

    @Test
    void createAccessTokenCookie_returnsCookieThatExpiresWithTheToken() {
        ResponseCookie cookie = accessTokenCookieService.createAccessTokenCookie("access-token");

        assertThat(cookie.getName()).isEqualTo(AccessTokenCookieService.COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("access-token");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(15));
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void clearAccessTokenCookie_returnsEmptyExpiredCookieWithSameAttributes() {
        ResponseCookie cookie = accessTokenCookieService.clearAccessTokenCookie();

        assertThat(cookie.getName()).isEqualTo(AccessTokenCookieService.COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void createAccessTokenCookie_usesConfiguredSecureAndPath() {
        var devCookieService = new AccessTokenCookieService(jwtProperties(false, "/"));

        ResponseCookie cookie = devCookieService.createAccessTokenCookie("access-token");

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    private static JwtProperties jwtProperties(boolean cookieSecure, String cookiePath) {
        return new JwtProperties("issuer", "key-id", Duration.ofMinutes(15), null, null, cookieSecure, cookiePath);
    }
}
