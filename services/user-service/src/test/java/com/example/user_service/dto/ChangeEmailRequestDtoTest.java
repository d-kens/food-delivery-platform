package com.example.user_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeEmailRequestDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validEmail_hasNoViolations() {
        assertThat(validator.validate(new ChangeEmailRequestDto("janet@gmail.com"))).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "not-an-email"})
    void newEmail_isRequiredAndMustBeValid(String email) {
        assertThat(invalidFields(validator.validate(new ChangeEmailRequestDto(email)))).containsOnly("newEmail");
    }

    @Test
    void newEmail_mustNotExceed255Characters() {
        assertThat(invalidFields(validator.validate(new ChangeEmailRequestDto(emailOfLength(256))))).containsOnly("newEmail");
    }

    private static String emailOfLength(int length) {
        String localPart = "a".repeat(64);
        int lastLabelLength = length - localPart.length() - "@".length() - 2 * (63 + ".".length());
        return localPart + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(lastLabelLength);
    }

    private Set<String> invalidFields(Set<ConstraintViolation<ChangeEmailRequestDto>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
