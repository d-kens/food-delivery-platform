package com.example.user_service.service;

import com.example.user_service.dto.RegistrationRequest;
import com.example.user_service.dto.RegistrationResponse;
import com.example.user_service.entity.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthenticationService authenticationService;

    @Test
    void register_withPhoneNumber_savesUserAndReturnsResponse() {
        RegistrationRequest request = new RegistrationRequest(
                "jane.doe@example.com",
                "T3st@12367",
                "Jane",
                "Doe",
                "0700000000"
        );

        User user = User
                .builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();

        RegistrationResponse expectedResponse = new RegistrationResponse(
                UUID.randomUUID(),
                request.email(),
                request.phoneNumber(),
                request.firstName(),
                request.lastName(),
                "USER",
                "ACTIVE",
                Instant.now()
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
        when(userMapper.toUserEntity(request)).thenReturn(user);
        when(userMapper.toRegistrationResponse(user)).thenReturn(expectedResponse);

        RegistrationResponse response = authenticationService.register(request);

        assertThat(response).isSameAs(expectedResponse);
        verify(userRepository).save(user);
    }

    @Test
    void register_setsEncodedPasswordRoleAndPhoneOnSavedUser() {

    }

    @Test
    void register_encodesRawPassword() {

    }

    @Test
    void register_withNullOrBlankPhone_savesNullPhoneAndSkipsPhoneCheck() {

    }

    @Test
    void register_whenEmailExists_throwsEmailAlreadyExistsException() {

    }

    @Test
    void register_whenPhoneExists_throwsPhoneNumberAlreadyExistsException() {

    }
}
