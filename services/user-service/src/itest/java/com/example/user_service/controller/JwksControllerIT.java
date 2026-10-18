package com.example.user_service.controller;

import com.example.user_service.AbstractIntegrationTest;
import com.example.user_service.common.Role;
import com.example.user_service.entity.User;
import com.example.user_service.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwksControllerIT extends AbstractIntegrationTest {

    private static final String JWKS_URL = "/.well-known/jwks.json";
    private static final String LOGIN_URL = "/auth/login";
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
    void loginToken_canBeVerifiedWithKeyPublishedAtJwks() throws Exception {
        var user = userRepository.save(User.builder()
                .email(EMAIL)
                .firstName("Jane")
                .lastName("Doe")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(Role.USER)
                .build());

        String accessToken = login();

        var jwksBody = mockMvc.perform(get(JWKS_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify the way other services will: pick the key by the token's kid, using only the JWKS response.
        String keyId = SignedJWT.parse(accessToken).getHeader().getKeyID();
        var publishedKey = JWKSet.parse(jwksBody).getKeyByKeyId(keyId);
        assertThat(publishedKey).as("JWKS key for kid %s", keyId).isNotNull();

        var decoder = NimbusJwtDecoder.withPublicKey(publishedKey.toRSAKey().toRSAPublicKey()).build();

        assertThat(decoder.decode(accessToken).getSubject()).isEqualTo(user.getId().toString());
    }

    private String login() throws Exception {
        var body = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
