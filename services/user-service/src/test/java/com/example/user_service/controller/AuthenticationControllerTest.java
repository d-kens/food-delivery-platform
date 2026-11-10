package com.example.user_service.controller;

import com.example.user_service.dto.AuthRequestDto;
import com.example.user_service.dto.AuthResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCredentialException;
import com.example.user_service.exception.InvalidRefreshTokenException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.config.SecurityConfig;
import com.example.user_service.security.AccessTokenCookieService;
import com.example.user_service.security.RefreshTokenCookieService;
import com.example.user_service.service.AuthenticationService;
import com.example.user_service.service.LoginResult;
import com.example.user_service.service.RefreshResult;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
class AuthenticationControllerTest {

    private static final String LOGIN_URL = "/auth/login";
    private static final String REGISTER_URL = "/auth/register";
    private static final String REFRESH_TOKEN_URL = "/auth/refresh-token";

    private static final String VALID_LOGIN_REQUEST = """
            {
              "email": "jane.doe@gmail.com",
              "password": "T3st@12367"
            }
            """;

    private static final String VALID_REQUEST = """
            {
              "email": "jane.doe@gmail.com",
              "password": "T3st@12367",
              "firstName": "Jane",
              "lastName": "Doe"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    // Required by the resource server config in SecurityConfig; not loaded by @WebMvcTest.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RefreshTokenCookieService refreshTokenCookieService;

    @MockitoBean
    private AccessTokenCookieService accessTokenCookieService;

    @Test
    void register_returnsCreatedWithResponseBody() throws Exception {
        var id = UUID.randomUUID();
        when(authenticationService.register(any(RegistrationRequestDto.class))).thenReturn(new RegistrationResponseDto(
                id,
                "jane.doe@gmail.com",
                "Jane",
                "Doe",
                List.of("USER"),
                "ACTIVE",
                Instant.parse("2026-01-01T10:00:00Z")
        ));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("jane.doe@gmail.com"))
                .andExpect(jsonPath("$.roles", contains("USER")))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void register_returnsBadRequestWithFieldErrors_whenRequestIsInvalid() throws Exception {
        var invalidRequest = """
                {
                  "email": "not-an-email",
                  "password": "short",
                  "firstName": "",
                  "lastName": "Doe"
                }
                """;

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").doesNotExist());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void register_returnsConflict_whenEmailAlreadyExists() throws Exception {
        when(authenticationService.register(any(RegistrationRequestDto.class)))
                .thenThrow(new EmailAlreadyExistsException("An account with this email already exists"));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"))
                .andExpect(jsonPath("$.detail").value("An account with this email already exists"));
    }

    @Test
    void register_returnsInternalServerError_whenUnexpectedErrorOccurs() throws Exception {
        when(authenticationService.register(any(RegistrationRequestDto.class)))
                .thenThrow(new IllegalStateException("database is down"));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void login_returnsOkWithLoginResponse() throws Exception {
        var userId = UUID.randomUUID().toString();

        when(authenticationService.login(any(AuthRequestDto.class))).thenReturn(
                new LoginResult(
                        new AuthResponseDto(
                                userId,
                                "Jane",
                                "Doe",
                                "jane.doe@gmail.com",
                                List.of("USER")
                        ),
                        "access-token",
                        "refresh-token"
                )
        );
        when(accessTokenCookieService.createAccessTokenCookie("access-token")).thenReturn(
                ResponseCookie.from(AccessTokenCookieService.COOKIE_NAME, "access-token")
                        .path("/")
                        .httpOnly(true)
                        .build()
        );
        when(refreshTokenCookieService.createRefreshTokenCookie("refresh-token")).thenReturn(
                ResponseCookie.from(RefreshTokenCookieService.COOKIE_NAME, "refresh-token")
                        .path("/auth")
                        .httpOnly(true)
                        .build()
        );

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_REQUEST))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItems(
                        containsString("access_token=access-token"),
                        containsString("refresh_token=refresh-token"))))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value("jane.doe@gmail.com"))
                .andExpect(jsonPath("$.roles", contains("USER")));
    }

    @Test
    void login_returnsBadRequestWithFieldErrors_whenRequestIsInvalid() throws Exception {
        var invalidRequest = """
                {
                  "email": "not-an-email",
                  "password": ""
                }
                """;

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(authenticationService, never()).login(any());
    }

    @Test
    void login_returnsUnauthorized_whenCredentialsAreInvalid() throws Exception {
        when(authenticationService.login(any(AuthRequestDto.class)))
                .thenThrow(new InvalidCredentialException("Invalid email or password"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Credentials"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void login_returnsForbidden_whenUserIsSuspended() throws Exception {
        when(authenticationService.login(any(AuthRequestDto.class)))
                .thenThrow(new UserSuspendedException("Your account has been suspended"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account Suspended"))
                .andExpect(jsonPath("$.detail").value("Your account has been suspended"));
    }

    @Test
    void refreshToken_returnsOkWithAuthenticationResponse() throws Exception {
        var userId = UUID.randomUUID().toString();
        String refreshToken = "valid-refresh-token";
        var authResponse = new AuthResponseDto(
                userId,
                "Jane",
                "Doe",
                "jane.doe@gmail.com",
                List.of("USER")
        );

        when(authenticationService.refreshToken(refreshToken)).thenReturn(new RefreshResult(authResponse, "access-token"));
        when(accessTokenCookieService.createAccessTokenCookie("access-token")).thenReturn(
                ResponseCookie.from(AccessTokenCookieService.COOKIE_NAME, "access-token").path("/").httpOnly(true).build()
        );

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(csrf())
                    .cookie(new Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value("jane.doe@gmail.com"))
                .andExpect(jsonPath("$.roles", contains("USER")))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItems(containsString("access_token=access-token"))))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void refreshToken_returnsUnauthorized_whenCookieIsMissing() throws Exception {
        when(authenticationService.refreshToken(null))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Refresh Token"))
                .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));

        verify(authenticationService).refreshToken(null);
    }

    @Test
    void refreshToken_returnsUnauthorized_whenTokenIsInvalid() throws Exception {
        String refreshToken = "invalid-refresh-token";
        when(authenticationService.refreshToken(refreshToken))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(csrf())
                    .cookie(new Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Refresh Token"))
                .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));

        verify(authenticationService).refreshToken(refreshToken);
    }

    @Test
    void refreshToken_returnsForbidden_whenUserIsSuspended() throws Exception {
        String refreshToken = "valid-refresh-token";
        when(authenticationService.refreshToken(refreshToken))
                .thenThrow(new UserSuspendedException("Your account has been suspended"));

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(csrf())
                        .cookie(new Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account Suspended"))
                .andExpect(jsonPath("$.detail").value("Your account has been suspended"));

        verify(authenticationService).refreshToken(refreshToken);
    }


}
