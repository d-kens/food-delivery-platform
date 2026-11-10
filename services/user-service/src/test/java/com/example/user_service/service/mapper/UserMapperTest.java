package com.example.user_service.service.mapper;

import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.common.Role;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.entity.User;
import com.example.user_service.common.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String EMAIL = "jane.doe@gmail.com";
    private static final String FIRST_NAME = "Jane";
    private static final String LAST_NAME = "Doe";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void toUserEntity_mapsProfileFields() {
        var user = userMapper.toUserEntity(registrationRequest());

        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(user.getLastName()).isEqualTo(LAST_NAME);
    }

    @Test
    void toUserEntity_leavesFieldsSetByServiceOrDatabaseUnset() {
        var user = userMapper.toUserEntity(registrationRequest());

        assertThat(user.getId()).isNull();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getRoles()).isEmpty();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void toUserEntity_defaultsStatusToActive() {
        var user = userMapper.toUserEntity(registrationRequest());
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void toRegistrationResponse_mapsAllFieldsAndEnumNames() {
        var response = userMapper.toRegistrationResponse(user());
        assertThat(response).isEqualTo(new RegistrationResponseDto(
                ID, EMAIL, FIRST_NAME, LAST_NAME, List.of("RIDER", "USER"), "ACTIVE", CREATED_AT));
    }

    @Test
    void toUserResponse_mapsUserFields() {
        var response = userMapper.toUserResponse(user());
        assertThat(response).isEqualTo(new UserResponseDto(
                ID.toString(), FIRST_NAME, LAST_NAME, EMAIL, List.of("RIDER", "USER")));
    }

    private RegistrationRequestDto registrationRequest() {
        return new RegistrationRequestDto(EMAIL, "T3st@12367", FIRST_NAME, LAST_NAME);
    }

    private User user() {
        return User
                .builder()
                .id(ID)
                .email(EMAIL)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .passwordHash("hashed")
                .roles(Set.of(Role.USER, Role.RIDER))
                .status(UserStatus.ACTIVE)
                .createdAt(CREATED_AT)
                .build();
    }
}
