package com.example.user_service.dto.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordLengthValidatorTest {

    private final PasswordLengthValidator validator = new PasswordLengthValidator();

    @Test
    void acceptsNull_soNotBlankReportsIt() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 72})
    void acceptsAsciiPasswordsWithinLimits(int length) {
        assertThat(validator.isValid("a".repeat(length), null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 73})
    void rejectsAsciiPasswordsOutsideLimits(int length) {
        assertThat(validator.isValid("a".repeat(length), null)).isFalse();
    }

    @Test
    void countsMultiByteCharactersAsBytes() {
        assertThat(validator.isValid("é".repeat(36), null)).isTrue();
        assertThat(validator.isValid("é".repeat(37), null)).isFalse();
    }

    @Test
    void countsEmojiAsFourBytes() {
        assertThat(validator.isValid("😀".repeat(18), null)).isTrue();
        assertThat(validator.isValid("😀".repeat(18) + "a", null)).isFalse();
    }
}
