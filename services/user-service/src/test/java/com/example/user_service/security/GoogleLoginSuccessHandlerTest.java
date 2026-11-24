package com.example.user_service.security;

import com.example.user_service.config.GoogleLoginProperties;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.service.AuthenticationService;
import com.example.user_service.service.GoogleAccount;
import com.example.user_service.service.LoginResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleLoginSuccessHandlerTest {

    private static final String SUCCESS_URL = "http://localhost:4200/";
    private static final String FAILURE_URL = "http://localhost:4200/login?error=google";

    @Mock private AuthenticationService authenticationService;
    @Mock private AuthCookieService authCookieService;

    private GoogleLoginSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        handler = new GoogleLoginSuccessHandler(
                new GoogleLoginProperties(SUCCESS_URL, FAILURE_URL), authenticationService, authCookieService);
        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
        response = new MockHttpServletResponse();
    }

    @Test
    void onSuccess_logsInWithGoogleAccount_setsCookies_andRedirectsToSuccessUrl() throws Exception {
        when(authenticationService.loginWithGoogle(any())).thenReturn(new LoginResult(null, "access", "refresh"));
        when(authCookieService.accessTokenCookie("access")).thenReturn(ResponseCookie.from("access_token", "access").build());
        when(authCookieService.refreshTokenCookie("refresh")).thenReturn(ResponseCookie.from("refresh_token", "refresh").build());

        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        var accountCaptor = ArgumentCaptor.forClass(GoogleAccount.class);
        verify(authenticationService).loginWithGoogle(accountCaptor.capture());
        assertThat(accountCaptor.getValue())
                .isEqualTo(new GoogleAccount("google-sub", "jane.doe@gmail.com", true, "Jane", "Doe"));
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .containsExactly("access_token=access", "refresh_token=refresh");
        assertThat(response.getRedirectedUrl()).isEqualTo(SUCCESS_URL);
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void onSuccess_whenLoginRefused_redirectsToFailureUrlWithoutCookies() throws Exception {
        when(authenticationService.loginWithGoogle(any())).thenThrow(new UserSuspendedException());

        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        assertThat(response.getRedirectedUrl()).isEqualTo(FAILURE_URL);
        assertThat(session.isInvalid()).isTrue();
    }

    private OAuth2AuthenticationToken googleAuthentication() {
        var idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of(
                "sub", "google-sub",
                "email", "jane.doe@gmail.com",
                "email_verified", true,
                "given_name", "Jane",
                "family_name", "Doe"));
        var googleUser = new DefaultOidcUser(List.of(), idToken);
        return new OAuth2AuthenticationToken(googleUser, List.of(), "google");
    }
}
