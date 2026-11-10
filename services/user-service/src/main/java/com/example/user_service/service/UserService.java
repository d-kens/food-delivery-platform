package com.example.user_service.service;

import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.AuthResponseDto;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public AuthResponseDto getCurrentUser(final UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserSuspendedException("Your account has been suspended");
        }

        return userMapper.toAuthResponse(user);
    }
}
