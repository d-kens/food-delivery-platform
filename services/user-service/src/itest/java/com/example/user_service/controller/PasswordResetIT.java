package com.example.user_service.controller;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.entity.OutboxMessage;
import com.example.user_service.repository.OutboxMessageRepository;
import com.example.user_service.repository.PasswordResetTokenRepository;
import com.example.user_service.repository.RefreshTokenRepository;
import com.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetIT extends AbstractIntegrationTest {

    private static final String EMAIL = "jane.doe@gmail.com";
    private static final String OLD_PASSWORD = "Str0ngPassword!";
    private static final String NEW_PASSWORD = "N3wPassword!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @BeforeEach
    void registerUser() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","firstName":"Jane","lastName":"Doe"}
                                """.formatted(EMAIL, OLD_PASSWORD)))
                .andExpect(status().isCreated());
    }

    @AfterEach
    void tearDown() {
        outboxMessageRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void resetFlow_changesPasswordLogsOutEverywhereAndCannotBeReused() throws Exception {
        login(OLD_PASSWORD, 200);
        requestReset(EMAIL);
        var token = tokenFromResetEmail();

        reset(token, NEW_PASSWORD)
                .andExpect(status().isNoContent());

        login(NEW_PASSWORD, 200);
        login(OLD_PASSWORD, 401);
        assertThat(refreshTokenRepository.findAll())
                .filteredOn(refreshToken -> refreshToken.getRevokedAt() == null)
                .hasSize(1);
        assertThat(notificationTypes()).contains("RESET_PASSWORD", "UPDATED_PASSWORD");

        reset(token, "An0therPassword!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("This reset link is invalid or has expired"));
    }

    @Test
    void forgotPassword_acceptsUnknownEmailWithoutSendingAnything() throws Exception {
        requestReset("nobody@gmail.com");

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        assertThat(notificationTypes()).doesNotContain("RESET_PASSWORD");
    }

    @Test
    void forgotPassword_sendsOneLinkWhenRequestedTwiceInAMinute() throws Exception {
        requestReset(EMAIL);
        requestReset(EMAIL);

        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
        assertThat(notificationTypes()).containsOnlyOnce("RESET_PASSWORD");
    }

    @Test
    void resetPassword_rejectsExpiredLink() throws Exception {
        requestReset(EMAIL);
        var token = tokenFromResetEmail();
        var stored = passwordResetTokenRepository.findAll().getFirst();
        stored.setExpiresAt(Instant.now().minusSeconds(1));
        passwordResetTokenRepository.save(stored);

        reset(token, NEW_PASSWORD)
                .andExpect(status().isBadRequest());
        login(OLD_PASSWORD, 200);
    }

    private void requestReset(String email) throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isAccepted());
    }

    private ResultActions reset(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, newPassword)));
    }

    private void login(String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, password)))
                .andExpect(status().is(expectedStatus));
    }

    private List<JsonNode> notifications() {
        return outboxMessageRepository.findAll().stream()
                .filter(message -> message.getTopic().equals("notifications"))
                .map(OutboxMessage::getPayload)
                .map(jsonMapper::readTree)
                .toList();
    }

    private List<String> notificationTypes() {
        return notifications().stream().map(json -> json.get("type").asString()).toList();
    }

    private String tokenFromResetEmail() {
        var resetUrl = notifications().stream()
                .filter(json -> json.get("type").asString().equals("RESET_PASSWORD"))
                .findFirst()
                .orElseThrow()
                .get("payload").get("resetUrl").asString();
        assertThat(resetUrl).startsWith("http://localhost:4200/reset-password?token=");
        return UriComponentsBuilder.fromUriString(resetUrl).build().getQueryParams().getFirst("token");
    }
}
