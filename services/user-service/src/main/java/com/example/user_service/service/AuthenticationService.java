package com.example.user_service.service;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegistrationRequest;
import com.example.user_service.dto.RegistrationResponse;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationResponse register(final RegistrationRequest request) {

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

    public LoginResult login(final LoginRequest request) {
        return null;
    }
}
