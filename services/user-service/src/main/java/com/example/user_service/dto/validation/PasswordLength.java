package com.example.user_service.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordLengthValidator.class)
public @interface PasswordLength {

    int MIN_CHARACTERS = 8;
    int MAX_BYTES = 72;

    String message() default "Password must be between 8 and 72 characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
