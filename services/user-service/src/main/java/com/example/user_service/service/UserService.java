package com.example.user_service.service;

import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.ChangePasswordRequestDto;
import com.example.user_service.dto.DeleteAccountRequestDto;
import com.example.user_service.dto.PageResponseDto;
import com.example.user_service.dto.UpdateUserRequestDto;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.entity.User;
import com.example.user_service.exception.AccountNotFoundException;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCurrentPasswordException;
import com.example.user_service.exception.InvalidStatusChangeException;
import com.example.user_service.exception.OwnRoleChangeException;
import com.example.user_service.exception.OwnStatusChangeException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.exception.UserDeletedException;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.exception.UserSuspendedException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static com.example.user_service.repository.UserSpecifications.hasRole;
import static com.example.user_service.repository.UserSpecifications.hasStatus;
import static com.example.user_service.repository.UserSpecifications.matchesSearch;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(final UUID userId) {
        return userMapper.toUserResponse(findActiveUser(userId));
    }

    @Transactional
    public UserResponseDto updateCurrentUser(final UUID userId, final UpdateUserRequestDto request) {
        var user = findActiveUser(userId);

        if (request.email() != null && !request.email().trim().equals(user.getEmail())) {
            changeEmail(user, request.email().trim(), request.currentPassword());
        }

        if (request.phoneNumber() != null && !request.phoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
                throw new PhoneNumberAlreadyExistsException("An account with this phone number already exists");
            }
            user.setPhoneNumber(request.phoneNumber());
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }

        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void changePassword(final UUID userId, final ChangePasswordRequestDto request) {
        var user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllFor(userId);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<UserResponseDto> getUsers(
            final UserStatus status, final Role role, final String search, final int page, final int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        var spec = hasStatus(status).and(hasRole(role)).and(matchesSearch(search));

        return PageResponseDto.from(userRepository.findAll(spec, pageable).map(userMapper::toUserResponse));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(final UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toUserResponse)
                .orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public void updateUserStatus(final UUID actorId, final UUID userId, final UserStatus status) {
        if (status == UserStatus.DELETED) {
            throw new InvalidStatusChangeException("Accounts can only be deleted by their owner");
        }

        if (actorId.equals(userId)) {
            throw new OwnStatusChangeException();
        }

        var user = findNotDeletedUser(userId);

        user.setStatus(status);

        if (status != UserStatus.ACTIVE) {
            refreshTokenService.revokeAllFor(userId);
        }
    }

    @Transactional
    public UserResponseDto updateUserRoles(final UUID actorId, final UUID userId, final Set<Role> roles) {
        if (actorId.equals(userId)) {
            throw new OwnRoleChangeException();
        }

        var user = findNotDeletedUser(userId);

        user.getRoles().clear();
        user.getRoles().addAll(roles);

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void deleteCurrentUser(final UUID userId, final DeleteAccountRequestDto request) {
        var user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException("Current password is incorrect");
        }

        user.setEmail("deleted-" + userId + "@deleted.invalid");
        user.setPhoneNumber(null);
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.getRoles().clear();
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(Instant.now());

        refreshTokenService.revokeAllFor(userId);
    }

    private User findNotDeletedUser(final UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getStatus() == UserStatus.DELETED) {
            throw new UserDeletedException();
        }

        return user;
    }

    private User findActiveUser(final UUID userId) {
        var user = userRepository.findById(userId)
                .filter(found -> found.getStatus() != UserStatus.DELETED)
                .orElseThrow(AccountNotFoundException::new);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserSuspendedException("Your account has been suspended");
        }

        return user;
    }

    private void changeEmail(final User user, final String newEmail, final String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new InvalidCurrentPasswordException("Current password is required to change your email");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException("Current password is incorrect");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        user.setEmail(newEmail);
    }
}
