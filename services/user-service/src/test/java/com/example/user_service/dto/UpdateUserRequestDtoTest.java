package com.example.user_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateUserRequestDtoTest {

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
    void emptyRequest_hasNoViolations() {
        var request = new UpdateUserRequestDto(null, null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void fullRequest_hasNoViolations() {
        var request = new UpdateUserRequestDto("Jane", "Doe", "jane.doe@gmail.com", "+254707127309", "secret123");

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void names_mustNotBeBlank_whenProvided(String name) {
        var request = new UpdateUserRequestDto(name, name, null, null, null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("firstName", "lastName");
    }

    @Test
    void names_atMaxLength_areValid() {
        var request = new UpdateUserRequestDto("a".repeat(100), "a".repeat(100), null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void names_mustNotExceedHundredCharacters() {
        var request = new UpdateUserRequestDto("a".repeat(101), "a".repeat(101), null, null, null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("firstName", "lastName");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-an-email"})
    void email_mustBeValid_whenProvided(String email) {
        var request = new UpdateUserRequestDto(null, null, email, null, null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("email");
    }

    @Test
    void email_mustNotExceed255Characters() {
        var request = new UpdateUserRequestDto(null, null, emailOfLength(256), null, null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0707127", "+254707127309", "123456789012345"})
    void phoneNumber_acceptsSevenToFifteenDigits(String phoneNumber) {
        var request = new UpdateUserRequestDto(null, null, null, phoneNumber, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123456", "1234567890123456", "12ab567", "254-707-127", "++254707127309"})
    void phoneNumber_rejectsInvalidValues(String phoneNumber) {
        var request = new UpdateUserRequestDto(null, null, null, phoneNumber, null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("phoneNumber");
    }

    private static String emailOfLength(int length) {
        String localPart = "a".repeat(64);
        int lastLabelLength = length - localPart.length() - "@".length() - 2 * (63 + ".".length());
        return localPart + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(lastLabelLength);
    }

    private Set<String> invalidFields(Set<ConstraintViolation<UpdateUserRequestDto>> violations) {
        return violations
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
