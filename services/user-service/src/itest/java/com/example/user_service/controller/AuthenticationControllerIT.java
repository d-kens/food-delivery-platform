package com.example.user_service.controller;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.UserStatus;
import com.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIT extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/auth/register";
    private static final String EMAIL = "jane.doe@gmail.com";
    private static final String PASSWORD = "Str0ngPassword!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void register_savesUserAndReturnsResponse() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(EMAIL, "0707127309")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.phoneNumber").value("0707127309"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(userRepository.findAll())
                .singleElement()
                .satisfies(user -> {
                    assertThat(user.getEmail()).isEqualTo(EMAIL);
                    assertThat(user.getRole()).isEqualTo(Role.USER);
                    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
                    assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
                });
    }

    @Test
    void register_returnsConflict_whenEmailAlreadyExists() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(EMAIL, "0707127309")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(EMAIL, "0711000000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    private String registrationJson(String email, String phoneNumber) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "phoneNumber": "%s"
                }
                """.formatted(email, PASSWORD, phoneNumber);
    }
}
