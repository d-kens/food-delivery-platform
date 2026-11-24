package com.example.user_service.service;

import com.example.user_service.event.UserEvent;
import com.example.user_service.outbox.OutboxService;
import org.mockito.ArgumentCaptor;
import com.example.user_service.dto.AuthRequestDto;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.entity.RefreshToken;
import com.example.user_service.entity.User;
import com.example.user_service.exception.*;
import com.example.user_service.repository.UserIdentityRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.common.AuthProvider;
import com.example.user_service.entity.UserIdentity;
import com.example.user_service.security.JwtService;
import com.example.user_service.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private OutboxService outboxService;
    @Mock private UserIdentityRepository userIdentityRepository;

    @InjectMocks private AuthenticationService authenticationService;

    @Test
    void register_savesUserAndReturnsResponse() {
        var request = createRegistrationRequest();

        var user = createUser(request);

        var expectedResponse = new UserResponseDto(
                UUID.randomUUID().toString(),
                request.firstName(),
                request.lastName(),
                request.email(),
                List.of("USER"),
                null,
                UserStatus.ACTIVE
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userMapper.toUserEntity(request)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(expectedResponse);

        var response = authenticationService.register(request);

        assertThat(response).isSameAs(expectedResponse);
        verify(userRepository).save(user);
    }

    @Test
    void register_savesUserCreatedEvent() {
        var request = createRegistrationRequest();
        var user = createUser(request);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toUserEntity(request)).thenReturn(user);

        authenticationService.register(request);

        var captor = ArgumentCaptor.forClass(UserEvent.class);
        verify(outboxService).saveUserEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(UserEvent.USER_CREATED);
        assertThat(event.firstName()).isEqualTo(request.firstName());
        assertThat(event.lastName()).isEqualTo(request.lastName());
        assertThat(event.email()).isEqualTo(request.email());
    }

    @Test
    void register_normalizesEmailBeforeCheckingAndSaving() {
        var request = new RegistrationRequestDto(" Jane.Doe@Gmail.com ", "T3st@12367", "Jane", "Doe");
        var user = createUser(request);
        when(userRepository.existsByEmail("jane.doe@gmail.com")).thenReturn(false);
        when(userMapper.toUserEntity(request)).thenReturn(user);

        authenticationService.register(request);

        assertThat(user.getEmail()).isEqualTo("jane.doe@gmail.com");
        verify(userRepository).existsByEmail("jane.doe@gmail.com");
    }

    @Test
    void register_setsEncodedPasswordAndRoleOnSavedUser() {
        var request = createRegistrationRequest();

        var user = createUser(request);

        var encodedPassword = "$2b$12$L73P6R7iNqGvE16vU7/bK.7SshYg58v8q7mB2O5";

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toUserEntity(request)).thenReturn(user);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);

        authenticationService.register(request);

        assertEquals(Set.of(Role.USER), user.getRoles());
        assertEquals(encodedPassword, user.getPasswordHash());
        verify(passwordEncoder).encode(request.password());
    }

    @Test
    void register_whenEmailExists_throwsAndSavesNothing() {
        var request = createRegistrationRequest();

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        var exception = assertThrows(EmailAlreadyExistsException.class, () -> {
            authenticationService.register(request);
        });

        assertEquals("An account with this email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void login_withValidCredentialsAndActiveUser_returnsLoginResult() {
        var request = createLoginRequest();
        var user = createActiveUser(request.email());

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createFor(user)).thenReturn("refresh-token");

        var expectedResponse = new UserResponseDto(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                List.of("USER")
        , null, UserStatus.ACTIVE);
        when(userMapper.toUserResponse(user)).thenReturn(expectedResponse);

        var loginResult = authenticationService.login(request);

        assertThat(loginResult.user()).isSameAs(expectedResponse);
        assertThat(loginResult.accessToken()).isEqualTo("access-token");
        assertThat(loginResult.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_looksUpUserByNormalizedEmail() {
        var request = new AuthRequestDto(" Jane.Doe@Gmail.com ", "T3st@12367");
        var user = createActiveUser("jane.doe@gmail.com");
        when(userRepository.findByEmail("jane.doe@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);

        authenticationService.login(request);

        verify(userRepository).findByEmail("jane.doe@gmail.com");
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
        verify(refreshTokenService, never()).createFor(any());
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
        verify(refreshTokenService, never()).createFor(any());
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
        verify(refreshTokenService, never()).createFor(any());
    }

    @Test
    void refreshToken_whenTokenIsValid_returnsAuthResponse() {
        var user = createActiveUser("j.doe@gmail.com");
        var storedToken = RefreshToken.builder().user(user).build();

        var expectedResponse = new UserResponseDto(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                List.of("USER")
        , null, UserStatus.ACTIVE);

        when(refreshTokenService.validate("raw-token")).thenReturn(storedToken);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(userMapper.toUserResponse(user)).thenReturn(expectedResponse);

        var result = authenticationService.refreshToken("raw-token");

        assertThat(result.user()).isSameAs(expectedResponse);
        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(refreshTokenService, never()).createFor(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void refreshToken_whenTokenIsNullOrBlank_throwsInvalidRefreshTokenException(String token) {
        assertThatThrownBy(() -> authenticationService.refreshToken(token))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
        verify(refreshTokenService, never()).validate(any());
    }

    @Test
    void refreshToken_whenTokenIsInvalid_throwsInvalidRefreshTokenException() {
        when(refreshTokenService.validate("raw-token")).thenThrow(new InvalidRefreshTokenException());

        assertThatThrownBy(() -> authenticationService.refreshToken("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void refreshToken_whenUserIsNotActive_throwsUserSuspendedException() {
        var user = createActiveUser("j.doe@gmail.com");
        user.setStatus(UserStatus.SUSPENDED);
        var storedToken = RefreshToken.builder().user(user).build();

        when(refreshTokenService.validate("raw-token")).thenReturn(storedToken);

        assertThatThrownBy(() -> authenticationService.refreshToken("raw-token"))
                .isInstanceOf(UserSuspendedException.class)
                .hasMessage("Your account has been suspended");
        verify(jwtService, never()).generateToken(any());
    }

    private AuthRequestDto createLoginRequest() {
        return new AuthRequestDto("jane.doe@gmail.com", "T3st@12367");
    }

    private User createActiveUser(String email) {
        return User
                .builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("encoded-password")
                .firstName("Jane")
                .lastName("Doe")
                .roles(Set.of(Role.USER))
                .status(UserStatus.ACTIVE)
                .build();
    }

    private RegistrationRequestDto createRegistrationRequest() {
        return new RegistrationRequestDto(
                "jane.doe@gmail.com",
                "T3st@12367",
                "Jane",
                "Doe"
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

    @Test
    void logout_revokesRefreshToken_whenPresent() {
        authenticationService.logout("raw-token");

        verify(refreshTokenService).revoke("raw-token");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void logout_doesNothing_whenRefreshTokenIsMissing(String refreshToken) {
        authenticationService.logout(refreshToken);

        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    void loginWithGoogle_whenGoogleAccountAlreadyLinked_issuesTokens() {
        var user = createActiveUser("jane.doe@gmail.com");
        when(userIdentityRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.of(createGoogleIdentity(user)));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createFor(user)).thenReturn("refresh-token");

        var result = authenticationService.loginWithGoogle(createGoogleAccount(true));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository, never()).findByEmail(any());
        verify(userIdentityRepository, never()).save(any());
    }

    @Test
    void loginWithGoogle_whenEmailMatchesPasswordAccount_linksItAndRemovesPassword() {
        var user = createActiveUser("jane.doe@gmail.com");
        when(userIdentityRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane.doe@gmail.com")).thenReturn(Optional.of(user));
        when(userIdentityRepository.existsByUserIdAndProvider(user.getId(), AuthProvider.GOOGLE)).thenReturn(false);

        authenticationService.loginWithGoogle(createGoogleAccount(true));

        assertThat(user.getPasswordHash()).isNull();
        assertSavedGoogleIdentity(user);
        verify(refreshTokenService).revokeAllFor(user.getId());
        verify(refreshTokenService).createFor(user);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithGoogle_whenEmailBelongsToAnotherGoogleAccount_throwsInvalidCredentialException() {
        var user = createActiveUser("jane.doe@gmail.com");
        when(userIdentityRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane.doe@gmail.com")).thenReturn(Optional.of(user));
        when(userIdentityRepository.existsByUserIdAndProvider(user.getId(), AuthProvider.GOOGLE)).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.loginWithGoogle(createGoogleAccount(true)))
                .isInstanceOf(InvalidCredentialException.class);

        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        verify(userIdentityRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllFor(any());
    }

    @Test
    void loginWithGoogle_whenNoUserExists_createsGoogleUserWithoutPassword() {
        when(userIdentityRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane.doe@gmail.com")).thenReturn(Optional.empty());

        authenticationService.loginWithGoogle(createGoogleAccount(true));

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        var saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("jane.doe@gmail.com");
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getRoles()).containsExactly(Role.USER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertSavedGoogleIdentity(saved);
        verify(outboxService).saveUserEvent(any(UserEvent.class));
        verify(refreshTokenService).createFor(saved);
    }

    @Test
    void loginWithGoogle_whenGoogleHasNoNames_fallsBackToEmailAndEmptyLastName() {
        when(userIdentityRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane.doe@gmail.com")).thenReturn(Optional.empty());

        authenticationService.loginWithGoogle(new GoogleAccount("google-sub", "Jane.Doe@Gmail.com", true, null, null));

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("jane.doe");
        assertThat(userCaptor.getValue().getLastName()).isEmpty();
    }

    @Test
    void loginWithGoogle_whenEmailNotVerified_throwsInvalidCredentialException() {
        assertThatThrownBy(() -> authenticationService.loginWithGoogle(createGoogleAccount(false)))
                .isInstanceOf(InvalidCredentialException.class);

        verifyNoInteractions(userRepository, userIdentityRepository, refreshTokenService);
    }

    @Test
    void loginWithGoogle_whenUserSuspended_throwsUserSuspendedException() {
        var user = createActiveUser("jane.doe@gmail.com");
        user.setStatus(UserStatus.SUSPENDED);
        when(userIdentityRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.of(createGoogleIdentity(user)));

        assertThatThrownBy(() -> authenticationService.loginWithGoogle(createGoogleAccount(true)))
                .isInstanceOf(UserSuspendedException.class);

        verify(refreshTokenService, never()).createFor(any());
    }

    private void assertSavedGoogleIdentity(User user) {
        var identityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getUser()).isSameAs(user);
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(identityCaptor.getValue().getSubject()).isEqualTo("google-sub");
    }

    private UserIdentity createGoogleIdentity(User user) {
        return UserIdentity.builder().user(user).provider(AuthProvider.GOOGLE).subject("google-sub").build();
    }

    private GoogleAccount createGoogleAccount(boolean emailVerified) {
        return new GoogleAccount("google-sub", "Jane.Doe@Gmail.com", emailVerified, "Jane", "Doe");
    }
}
