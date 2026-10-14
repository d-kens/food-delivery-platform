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

class RegistrationRequestDtoTest {

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
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", "0707127309");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void phoneNumber_isOptional() {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-an-email"})
    void email_isRequiredAndMustBeValid(String email) {
        var request = new RegistrationRequestDto(email, "T3st@12367", "Jane", "Doe", null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short"})
    void password_isRequiredAndAtLeastEightCharacters(String password) {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", password, "Jane", "Doe", null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("password");
    }

    @Test
    void password_mustNotExceedHundredCharacters() {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "a".repeat(101), "Jane", "Doe", null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("password");
    }

    @Test
    void names_areRequired() {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "T3st@12367", " ", null, null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("firstName", "lastName");
    }

    @Test
    void phoneNumber_mustNotExceedTwentyCharacters() {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", "1".repeat(21));

        assertThat(invalidFields(validator.validate(request))).containsOnly("phoneNumber");
    }

    @Test
    void email_atMaxLength_isValid() {
        var request = new RegistrationRequestDto(emailOfLength(255), "T3st@12367", "Jane", "Doe", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void email_mustNotExceed255Characters() {
        var request = new RegistrationRequestDto(emailOfLength(256), "T3st@12367", "Jane", "Doe", null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("email");
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 100})
    void password_atLengthLimits_isValid(int length) {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "a".repeat(length), "Jane", "Doe", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void names_atMaxLength_areValid() {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "T3st@12367", "a".repeat(100), "a".repeat(100), null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void names_mustNotExceedHundredCharacters() {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "T3st@12367", "a".repeat(101), "a".repeat(101), null);

        assertThat(invalidFields(validator.validate(request))).containsOnly("firstName", "lastName");
    }

    @Test
    void phoneNumber_atMaxLength_isValid() {
        var request = new RegistrationRequestDto("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", "1".repeat(20));

        assertThat(validator.validate(request)).isEmpty();
    }

    // Builds a well-formed email of the given length, so only @Size can fail.
    // @Email caps the local part at 64 chars and each domain label at 63, so the length goes into domain labels.
    private static String emailOfLength(int length) {
        String localPart = "a".repeat(64);
        int lastLabelLength = length - localPart.length() - "@".length() - 2 * (63 + ".".length());
        return localPart + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(lastLabelLength);
    }

    private Set<String> invalidFields(Set<ConstraintViolation<RegistrationRequestDto>> violations) {
        return violations
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
