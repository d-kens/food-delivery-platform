package com.example.user_service.service;

import com.example.user_service.dto.RegistrationRequest;
import com.example.user_service.dto.RegistrationResponse;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthenticationService authenticationService;

    @Test
    void register_withPhoneNumber_savesUserAndReturnsResponse() {
        var request = createRegistrationRequest();

        var user = createUser(request);

        var expectedResponse = new RegistrationResponse(
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
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userMapper.toUserEntity(request)).thenReturn(user);
        when(userMapper.toRegistrationResponse(user)).thenReturn(expectedResponse);

        var response = authenticationService.register(request);

        assertThat(response).isSameAs(expectedResponse);
        verify(userRepository).save(user);
    }

    @Test
    void register_setsEncodedPasswordRoleAndPhoneOnSavedUser() {
        var request = createRegistrationRequest();

        var user = createUser(request);

        var encodedPassword = "$2b$12$L73P6R7iNqGvE16vU7/bK.7SshYg58v8q7mB2O5";

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
        when(userMapper.toUserEntity(request)).thenReturn(user);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);


        authenticationService.register(request);

        assertEquals(Role.USER, user.getRole());
        assertEquals(encodedPassword, user.getPasswordHash());
        assertEquals(request.phoneNumber(), user.getPhoneNumber());
        verify(passwordEncoder).encode(request.password());
    }


    @Test
    void register_withNullOrBlankPhone_savesNullPhoneAndSkipsPhoneCheck() {
        var request = createRegistrationRequestWithoutPhoneNumber();
        var user = createUser(request);

        when(userMapper.toUserEntity(request)).thenReturn(user);

        authenticationService.register(request);
        assertNull(user.getPhoneNumber());
        verify(userRepository, never()).existsByPhoneNumber(any());
    }

    @Test
    void register_whenEmailExists_throwsEmailAlreadyExistsException() {
        var request = createRegistrationRequest();

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        var exception = assertThrows(EmailAlreadyExistsException.class, () -> {
            authenticationService.register(request);
        });

        assertEquals("An account with this email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenPhoneExists_throwsPhoneNumberAlreadyExistsException() {
        var request = createRegistrationRequest();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(true);

        var exception = assertThrows(PhoneNumberAlreadyExistsException.class, () -> {
            authenticationService.register(request);
        });

        assertEquals("An account with this phone number already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    private RegistrationRequest createRegistrationRequest() {
        return new RegistrationRequest(
                "jane.doe@gmail.com",
                "T3st@12367",
                "Jane",
                "Doe",
                "0707127309"
        );
    }

    private RegistrationRequest createRegistrationRequestWithoutPhoneNumber() {
        return new RegistrationRequest(
                "jane.doe@gmail.com",
                "T3st@12367",
                "Jane",
                "Doe",
                null
        );
    }

    private User createUser(RegistrationRequest request) {
        return User
                .builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();
    }
}
