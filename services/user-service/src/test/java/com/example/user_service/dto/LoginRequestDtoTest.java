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

class LoginRequestDtoTest {

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
        var request = new LoginRequestDto("jane.doe@gmail.com", "T3st@12367");

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void email_isRequired(String email) {
        var request = new LoginRequestDto(email, "T3st@12367");

        assertThat(invalidFields(validator.validate(request))).containsOnly("email");
    }

    @Test
    void email_mustBeValid() {
        var request = new LoginRequestDto("not-an-email", "T3st@12367");

        assertThat(invalidFields(validator.validate(request))).containsOnly("email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void password_isRequired(String password) {
        var request = new LoginRequestDto("jane.doe@gmail.com", password);

        assertThat(invalidFields(validator.validate(request))).containsOnly("password");
    }

    private Set<String> invalidFields(Set<ConstraintViolation<LoginRequestDto>> violations) {
        return violations
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
