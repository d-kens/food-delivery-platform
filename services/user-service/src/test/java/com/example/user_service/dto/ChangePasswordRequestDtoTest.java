package com.example.user_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordRequestDtoTest {

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
    void validRequest_hasNoViolations() {
        var request = new ChangePasswordRequestDto("OldPassw0rd!", "NewPassw0rd!");

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void currentPassword_isRequired(String currentPassword) {
        var request = new ChangePasswordRequestDto(currentPassword, "NewPassw0rd!");

        assertThat(invalidFields(validator.validate(request))).containsOnly("currentPassword");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short"})
    void newPassword_isRequiredAndAtLeastEightCharacters(String newPassword) {
        var request = new ChangePasswordRequestDto("OldPassw0rd!", newPassword);

        assertThat(invalidFields(validator.validate(request))).containsOnly("newPassword");
    }

    @Test
    void newPassword_mustNotExceedHundredCharacters() {
        var request = new ChangePasswordRequestDto("OldPassw0rd!", "a".repeat(101));

        assertThat(invalidFields(validator.validate(request))).containsOnly("newPassword");
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 100})
    void newPassword_atLengthLimits_isValid(int length) {
        var request = new ChangePasswordRequestDto("OldPassw0rd!", "a".repeat(length));

        assertThat(validator.validate(request)).isEmpty();
    }

    private Set<String> invalidFields(Set<ConstraintViolation<ChangePasswordRequestDto>> violations) {
        return violations
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
