package com.example.user_service.dto;

import com.example.user_service.common.Role;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateUserRolesRequestDtoTest {

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
        assertThat(validator.validate(new UpdateUserRolesRequestDto(Set.of(Role.USER, Role.ADMIN)))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void roles_isRequired(Set<Role> roles) {
        assertThat(validator.validate(new UpdateUserRolesRequestDto(roles)))
                .singleElement()
                .satisfies(violation -> assertThat(violation.getMessage()).isEqualTo("At least one role is required"));
    }

    @Test
    void roles_mustNotContainNull() {
        var roles = new HashSet<Role>();
        roles.add(Role.USER);
        roles.add(null);

        assertThat(validator.validate(new UpdateUserRolesRequestDto(roles)))
                .singleElement()
                .satisfies(violation -> assertThat(violation.getMessage()).isEqualTo("Role must not be null"));
    }
}
