package com.example.user_service.dto;

import com.example.user_service.common.UserStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateUserStatusRequestDtoTest {

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

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void everyStatus_isValid(UserStatus status) {
        assertThat(validator.validate(new UpdateUserStatusRequestDto(status))).isEmpty();
    }

    @Test
    void status_isRequired() {
        assertThat(validator.validate(new UpdateUserStatusRequestDto(null)))
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("status");
                    assertThat(violation.getMessage()).isEqualTo("Status is required");
                });
    }
}
