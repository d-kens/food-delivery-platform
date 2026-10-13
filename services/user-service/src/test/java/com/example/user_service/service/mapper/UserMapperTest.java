package com.example.user_service.service.mapper;

import com.example.user_service.dto.RegistrationRequest;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.entity.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void toUserEntity_mapsProfileFields() {
        var request = new RegistrationRequest("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", "0707127309");

        var user = userMapper.toUserEntity(request);

        assertThat(user.getEmail()).isEqualTo("jane.doe@gmail.com");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    void toUserEntity_leavesFieldsSetByServiceOrDatabaseUnset() {
        var request = new RegistrationRequest("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", "0707127309");

        var user = userMapper.toUserEntity(request);

        assertThat(user.getId()).isNull();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getRole()).isNull();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void toUserEntity_defaultsStatusToActive() {
        var request = new RegistrationRequest("jane.doe@gmail.com", "T3st@12367", "Jane", "Doe", null);

        var user = userMapper.toUserEntity(request);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void toRegistrationResponse_mapsAllFieldsAndEnumNames() {
        var id = UUID.randomUUID();
        var createdAt = Instant.parse("2026-01-01T10:00:00Z");
        var user = User
                .builder()
                .id(id)
                .email("jane.doe@gmail.com")
                .phoneNumber("0707127309")
                .firstName("Jane")
                .lastName("Doe")
                .passwordHash("hashed")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(createdAt)
                .build();

        var response = userMapper.toRegistrationResponse(user);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("jane.doe@gmail.com");
        assertThat(response.phoneNumber()).isEqualTo("0707127309");
        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
