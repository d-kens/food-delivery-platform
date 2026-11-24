package com.example.user_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestDto(
        @Pattern(regexp = ".*\\S.*", message = "First name must not be blank")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Pattern(regexp = ".*\\S.*", message = "Last name must not be blank")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be 7 to 15 digits, optionally starting with +")
        String phoneNumber
) {
}
