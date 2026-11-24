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
        var request = new UpdateUserRequestDto(null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void fullRequest_hasNoViolations() {
        var request = new UpdateUserRequestDto("Jane", "Doe", "+254707127309");

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void names_mustNotBeBlank_whenProvided(String name) {
        var request = new UpdateUserRequestDto(name, name, null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("firstName", "lastName");
    }

    @Test
    void names_atMaxLength_areValid() {
        var request = new UpdateUserRequestDto("a".repeat(100), "a".repeat(100), null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void names_mustNotExceedHundredCharacters() {
        var request = new UpdateUserRequestDto("a".repeat(101), "a".repeat(101), null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("firstName", "lastName");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0707127", "+254707127309", "123456789012345"})
    void phoneNumber_acceptsSevenToFifteenDigits(String phoneNumber) {
        var request = new UpdateUserRequestDto(null, null, phoneNumber);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123456", "1234567890123456", "12ab567", "254-707-127", "++254707127309"})
    void phoneNumber_rejectsInvalidValues(String phoneNumber) {
        var request = new UpdateUserRequestDto(null, null, phoneNumber);

        assertThat(invalidFields(validator.validate(request))).containsOnly("phoneNumber");
    }

    private Set<String> invalidFields(Set<ConstraintViolation<UpdateUserRequestDto>> violations) {
        return violations
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
