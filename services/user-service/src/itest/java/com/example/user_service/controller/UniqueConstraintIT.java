package com.example.user_service.controller;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.common.Role;
import com.example.user_service.entity.User;
import com.example.user_service.repository.OutboxMessageRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UniqueConstraintIT extends AbstractIntegrationTest {

    private static final String JANE_EMAIL = "jane.doe@gmail.com";
    private static final String JANE_PHONE = "+254707127309";
    private static final String JOHN_EMAIL = "john.smith@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private JwtService jwtService;

    private User john;

    @BeforeEach
    void setUp() {
        saveUser(JANE_EMAIL, JANE_PHONE);
        john = saveUser(JOHN_EMAIL, null);
        doReturn(false).when(userRepository).existsByEmail(anyString());
        doReturn(false).when(userRepository).existsByPhoneNumber(anyString());
    }

    @AfterEach
    void tearDown() {
        outboxMessageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_returnsConflict_whenEmailIsTakenBetweenCheckAndInsert() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Str0ngPassword!",
                                  "firstName": "Jane",
                                  "lastName": "Again"
                                }
                                """.formatted(JANE_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"))
                .andExpect(jsonPath("$.detail").value("An account with this email already exists"));

        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(outboxMessageRepository.count()).isZero();
    }

    @Test
    void updateMe_returnsConflict_whenPhoneNumberIsTakenBetweenCheckAndUpdate() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(john))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\": \"%s\"}".formatted(JANE_PHONE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Phone number already exists"))
                .andExpect(jsonPath("$.detail").value("An account with this phone number already exists"));

        assertThat(userRepository.findById(john.getId()).orElseThrow().getPhoneNumber()).isNull();
        assertThat(outboxMessageRepository.count()).isZero();
    }

    private User saveUser(String email, String phoneNumber) {
        return userRepository.save(User.builder()
                .email(email)
                .phoneNumber(phoneNumber)
                .firstName("Test")
                .lastName("User")
                .passwordHash("hashed")
                .roles(Set.of(Role.USER))
                .build());
    }
}
