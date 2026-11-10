package com.example.user_service.service;

import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.exception.AccountNotFoundException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(final UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(AccountNotFoundException::new);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserSuspendedException("Your account has been suspended");
        }

        return userMapper.toUserResponse(user);
    }
}
