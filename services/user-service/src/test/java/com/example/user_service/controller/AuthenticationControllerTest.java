package com.example.user_service.controller;

import com.example.user_service.dto.AuthRequestDto;
import com.example.user_service.dto.AuthResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCredentialException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.config.SecurityConfig;
import com.example.user_service.security.RefreshTokenCookieService;
import com.example.user_service.service.AuthenticationService;
import com.example.user_service.service.LoginResult;
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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
class AuthenticationControllerTest {

    private static final String REGISTER_URL = "/auth/register";
    private static final String LOGIN_URL = "/auth/login";

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
              "lastName": "Doe",
              "phoneNumber": "0707127309"
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

    @Test
    void register_returnsCreatedWithResponseBody() throws Exception {
        var id = UUID.randomUUID();
        when(authenticationService.register(any(RegistrationRequestDto.class))).thenReturn(new RegistrationResponseDto(
                id,
                "jane.doe@gmail.com",
                "0707127309",
                "Jane",
                "Doe",
                "USER",
                "ACTIVE",
                Instant.parse("2026-01-01T10:00:00Z")
        ));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("jane.doe@gmail.com"))
                .andExpect(jsonPath("$.role").value("USER"))
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
    void register_returnsConflict_whenPhoneNumberAlreadyExists() throws Exception {
        when(authenticationService.register(any(RegistrationRequestDto.class)))
                .thenThrow(new PhoneNumberAlreadyExistsException("An account with this phone number already exists"));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Phone number already exists"))
                .andExpect(jsonPath("$.detail").value("An account with this phone number already exists"));
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
                                "access-token",
                                userId,
                                "Jane",
                                "Doe",
                                "jane.doe@gmail.com",
                                "USER"
                        ),
                        "refresh-token"
                )
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
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value("jane.doe@gmail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
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
}
