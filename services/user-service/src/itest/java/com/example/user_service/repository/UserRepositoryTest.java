package com.example.user_service.repository;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.common.Role;
import com.example.user_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UserRepositoryTest extends AbstractIntegrationTest {

    private static final String EMAIL = "jane.doe@gmail.com";
    private static final String PHONE_NUMBER = "0707127309";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.persistAndFlush(buildUser(EMAIL, PHONE_NUMBER));
    }

    @Test
    void existsByEmail_returnsTrue_whenEmailExists() {
        assertTrue(userRepository.existsByEmail(EMAIL));
    }

    @Test
    void existsByEmail_returnsFalse_whenEmailDoesNotExist() {
        assertFalse(userRepository.existsByEmail("john.doe@gmail.com"));
    }

    @Test
    void existsByPhoneNumber_returnsTrue_whenPhoneNumberExists() {
        assertTrue(userRepository.existsByPhoneNumber(PHONE_NUMBER));
    }

    @Test
    void existsByPhoneNumber_returnsFalse_whenPhoneNumberDoesNotExist() {
        assertFalse(userRepository.existsByPhoneNumber("0711000000"));
    }

    private User buildUser(String email, String phoneNumber) {
        return User
                .builder()
                .email(email)
                .phoneNumber(phoneNumber)
                .firstName("Jane")
                .lastName("Doe")
                .passwordHash("hashed")
                .role(Role.USER)
                .build();
    }
}
