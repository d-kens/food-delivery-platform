package com.example.user_service.controller;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.entity.User;
import com.example.user_service.repository.RefreshTokenRepository;
import com.example.user_service.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT extends AbstractIntegrationTest {

    private static final String USERS_URL = "/users";
    private static final String LOGIN_URL = "/auth/login";
    private static final String PASSWORD = "Str0ngPassword!";
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String JANE_EMAIL = "jane.doe@gmail.com";
    private static final String JOHN_EMAIL = "john.smith@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User jane;
    private User john;

    @BeforeEach
    void setUp() {
        admin = saveUser(ADMIN_EMAIL, "Ada", "Admin", null, Role.ADMIN);
        jane = saveUser(JANE_EMAIL, "Jane", "Doe", "+254707127309", Role.USER);
        john = saveUser(JOHN_EMAIL, "John", "Smith", "+254700000001", Role.USER);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getUsers_returnsUsersFromDatabase_forAdmin() throws Exception {
        mockMvc.perform(get(USERS_URL).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[*].email", containsInAnyOrder(ADMIN_EMAIL, JANE_EMAIL, JOHN_EMAIL)));
    }

    @Test
    void getUsers_appliesFilters() throws Exception {
        john.setStatus(UserStatus.SUSPENDED);
        userRepository.save(john);
        String token = bearer(ADMIN_EMAIL);

        mockMvc.perform(get(USERS_URL).param("status", "SUSPENDED").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", containsInAnyOrder(JOHN_EMAIL)));

        mockMvc.perform(get(USERS_URL).param("role", "ADMIN").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", containsInAnyOrder(ADMIN_EMAIL)));

        mockMvc.perform(get(USERS_URL).param("search", "DOE").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", containsInAnyOrder(JANE_EMAIL)));
    }

    @Test
    void getUsers_paginates() throws Exception {
        mockMvc.perform(get(USERS_URL).param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getUsers_returnsForbidden_forRegularUser() throws Exception {
        mockMvc.perform(get(USERS_URL).header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_returnsUser_forAdmin() throws Exception {
        mockMvc.perform(get(USERS_URL + "/" + jane.getId()).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(jane.getId().toString()))
                .andExpect(jsonPath("$.email").value(JANE_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    void getUserById_returnsNotFound_whenMissing() throws Exception {
        mockMvc.perform(get(USERS_URL + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));
    }

    @Test
    void getUserById_returnsForbidden_forRegularUser() throws Exception {
        mockMvc.perform(get(USERS_URL + "/" + john.getId()).header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_suspendsUserAndRevokesTheirRefreshTokens() throws Exception {
        login(JANE_EMAIL);
        login(JOHN_EMAIL);

        mockMvc.perform(patch(USERS_URL + "/" + jane.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(refreshTokenRepository.findAll())
                .filteredOn(token -> token.getUser().getId().equals(jane.getId()))
                .isNotEmpty()
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
        assertThat(refreshTokenRepository.findAll())
                .filteredOn(token -> !token.getUser().getId().equals(jane.getId()))
                .isNotEmpty()
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNull());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(JANE_EMAIL, PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_reactivatesSuspendedUser() throws Exception {
        jane.setStatus(UserStatus.SUSPENDED);
        userRepository.save(jane);

        mockMvc.perform(patch(USERS_URL + "/" + jane.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateStatus_returnsConflict_whenAdminChangesOwnStatus() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/" + admin.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isConflict());

        assertThat(userRepository.findById(admin.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateStatus_returnsForbidden_forRegularUser() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/" + john.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(john.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateMe_persistsNameAndPhoneChanges() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \" Janet \", \"phoneNumber\": \"+254711111111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Janet"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        var saved = userRepository.findById(jane.getId()).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Janet");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getPhoneNumber()).isEqualTo("+254711111111");
        assertThat(saved.getEmail()).isEqualTo(JANE_EMAIL);
    }

    @Test
    void updateMe_changesEmail_whenCurrentPasswordIsCorrect() throws Exception {
        String newEmail = "janet@gmail.com";

        mockMvc.perform(patch(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"currentPassword\": \"%s\"}".formatted(newEmail, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getEmail()).isEqualTo(newEmail);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(newEmail, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void updateMe_returnsBadRequest_andKeepsEmail_whenCurrentPasswordIsWrong() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"janet@gmail.com\", \"currentPassword\": \"Wr0ngPassword!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Current password is incorrect"));

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getEmail()).isEqualTo(JANE_EMAIL);
    }

    @Test
    void updateMe_returnsConflict_whenEmailBelongsToAnotherUser() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"currentPassword\": \"%s\"}".formatted(JOHN_EMAIL, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"));

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getEmail()).isEqualTo(JANE_EMAIL);
    }

    @Test
    void updateMe_returnsConflict_whenPhoneNumberBelongsToAnotherUser() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\": \"%s\"}".formatted(john.getPhoneNumber())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Phone number already exists"));

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getPhoneNumber()).isEqualTo("+254707127309");
    }

    @Test
    void updateMe_returnsBadRequest_whenFieldsInvalid() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"  \", \"phoneNumber\": \"12ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.phoneNumber").exists());

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void updateMe_withAccessTokenCookie_isRejected_withoutCsrfToken() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me")
                        .cookie(new Cookie("access_token", login(JANE_EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Janet\"}"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void updateRoles_replacesRolesAndNextLoginCarriesThem() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + jane.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"USER\", \"ADMIN\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER", "ADMIN")));

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getRoles())
                .containsExactlyInAnyOrder(Role.USER, Role.ADMIN);

        mockMvc.perform(get(USERS_URL).header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    void updateRoles_canRemoveAdmin() throws Exception {
        var otherAdmin = saveUser("second.admin@gmail.com", "Sam", "Second", null, Role.ADMIN);

        mockMvc.perform(put(USERS_URL + "/" + otherAdmin.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"USER\"]}"))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(otherAdmin.getId()).orElseThrow().getRoles()).containsExactly(Role.USER);

        mockMvc.perform(get(USERS_URL).header(HttpHeaders.AUTHORIZATION, bearer("second.admin@gmail.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRoles_returnsConflict_whenAdminChangesOwnRoles() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + admin.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"USER\"]}"))
                .andExpect(status().isConflict());

        assertThat(userRepository.findById(admin.getId()).orElseThrow().getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    void updateRoles_returnsForbidden_forRegularUser() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + jane.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"ADMIN\"]}"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getRoles()).containsExactly(Role.USER);
    }

    @Test
    void updateRoles_returnsNotFound_whenUserMissing() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + UUID.randomUUID() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"ADMIN\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMe_anonymisesAccountRevokesSessionsAndClearsCookies() throws Exception {
        login(JANE_EMAIL);
        login(JOHN_EMAIL);
        String accessToken = login(JANE_EMAIL);

        List<String> setCookies = mockMvc.perform(delete(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteAccountJson(PASSWORD)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(setCookies)
                .anySatisfy(cookie -> assertThat(cookie).startsWith("access_token=").contains("Max-Age=0"))
                .anySatisfy(cookie -> assertThat(cookie).startsWith("refresh_token=").contains("Max-Age=0"));

        var saved = userRepository.findById(jane.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(saved.getDeletedAt()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("deleted-" + jane.getId() + "@deleted.invalid");
        assertThat(saved.getPhoneNumber()).isNull();
        assertThat(saved.getFirstName()).isEqualTo("Deleted");
        assertThat(saved.getLastName()).isEqualTo("User");
        assertThat(saved.getRoles()).isEmpty();
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isFalse();

        assertThat(refreshTokenRepository.findAll())
                .filteredOn(token -> token.getUser().getId().equals(jane.getId()))
                .isNotEmpty()
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
        assertThat(refreshTokenRepository.findAll())
                .filteredOn(token -> !token.getUser().getId().equals(jane.getId()))
                .isNotEmpty()
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNull());

        mockMvc.perform(get(USERS_URL + "/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(JANE_EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMe_freesEmailAndPhoneForReuse() throws Exception {
        mockMvc.perform(delete(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteAccountJson(PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "firstName": "Jane",
                                  "lastName": "Doe"
                                }
                                """.formatted(JANE_EMAIL, PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\": \"+254707127309\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMe_returnsBadRequest_andKeepsAccount_whenCurrentPasswordIsWrong() throws Exception {
        mockMvc.perform(delete(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteAccountJson("Wr0ngPassword!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Current password is incorrect"));

        var saved = userRepository.findById(jane.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getEmail()).isEqualTo(JANE_EMAIL);
    }

    @Test
    void deleteMe_withAccessTokenCookie_isRejected_withoutCsrfToken() throws Exception {
        mockMvc.perform(delete(USERS_URL + "/me")
                        .cookie(new Cookie("access_token", login(JANE_EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteAccountJson(PASSWORD)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void deletedUsers_areListedByStatusFilter_andCannotBeChangedByAdmin() throws Exception {
        mockMvc.perform(delete(USERS_URL + "/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteAccountJson(PASSWORD)))
                .andExpect(status().isNoContent());
        String adminToken = bearer(ADMIN_EMAIL);

        mockMvc.perform(get(USERS_URL).param("status", "DELETED").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].userId", containsInAnyOrder(jane.getId().toString())));

        mockMvc.perform(patch(USERS_URL + "/" + jane.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put(USERS_URL + "/" + jane.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"ADMIN\"]}"))
                .andExpect(status().isConflict());

        assertThat(userRepository.findById(jane.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void updateStatus_returnsBadRequest_whenAdminSetsDeleted() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/" + john.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DELETED\"}"))
                .andExpect(status().isBadRequest());

        var saved = userRepository.findById(john.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getEmail()).isEqualTo(JOHN_EMAIL);
    }

    @Test
    void changePassword_updatesPasswordRevokesSessionsAndClearsCookies() throws Exception {
        login(JANE_EMAIL);
        login(JOHN_EMAIL);
        String newPassword = "N3wPassword!";

        List<String> setCookies = mockMvc.perform(patch(USERS_URL + "/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson(PASSWORD, newPassword)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(setCookies)
                .anySatisfy(cookie -> assertThat(cookie).startsWith("access_token=").contains("Max-Age=0"))
                .anySatisfy(cookie -> assertThat(cookie).startsWith("refresh_token=").contains("Max-Age=0"));

        var saved = userRepository.findById(jane.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, saved.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isFalse();

        assertThat(refreshTokenRepository.findAll())
                .filteredOn(token -> token.getUser().getId().equals(jane.getId()))
                .isNotEmpty()
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
        assertThat(refreshTokenRepository.findAll())
                .filteredOn(token -> !token.getUser().getId().equals(jane.getId()))
                .isNotEmpty()
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNull());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(JANE_EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(JANE_EMAIL, newPassword)))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_returnsBadRequest_andKeepsPassword_whenCurrentPasswordIsWrong() throws Exception {
        login(JANE_EMAIL);

        mockMvc.perform(patch(USERS_URL + "/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JANE_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("Wr0ngPassword!", "N3wPassword!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Current password is incorrect"));

        var saved = userRepository.findById(jane.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(refreshTokenRepository.findAll())
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNull());
    }

    @Test
    void changePassword_withAccessTokenCookie_isRejected_withoutCsrfToken() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me/password")
                        .cookie(new Cookie("access_token", login(JANE_EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson(PASSWORD, "N3wPassword!")))
                .andExpect(status().isForbidden());

        var saved = userRepository.findById(jane.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
    }

    private String deleteAccountJson(String currentPassword) {
        return """
                {
                  "currentPassword": "%s"
                }
                """.formatted(currentPassword);
    }

    private String changePasswordJson(String currentPassword, String newPassword) {
        return """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s"
                }
                """.formatted(currentPassword, newPassword);
    }

    private User saveUser(String email, String firstName, String lastName, String phoneNumber, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .roles(EnumSet.of(role))
                .build());
    }

    private String bearer(String email) throws Exception {
        return "Bearer " + login(email);
    }

    private String login(String email) throws Exception {
        List<String> setCookies = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        String accessCookie = setCookies.stream()
                .filter(header -> header.startsWith("access_token="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No access_token cookie in " + setCookies));
        return accessCookie.substring(accessCookie.indexOf('=') + 1, accessCookie.indexOf(';'));
    }

    private String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
