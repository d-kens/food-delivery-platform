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

class RegistrationRequestTest {

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
        var request = new RegistrationRequest("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", "0707127309");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void phoneNumber_isOptional() {
        var request = new RegistrationRequest("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-an-email", "jane.doe@"})
    void email_isRequiredAndMustBeValid(String email) {
        var request = new RegistrationRequest(email, "T3st@12367", "Jane", "Doe", null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short"})
    void password_isRequiredAndAtLeastEightCharacters(String password) {
        var request = new RegistrationRequest("jane.doe@gmail.com", password, "Jane", "Doe", null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("password");
    }

    @Test
    void password_mustNotExceedHundredCharacters() {
        var request = new RegistrationRequest("jane.doe@gmail.com", "a".repeat(101), "Jane", "Doe", null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("password");
    }

    @Test
    void names_areRequired() {
        var request = new RegistrationRequest("jane.doe@gmail.com", "T3st@12367", " ", null, null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("firstName", "lastName");
    }

    @Test
    void phoneNumber_mustNotExceedTwentyCharacters() {
        var request = new RegistrationRequest("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", "1".repeat(21));

        assertThat(invalidFields(validator.validate(request))).containsOnly("phoneNumber");
    }

    private Set<String> invalidFields(Set<ConstraintViolation<RegistrationRequest>> violations) {
        return violations
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
