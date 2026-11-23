package com.example.user_service.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.length() >= PasswordLength.MIN_CHARACTERS
                && value.getBytes(StandardCharsets.UTF_8).length <= PasswordLength.MAX_BYTES;
    }
}
