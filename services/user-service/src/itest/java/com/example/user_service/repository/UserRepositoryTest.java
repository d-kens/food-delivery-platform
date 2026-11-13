package com.example.user_service.repository;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;

import static com.example.user_service.repository.UserSpecifications.hasRole;
import static com.example.user_service.repository.UserSpecifications.hasStatus;
import static com.example.user_service.repository.UserSpecifications.matchesSearch;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void findAll_filtersByStatus() {
        var suspended = buildUser("john.doe@gmail.com", null);
        suspended.setStatus(UserStatus.SUSPENDED);
        entityManager.persistAndFlush(suspended);

        assertThat(emails(hasStatus(UserStatus.SUSPENDED))).containsExactly("john.doe@gmail.com");
    }

    @Test
    void findAll_filtersByRole() {
        var rider = buildUser("rider@gmail.com", null);
        rider.setRoles(Set.of(Role.USER, Role.RIDER));
        entityManager.persistAndFlush(rider);

        assertThat(emails(hasRole(Role.RIDER))).containsExactly("rider@gmail.com");
        assertThat(emails(hasRole(Role.USER))).containsExactlyInAnyOrder(EMAIL, "rider@gmail.com");
    }

    @Test
    void findAll_searchesEmailAndNamesIgnoringCase() {
        var john = buildUser("jsmith@gmail.com", null);
        john.setFirstName("John");
        john.setLastName("Smith");
        entityManager.persistAndFlush(john);

        assertThat(emails(matchesSearch("SMITH"))).containsExactly("jsmith@gmail.com");
        assertThat(emails(matchesSearch("jane.doe"))).containsExactly(EMAIL);
    }

    @Test
    void findAll_treatsLikeWildcardsInSearchLiterally() {
        assertThat(emails(matchesSearch("%"))).isEmpty();
        assertThat(emails(matchesSearch("_"))).isEmpty();
    }

    @Test
    void findAll_returnsEveryone_whenNoFilters() {
        entityManager.persistAndFlush(buildUser("john.doe@gmail.com", null));

        assertThat(emails(hasStatus(null).and(hasRole(null)).and(matchesSearch(" ")))).hasSize(2);
    }

    private List<String> emails(Specification<User> spec) {
        return userRepository.findAll(spec, Pageable.unpaged()).map(User::getEmail).getContent();
    }

    private User buildUser(String email, String phoneNumber) {
        return User
                .builder()
                .email(email)
                .phoneNumber(phoneNumber)
                .firstName("Jane")
                .lastName("Doe")
                .passwordHash("hashed")
                .roles(Set.of(Role.USER))
                .build();
    }
}
