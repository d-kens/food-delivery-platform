package com.example.user_service.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator refreshTokenGenerator = new RefreshTokenGenerator();

    @Test
    void generate_returnsUrlSafeTokenOf32Bytes() {
        String token = refreshTokenGenerator.generate();

        // 32 bytes in Base64 without padding is 43 characters
        assertThat(token).hasSize(43);
        assertThat(token).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void generate_returnsDifferentTokenEachCall() {
        assertThat(refreshTokenGenerator.generate()).isNotEqualTo(refreshTokenGenerator.generate());
    }

    @Test
    void hash_returnsSameSha256HexForSameInput() {
        String hash = refreshTokenGenerator.hash("raw-token");

        assertThat(hash).isEqualTo(refreshTokenGenerator.hash("raw-token"));
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]+");
        assertThat(hash).isNotEqualTo("raw-token");
    }

    @Test
    void hash_returnsDifferentHashForDifferentInput() {
        assertThat(refreshTokenGenerator.hash("token-a")).isNotEqualTo(refreshTokenGenerator.hash("token-b"));
    }
}
