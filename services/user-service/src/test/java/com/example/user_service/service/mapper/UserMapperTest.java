package com.example.user_service.service.mapper;

import com.example.user_service.dto.AuthResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.common.Role;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.entity.User;
import com.example.user_service.common.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String EMAIL = "jane.doe@gmail.com";
    private static final String PHONE_NUMBER = "0707127309";
    private static final String FIRST_NAME = "Jane";
    private static final String LAST_NAME = "Doe";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void toUserEntity_mapsProfileFields() {
        var user = userMapper.toUserEntity(registrationRequest(PHONE_NUMBER));

        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(user.getLastName()).isEqualTo(LAST_NAME);
    }

    @Test
    void toUserEntity_leavesFieldsSetByServiceOrDatabaseUnset() {
        var user = userMapper.toUserEntity(registrationRequest(PHONE_NUMBER));

        assertThat(user.getId()).isNull();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getRole()).isNull();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void toUserEntity_defaultsStatusToActive() {
        var user = userMapper.toUserEntity(registrationRequest(null));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void toRegistrationResponse_mapsAllFieldsAndEnumNames() {
        var response = userMapper.toRegistrationResponse(user());
        assertThat(response).isEqualTo(new RegistrationResponseDto(
                ID, EMAIL, PHONE_NUMBER, FIRST_NAME, LAST_NAME, "USER", "ACTIVE", CREATED_AT));
    }

    @Test
    void toAuthResponse_mapsUserFieldsAndAccessToken() {
        var response = userMapper.toAuthResponse(user(), "access-token");
        assertThat(response).isEqualTo(new AuthResponseDto(
                "access-token", ID.toString(), FIRST_NAME, LAST_NAME, EMAIL, "USER"));
    }

    private RegistrationRequestDto registrationRequest(String phoneNumber) {
        return new RegistrationRequestDto(EMAIL, "T3st@12367", FIRST_NAME, LAST_NAME, phoneNumber);
    }

    private User user() {
        return User
                .builder()
                .id(ID)
                .email(EMAIL)
                .phoneNumber(PHONE_NUMBER)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .passwordHash("hashed")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(CREATED_AT)
                .build();
    }
}
