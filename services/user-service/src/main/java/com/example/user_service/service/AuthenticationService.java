package com.example.user_service.service;

import com.example.user_service.common.AuthProvider;
import com.example.user_service.common.Emails;
import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.AuthRequestDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.common.Role;
import com.example.user_service.entity.User;
import com.example.user_service.entity.UserExternalIdentity;
import com.example.user_service.event.UserEvent;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCredentialException;
import com.example.user_service.exception.InvalidRefreshTokenException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.outbox.OutboxService;
import com.example.user_service.repository.UserExternalIdentityRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtService;
import com.example.user_service.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final OutboxService outboxService;
    private final UserExternalIdentityRepository userExternalIdentityRepository;

    @Transactional
    public UserResponseDto register(final RegistrationRequestDto request) {

        String email = Emails.normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = userMapper.toUserEntity(request);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(EnumSet.of(Role.USER));

        userRepository.save(user);
        outboxService.saveUserEvent(UserEvent.created(user));

        return userMapper.toUserResponse(user);
    }


    public LoginResult login(final AuthRequestDto request) {
        User user = userRepository.findByEmail(Emails.normalize(request.email()))
                .orElseThrow(InvalidCredentialException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserSuspendedException();
        }

        return issueTokens(user);
    }

    @Transactional
    public LoginResult loginWithGoogle(final GoogleAccount googleAccount) {
        if (!googleAccount.emailVerified()) {
            throw new InvalidCredentialException();
        }

        User user = userExternalIdentityRepository.findByProviderAndSubject(AuthProvider.GOOGLE, googleAccount.subject())
                .map(UserExternalIdentity::getUser)
                .orElseGet(() -> linkOrCreateGoogleUser(googleAccount));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserSuspendedException();
        }

        return issueTokens(user);
    }

    private User linkOrCreateGoogleUser(final GoogleAccount googleAccount) {
        String email = Emails.normalize(googleAccount.email());

        return userRepository.findByEmail(email)
                .map(user -> linkGoogleAccount(user, googleAccount.subject()))
                .orElseGet(() -> createGoogleUser(googleAccount, email));
    }

    private User linkGoogleAccount(final User user, final String googleSubject) {
        if (userExternalIdentityRepository.existsByUserIdAndProvider(user.getId(), AuthProvider.GOOGLE)) {
            throw new InvalidCredentialException();
        }

        user.setPasswordHash(null);
        refreshTokenService.revokeAllFor(user.getId());
        saveGoogleIdentity(user, googleSubject);
        return user;
    }

    private User createGoogleUser(final GoogleAccount googleAccount, final String email) {
        User user = User.builder()
                .email(email)
                .firstName(googleAccount.firstName() != null ? googleAccount.firstName() : email.substring(0, email.indexOf('@')))
                .lastName(googleAccount.lastName() != null ? googleAccount.lastName() : "")
                .roles(EnumSet.of(Role.USER))
                .build();

        userRepository.save(user);
        saveGoogleIdentity(user, googleAccount.subject());
        outboxService.saveUserEvent(UserEvent.created(user));
        return user;
    }

    private void saveGoogleIdentity(final User user, final String googleSubject) {
        userExternalIdentityRepository.save(UserExternalIdentity.builder()
                .user(user)
                .provider(AuthProvider.GOOGLE)
                .subject(googleSubject)
                .build());
    }

    private LoginResult issueTokens(final User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createFor(user);

        return new LoginResult(userMapper.toUserResponse(user), accessToken, refreshToken);
    }

    public RefreshResult refreshToken(final String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        var storedToken = refreshTokenService.validate(refreshToken);
        var user = storedToken.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserSuspendedException();
        }

        String accessToken = jwtService.generateToken(user);
        return new RefreshResult(userMapper.toUserResponse(user), accessToken);
    }

    public void logout(final String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }
}
