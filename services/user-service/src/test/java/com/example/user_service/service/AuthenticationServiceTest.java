package com.example.user_service.service;

import com.example.user_service.dto.LoginRequestDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.entity.User;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCredentialException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtService;
import com.example.user_service.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthenticationService authenticationService;

    @Test
    void register_withPhoneNumber_savesUserAndReturnsResponse() {
        var request = createRegistrationRequest();

        var user = createUser(request);

        var expectedResponse = new RegistrationResponseDto(
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

    @Test
    void login_withValidCredentialsAndActiveUser_returnsLoginResponse() {
        var request = createLoginRequest();
        var user = createActiveUser(request.email());

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token");

        var response = authenticationService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals(user.getId().toString(), response.userId());
        assertEquals(user.getFirstName(), response.firstName());
        assertEquals(user.getLastName(), response.lastName());
        assertEquals(user.getEmail(), response.email());
        assertEquals("USER", response.role());
        verify(jwtService).generateToken(user);
    }

    @Test
    void login_whenEmailNotFound_throwsInvalidCredentialException() {
        var request = createLoginRequest();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        var exception = assertThrows(InvalidCredentialException.class, () -> {
            authenticationService.login(request);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_whenPasswordDoesNotMatch_throwsInvalidCredentialException() {
        var request = createLoginRequest();
        var user = createActiveUser(request.email());

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        var exception = assertThrows(InvalidCredentialException.class, () -> {
            authenticationService.login(request);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_whenUserSuspended_throwsUserSuspendedException() {
        var request = createLoginRequest();
        var user = createActiveUser(request.email());
        user.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);

        var exception = assertThrows(UserSuspendedException.class, () -> {
            authenticationService.login(request);
        });

        assertEquals("Your account has been suspended", exception.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    private LoginRequestDto createLoginRequest() {
        return new LoginRequestDto("jane.doe@gmail.com", "T3st@12367");
    }

    private User createActiveUser(String email) {
        return User
                .builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("encoded-password")
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private RegistrationRequestDto createRegistrationRequest() {
        return new RegistrationRequestDto(
                "jane.doe@gmail.com",
                "T3st@12367",
                "Jane",
                "Doe",
                "0707127309"
        );
    }

    private RegistrationRequestDto createRegistrationRequestWithoutPhoneNumber() {
        return new RegistrationRequestDto(
                "jane.doe@gmail.com",
                "T3st@12367",
                "Jane",
                "Doe",
                null
        );
    }

    private User createUser(RegistrationRequestDto request) {
        return User
                .builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();
    }
}
