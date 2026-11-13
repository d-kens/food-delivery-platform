package com.example.user_service.service;

import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.ChangePasswordRequestDto;
import com.example.user_service.dto.UpdateUserRequestDto;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.entity.User;
import com.example.user_service.exception.AccountNotFoundException;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCurrentPasswordException;
import com.example.user_service.exception.OwnStatusChangeException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;

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

    @Test
    @SuppressWarnings("unchecked")
    void getUsers_returnsMappedPageSortedNewestFirst() {
        var user = user(UserStatus.ACTIVE);
        var dto = new UserResponseDto(USER_ID.toString(), "Jane", "Doe", "jane.doe@gmail.com", List.of("USER"));
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(any(Specification.class), pageable.capture()))
                .thenAnswer(inv -> new PageImpl<>(List.of(user), inv.getArgument(1), 41));
        when(userMapper.toUserResponse(user)).thenReturn(dto);

        var result = userService.getUsers(UserStatus.ACTIVE, Role.USER, "jane", 2, 20);

        assertThat(result.content()).containsExactly(dto);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        verify(userMapper).toUserResponse(user);
    }

    @Test
    void getUserById_returnsMappedUser_evenWhenSuspended() {
        var user = user(UserStatus.SUSPENDED);
        var expected = new UserResponseDto(USER_ID.toString(), "Jane", "Doe", "jane.doe@gmail.com", List.of("USER"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expected);

        assertThat(userService.getUserById(USER_ID)).isSameAs(expected);
    }

    @Test
    void getUserById_throwsUserNotFound_whenMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUserStatus_suspendsUserAndRevokesRefreshTokens() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.updateUserStatus(ADMIN_ID, USER_ID, UserStatus.SUSPENDED);

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(refreshTokenService).revokeAllFor(USER_ID);
    }

    @Test
    void updateUserStatus_reactivatesUserWithoutRevokingTokens() {
        var user = user(UserStatus.SUSPENDED);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.updateUserStatus(ADMIN_ID, USER_ID, UserStatus.ACTIVE);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(refreshTokenService, never()).revokeAllFor(any());
    }

    @Test
    void updateUserStatus_throwsUserNotFound_whenMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserStatus(ADMIN_ID, USER_ID, UserStatus.SUSPENDED))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUserStatus_throws_whenAdminChangesOwnStatus() {
        assertThatThrownBy(() -> userService.updateUserStatus(ADMIN_ID, ADMIN_ID, UserStatus.SUSPENDED))
                .isInstanceOf(OwnStatusChangeException.class);

        verify(userRepository, never()).findById(any());
        verify(refreshTokenService, never()).revokeAllFor(any());
    }

    @Test
    void updateCurrentUser_updatesOnlyProvidedFields() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber("0707127309")).thenReturn(false);

        userService.updateCurrentUser(USER_ID, new UpdateUserRequestDto(" Janet ", null, null, "0707127309", null));

        assertThat(user.getFirstName()).isEqualTo("Janet");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getEmail()).isEqualTo("jane.doe@gmail.com");
        assertThat(user.getPhoneNumber()).isEqualTo("0707127309");
        verify(userMapper).toUserResponse(user);
    }

    @Test
    void updateCurrentUser_changesEmail_whenCurrentPasswordMatches() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(userRepository.existsByEmail("janet@gmail.com")).thenReturn(false);

        userService.updateCurrentUser(USER_ID, new UpdateUserRequestDto(null, null, "janet@gmail.com", null, "secret123"));

        assertThat(user.getEmail()).isEqualTo("janet@gmail.com");
    }

    @Test
    void updateCurrentUser_doesNotRequirePassword_whenEmailIsUnchanged() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.updateCurrentUser(USER_ID, new UpdateUserRequestDto(null, null, "jane.doe@gmail.com", null, null));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void updateCurrentUser_throws_whenChangingEmailWithoutPassword() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));

        assertThatThrownBy(() -> userService.updateCurrentUser(USER_ID,
                new UpdateUserRequestDto(null, null, "janet@gmail.com", null, null)))
                .isInstanceOf(InvalidCurrentPasswordException.class)
                .hasMessage("Current password is required to change your email");
    }

    @Test
    void updateCurrentUser_throws_whenCurrentPasswordIsWrong() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateCurrentUser(USER_ID,
                new UpdateUserRequestDto(null, null, "janet@gmail.com", null, "wrong")))
                .isInstanceOf(InvalidCurrentPasswordException.class)
                .hasMessage("Current password is incorrect");
        assertThat(user.getEmail()).isEqualTo("jane.doe@gmail.com");
    }

    @Test
    void updateCurrentUser_throwsConflict_whenEmailTaken() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.existsByEmail("taken@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUser(USER_ID,
                new UpdateUserRequestDto(null, null, "taken@gmail.com", null, "secret123")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void updateCurrentUser_throwsConflict_whenPhoneTaken() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(userRepository.existsByPhoneNumber("0711000000")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUser(USER_ID,
                new UpdateUserRequestDto(null, null, null, "0711000000", null)))
                .isInstanceOf(PhoneNumberAlreadyExistsException.class);
    }

    @Test
    void updateCurrentUser_throwsSuspended_whenUserIsNotActive() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> userService.updateCurrentUser(USER_ID,
                new UpdateUserRequestDto("Janet", null, null, null, null)))
                .isInstanceOf(UserSuspendedException.class);
    }

    @Test
    void changePassword_savesNewHashAndRevokesRefreshTokens() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassw0rd!", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("NewPassw0rd!")).thenReturn("new-hash");

        userService.changePassword(USER_ID, new ChangePasswordRequestDto("OldPassw0rd!", "NewPassw0rd!"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenService).revokeAllFor(USER_ID);
    }

    @Test
    void changePassword_throws_whenCurrentPasswordIsWrong() {
        var user = user(UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(USER_ID,
                new ChangePasswordRequestDto("wrong", "NewPassw0rd!")))
                .isInstanceOf(InvalidCurrentPasswordException.class)
                .hasMessage("Current password is incorrect");

        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenService, never()).revokeAllFor(any());
    }

    @Test
    void changePassword_throwsSuspended_whenUserIsNotActive() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> userService.changePassword(USER_ID,
                new ChangePasswordRequestDto("OldPassw0rd!", "NewPassw0rd!")))
                .isInstanceOf(UserSuspendedException.class);

        verify(refreshTokenService, never()).revokeAllFor(any());
    }

    @Test
    void changePassword_throwsAccountNotFound_whenUserNoLongerExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(USER_ID,
                new ChangePasswordRequestDto("OldPassw0rd!", "NewPassw0rd!")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private User user(UserStatus status) {
        return User.builder()
                .id(USER_ID)
                .email("jane.doe@gmail.com")
                .firstName("Jane")
                .lastName("Doe")
                .passwordHash("hashed")
                .status(status)
                .build();
    }
}
