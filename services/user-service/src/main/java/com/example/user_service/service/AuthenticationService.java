package com.example.user_service.service;

import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.LoginRequestDto;
import com.example.user_service.dto.LoginResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.common.Role;
import com.example.user_service.entity.User;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCredentialException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtService;
import com.example.user_service.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public RegistrationResponseDto register(final RegistrationRequestDto request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        String phoneNumber = (request.phoneNumber() == null || request.phoneNumber().isBlank())
                ? null
                : request.phoneNumber();

        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyExistsException("An account with this phone number already exists");
        }

        User user = userMapper.toUserEntity(request);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        userRepository.save(user);

        return userMapper.toRegistrationResponse(user);
    }


    public LoginResponseDto login(final LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserSuspendedException("Your account has been suspended");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createFor(user);

        System.out.println("----------------------------------------------------------");
        System.out.println("Refresh Token: " + refreshToken);
        System.out.println("----------------------------------------------------------");

        return new LoginResponseDto(
                accessToken,
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().toString()
        );
    }
}
