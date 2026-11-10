package com.example.user_service.service;

import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.entity.User;
import com.example.user_service.exception.AccountNotFoundException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserService userService;

    @Test
    void getCurrentUser_returnsMappedUser() {
        var user = user(UserStatus.ACTIVE);
        var expected = new UserResponseDto(USER_ID.toString(), "Jane", "Doe", "jane.doe@gmail.com", List.of("USER"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expected);

        assertThat(userService.getCurrentUser(USER_ID)).isSameAs(expected);
    }

    @Test
    void getCurrentUser_throwsAccountNotFound_whenUserNoLongerExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getCurrentUser_throwsSuspended_whenUserIsNotActive() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
                .isInstanceOf(UserSuspendedException.class);
    }

    private User user(UserStatus status) {
        return User.builder()
                .id(USER_ID)
                .email("jane.doe@gmail.com")
                .firstName("Jane")
                .lastName("Doe")
                .status(status)
                .build();
    }
}
