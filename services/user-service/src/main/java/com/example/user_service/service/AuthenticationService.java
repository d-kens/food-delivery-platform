package com.example.user_service.service;

import com.example.user_service.common.Emails;
import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.AuthRequestDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.common.Role;
import com.example.user_service.entity.User;
import com.example.user_service.event.UserEvent;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCredentialException;
import com.example.user_service.exception.InvalidRefreshTokenException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.outbox.OutboxService;
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

    @Transactional
    public UserResponseDto register(final RegistrationRequestDto request) {

        String email = Emails.normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = userMapper.toUserEntity(request);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(EnumSet.of(Role.USER));

        userRepository.saveAndFlush(user);
        outboxService.saveUserEvent(UserEvent.created(user));

        return userMapper.toUserResponse(user);
    }


    public LoginResult login(final AuthRequestDto request) {
        User user = userRepository.findByEmail(Emails.normalize(request.email()))
                .orElseThrow(() -> new InvalidCredentialException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserSuspendedException("Your account has been suspended");
        }

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
            throw new UserSuspendedException("Your account has been suspended");
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
