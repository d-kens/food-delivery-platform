package com.example.user_service.controller;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.repository.RefreshTokenRepository;
import com.example.user_service.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIT extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/auth/register";
    private static final String LOGIN_URL = "/auth/login";
    private static final String REFRESH_TOKEN_URL = "/auth/refresh-token";
    private static final String ME_URL = "/users/me";
    private static final String EMAIL = "jane.doe@gmail.com";
    private static final String PASSWORD = "Str0ngPassword!";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_savesUserAndReturnsResponse() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.roles", contains("USER")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(userRepository.findAll())
                .singleElement()
                .satisfies(user -> {
                    assertThat(user.getEmail()).isEqualTo(EMAIL);
                    assertThat(user.getRoles()).containsExactly(Role.USER);
                    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
                    assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
                });
    }

    @Test
    void register_returnsConflict_whenEmailAlreadyExists() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(EMAIL)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void login_returnsSignedAccessToken_whenCredentialsAreValid() throws Exception {
        registerUser();
        var user = userRepository.findByEmail(EMAIL).orElseThrow();

        var setCookies = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.roles", contains("USER")))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        var jwt = jwtDecoder.decode(cookieValue(cookieHeader(setCookies, "access_token")));

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void login_setsRefreshTokenCookie_whenCredentialsAreValid() throws Exception {
        registerUser();

        String setCookie = cookieHeader(mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE), "refresh_token");

        assertThat(setCookie)
                .startsWith("refresh_token=")
                .contains("Path=/api/auth")
                .contains("Max-Age=604800")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict");
        assertThat(refreshTokenFrom(setCookie)).isNotBlank();
    }

    @Test
    void login_storesHashedRefreshToken_whenCredentialsAreValid() throws Exception {
        registerUser();
        var user = userRepository.findByEmail(EMAIL).orElseThrow();

        Instant before = Instant.now();
        String setCookie = cookieHeader(mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE), "refresh_token");
        Instant after = Instant.now();

        String rawToken = refreshTokenFrom(setCookie);

        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getUser().getId()).isEqualTo(user.getId());
                    assertThat(token.getTokenHash()).isEqualTo(sha256Hex(rawToken));
                    assertThat(token.getTokenHash()).isNotEqualTo(rawToken);
                    assertThat(token.getExpiresAt()).isBetween(
                            before.plus(REFRESH_TOKEN_TTL).truncatedTo(ChronoUnit.MILLIS),
                            after.plus(REFRESH_TOKEN_TTL).plusMillis(1));
                    assertThat(token.getRevokedAt()).isNull();
                });
    }

    @Test
    void login_returnsUnauthorized_whenPasswordIsWrong() throws Exception {
        registerUser();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, "Wr0ngPassword!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, contains(startsWith("XSRF-TOKEN="))));

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void login_returnsUnauthorized_whenEmailIsUnknown() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown@gmail.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, contains(startsWith("XSRF-TOKEN="))));

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void login_returnsForbidden_whenUserIsSuspended() throws Exception {
        registerUser();
        var user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account Suspended"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, contains(startsWith("XSRF-TOKEN="))));

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void refreshToken_returnsNewAccessToken_whenRefreshTokenIsValid() throws Exception {
        registerUser();
        var user = userRepository.findByEmail(EMAIL).orElseThrow();
        String refreshToken = loginAndGetRefreshToken();

        var setCookies = mockMvc.perform(post(REFRESH_TOKEN_URL).with(xsrf())
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.roles", contains("USER")))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        var jwt = jwtDecoder.decode(cookieValue(cookieHeader(setCookies, "access_token")));

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void refreshToken_doesNotRotateRefreshToken() throws Exception {
        registerUser();
        String refreshToken = loginAndGetRefreshToken();

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(xsrf())
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, everyItem(not(startsWith("refresh_token=")))));

        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getTokenHash()).isEqualTo(sha256Hex(refreshToken));
                    assertThat(token.getRevokedAt()).isNull();
                });
    }

    @Test
    void refreshToken_returnsUnauthorized_whenCookieIsMissing() throws Exception {
        mockMvc.perform(post(REFRESH_TOKEN_URL).with(xsrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Refresh Token"))
                .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void refreshToken_returnsUnauthorized_whenTokenIsUnknown() throws Exception {
        registerUser();
        loginAndGetRefreshToken();

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(xsrf())
                        .cookie(new Cookie("refresh_token", "unknown-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Refresh Token"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void refreshToken_returnsUnauthorized_whenTokenIsExpired() throws Exception {
        registerUser();
        String refreshToken = loginAndGetRefreshToken();
        var storedToken = refreshTokenRepository.findAll().getFirst();
        storedToken.setExpiresAt(Instant.now().minusSeconds(60));
        refreshTokenRepository.save(storedToken);

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(xsrf())
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Refresh Token"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void refreshToken_returnsUnauthorized_whenTokenIsRevoked() throws Exception {
        registerUser();
        String refreshToken = loginAndGetRefreshToken();
        var storedToken = refreshTokenRepository.findAll().getFirst();
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(xsrf())
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Refresh Token"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void refreshToken_returnsForbidden_whenUserIsSuspended() throws Exception {
        registerUser();
        String refreshToken = loginAndGetRefreshToken();
        var user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        mockMvc.perform(post(REFRESH_TOKEN_URL).with(xsrf())
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account Suspended"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void login_setsAccessTokenCookie_whenCredentialsAreValid() throws Exception {
        registerUser();

        var setCookies = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(cookieHeader(setCookies, "access_token"))
                .contains("Path=/api")
                .contains("Max-Age=900")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict");
    }

    @Test
    void me_returnsCurrentUser_whenAccessTokenCookieIsValid() throws Exception {
        registerUser();
        var user = userRepository.findByEmail(EMAIL).orElseThrow();

        mockMvc.perform(get(ME_URL).cookie(new Cookie("access_token", loginAndGetAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.roles", contains("USER")));
    }

    @Test
    void me_acceptsAccessTokenInAuthorizationHeader() throws Exception {
        registerUser();

        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAndGetAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void me_returnsUnauthorized_withoutAccessToken() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsUnauthorized_whenAccessTokenCookieIsInvalid() throws Exception {
        mockMvc.perform(get(ME_URL).cookie(new Cookie("access_token", "not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoints_ignoreAnInvalidAccessTokenCookie() throws Exception {
        registerUser();

        mockMvc.perform(post(LOGIN_URL).with(xsrf())
                        .cookie(new Cookie("access_token", "expired-or-garbage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void everyResponse_setsReadableXsrfCookie() throws Exception {
        var setCookies = mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(cookieHeader(setCookies, "XSRF-TOKEN"))
                .contains("Path=/")
                .doesNotContain("HttpOnly");
    }

    @Test
    void cookieAuthenticatedPost_isRejected_withoutCsrfToken() throws Exception {
        registerUser();
        String refreshToken = loginAndGetRefreshToken();

        mockMvc.perform(post(REFRESH_TOKEN_URL).cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, everyItem(not(startsWith("access_token=")))));
    }

    @Test
    void cookieAuthenticatedPost_isAccepted_withMatchingXsrfCookieAndHeader() throws Exception {
        registerUser();
        String refreshToken = loginAndGetRefreshToken();
        String xsrfToken = cookieValue(cookieHeader(
                mockMvc.perform(get(ME_URL)).andReturn().getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                "XSRF-TOKEN"));

        mockMvc.perform(post(REFRESH_TOKEN_URL)
                        .cookie(new Cookie("refresh_token", refreshToken), new Cookie("XSRF-TOKEN", xsrfToken))
                        .header("X-XSRF-TOKEN", xsrfToken))
                .andExpect(status().isOk());
    }

    @Test
    void cookieAuthenticatedPost_isRejected_whenXsrfHeaderDoesNotMatchCookie() throws Exception {
        registerUser();
        String refreshToken = loginAndGetRefreshToken();

        mockMvc.perform(post(REFRESH_TOKEN_URL)
                        .cookie(new Cookie("refresh_token", refreshToken), new Cookie("XSRF-TOKEN", "real-token"))
                        .header("X-XSRF-TOKEN", "forged-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postsWithoutAuthCookies_doNotNeedCsrfToken() throws Exception {
        registerUser();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk());
    }

    private String loginAndGetAccessToken() throws Exception {
        var setCookies = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);
        return cookieValue(cookieHeader(setCookies, "access_token"));
    }

    private String loginAndGetRefreshToken() throws Exception {
        String setCookie = cookieHeader(mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE), "refresh_token");
        return refreshTokenFrom(setCookie);
    }

    private String refreshTokenFrom(String setCookie) {
        return cookieValue(setCookie);
    }

    private RequestPostProcessor xsrf() {
        return request -> {
            var xsrfCookie = new Cookie("XSRF-TOKEN", "test-xsrf-token");
            var existing = request.getCookies() == null ? new Cookie[0] : request.getCookies();
            var cookies = Arrays.copyOf(existing, existing.length + 1);
            cookies[existing.length] = xsrfCookie;
            request.setCookies(cookies);
            request.addHeader("X-XSRF-TOKEN", xsrfCookie.getValue());
            return request;
        };
    }

    private String cookieHeader(List<String> setCookies, String name) {
        return setCookies.stream()
                .filter(header -> header.startsWith(name + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Set-Cookie for " + name + " in " + setCookies));
    }

    private String cookieValue(String setCookie) {
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    private String sha256Hex(String value) throws Exception {
        byte[] hashed = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }

    private void registerUser() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(EMAIL)))
                .andExpect(status().isCreated());
    }

    private String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private String registrationJson(String email) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe"
                }
                """.formatted(email, PASSWORD);
    }
}
