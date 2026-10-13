package com.example.user_service.controller;

import com.example.user_service.dto.RegistrationRequest;
import com.example.user_service.dto.RegistrationResponse;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.security.SecurityConfig;
import com.example.user_service.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
class AuthenticationControllerTest {

    private static final String REGISTER_URL = "/auth/register";

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

    @Test
    void register_returnsCreatedWithResponseBody() throws Exception {
        var id = UUID.randomUUID();
        when(authenticationService.register(any(RegistrationRequest.class))).thenReturn(new RegistrationResponse(
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
        when(authenticationService.register(any(RegistrationRequest.class)))
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
        when(authenticationService.register(any(RegistrationRequest.class)))
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
        when(authenticationService.register(any(RegistrationRequest.class)))
                .thenThrow(new IllegalStateException("database is down"));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
