package com.example.user_service.service;


import com.example.user_service.dto.RegistrationRequest;
import com.example.user_service.dto.RegistrationResponse;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationResponse register(final RegistrationRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("User with that email already exists");
        }

        String phoneNumber = (request.phoneNumber() == null || request.phoneNumber().isBlank())
                ? null
                : request.phoneNumber();

        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyExistsException("User with that phone number already exists");
        }

        User user = User
                .builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phoneNumber(phoneNumber)
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return new RegistrationResponse(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().toString(),
                user.getStatus().toString(),
                user.getCreatedAt()
        );
    }
}
