package com.example.user_service.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGeneratorTest {

    private final TokenGenerator tokenGenerator = new TokenGenerator();

    @Test
    void generate_returnsUrlSafeTokenOf32Bytes() {
        String token = tokenGenerator.generate();

        // 32 bytes in Base64 without padding is 43 characters
        assertThat(token).hasSize(43);
        assertThat(token).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void generate_returnsDifferentTokenEachCall() {
        assertThat(tokenGenerator.generate()).isNotEqualTo(tokenGenerator.generate());
    }

    @Test
    void hash_returnsSameSha256HexForSameInput() {
        String hash = tokenGenerator.hash("raw-token");

        assertThat(hash).isEqualTo(tokenGenerator.hash("raw-token"));
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]+");
        assertThat(hash).isNotEqualTo("raw-token");
    }

    @Test
    void hash_returnsDifferentHashForDifferentInput() {
        assertThat(tokenGenerator.hash("token-a")).isNotEqualTo(tokenGenerator.hash("token-b"));
    }
}
