package com.example.user_service.security;

import com.example.user_service.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieBearerTokenResolverTest {

    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver(SecurityConfig.PUBLIC_ENDPOINTS);

    @Test
    void resolve_readsTokenFromAccessTokenCookie() {
        var request = request("GET", "/users/me");
        request.setCookies(new Cookie(AccessTokenCookieService.COOKIE_NAME, "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("cookie-token");
    }

    @Test
    void resolve_prefersAuthorizationHeaderOverCookie() {
        var request = request("GET", "/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");
        request.setCookies(new Cookie(AccessTokenCookieService.COOKIE_NAME, "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("header-token");
    }

    @Test
    void resolve_returnsNull_whenNoTokenIsPresent() {
        assertThat(resolver.resolve(request("GET", "/users/me"))).isNull();
    }

    @Test
    void resolve_returnsNull_whenCookieIsBlank() {
        var request = request("GET", "/users/me");
        request.setCookies(new Cookie(AccessTokenCookieService.COOKIE_NAME, ""));

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void resolve_ignoresTokens_onPublicEndpoints() {
        var request = request("POST", "/auth/login");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token");
        request.setCookies(new Cookie(AccessTokenCookieService.COOKIE_NAME, "expired-token"));

        assertThat(resolver.resolve(request)).isNull();
    }

    private MockHttpServletRequest request(String method, String path) {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
