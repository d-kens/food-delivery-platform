package com.example.user_service.controller;

import com.example.user_service.config.SecurityConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JwksController.class)
@Import({SecurityConfig.class, JwksControllerTest.TestKeys.class})
class JwksControllerTest {

    private static final String JWKS_URL = "/.well-known/jwks.json";
    private static final String KEY_ID = "test-key";

    @TestConfiguration
    static class TestKeys {
        // Includes the private key, like the production JWKSet bean, so leaks are detectable.
        @Bean
        JWKSet jwkSet() throws JOSEException {
            return new JWKSet(new RSAKeyGenerator(2048)
                    .keyID(KEY_ID)
                    .keyUse(KeyUse.SIGNATURE)
                    .generate());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void jwks_returnsOk_withoutToken() throws Exception {
        mockMvc.perform(get(JWKS_URL))
                .andExpect(status().isOk());
    }

    @Test
    void jwks_returnsPublicKeyFields() throws Exception {
        mockMvc.perform(get(JWKS_URL))
                .andExpect(jsonPath("$.keys.length()").value(1))
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").value(KEY_ID))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").exists());
    }

    @Test
    void jwks_doesNotExposePrivateKeyParts() throws Exception {
        mockMvc.perform(get(JWKS_URL))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist())
                .andExpect(jsonPath("$.keys[0].q").doesNotExist())
                .andExpect(jsonPath("$.keys[0].dp").doesNotExist())
                .andExpect(jsonPath("$.keys[0].dq").doesNotExist())
                .andExpect(jsonPath("$.keys[0].qi").doesNotExist());
    }
}
