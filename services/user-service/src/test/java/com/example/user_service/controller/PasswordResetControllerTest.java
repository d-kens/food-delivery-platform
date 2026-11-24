package com.example.user_service.controller;

import com.example.user_service.config.SecurityConfig;
import com.example.user_service.security.GoogleLoginFailureHandler;
import com.example.user_service.security.GoogleLoginSuccessHandler;
import com.example.user_service.exception.InvalidPasswordResetTokenException;
import com.example.user_service.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@Import(SecurityConfig.class)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private GoogleLoginSuccessHandler googleLoginSuccessHandler;

    @MockitoBean
    private GoogleLoginFailureHandler googleLoginFailureHandler;

    @Test
    void forgotPassword_acceptsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane.doe@gmail.com\"}"))
                .andExpect(status().isAccepted());

        verify(passwordResetService).requestReset("jane.doe@gmail.com");
    }

    @Test
    void forgotPassword_rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email[0]").value("Email must be valid"));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void resetPassword_returnsNoContent() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\",\"newPassword\":\"NewPassw0rd!\"}"))
                .andExpect(status().isNoContent());

        verify(passwordResetService).resetPassword("raw-token", "NewPassw0rd!");
    }

    @Test
    void resetPassword_rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword[0]")
                        .value("Password must be between 8 and 72 characters"));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void resetPassword_returnsBadRequest_whenLinkIsInvalid() throws Exception {
        doThrow(new InvalidPasswordResetTokenException()).when(passwordResetService).resetPassword(any(), any());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\",\"newPassword\":\"NewPassw0rd!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid reset link"))
                .andExpect(jsonPath("$.detail").value("This reset link is invalid or has expired"));
    }
}
