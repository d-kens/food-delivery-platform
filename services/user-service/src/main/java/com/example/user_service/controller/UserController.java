package com.example.user_service.controller;

import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.dto.ChangePasswordRequestDto;
import com.example.user_service.dto.PageResponseDto;
import com.example.user_service.dto.UpdateUserRequestDto;
import com.example.user_service.dto.UpdateUserStatusRequestDto;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.security.AccessTokenCookieService;
import com.example.user_service.security.RefreshTokenCookieService;
import com.example.user_service.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AccessTokenCookieService accessTokenCookieService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @GetMapping
    public PageResponseDto<UserResponseDto> getUsers(
            @RequestParam(required = false) final UserStatus status,
            @RequestParam(required = false) final Role role,
            @RequestParam(required = false) final String search,
            @RequestParam(defaultValue = "0") @Min(0) final int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) final int size) {
        return userService.getUsers(status, role, search, page, size);
    }

    @GetMapping("me")
    public UserResponseDto me(@AuthenticationPrincipal final Jwt jwt) {
        return userService.getCurrentUser(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("me")
    public UserResponseDto updateMe(
            @AuthenticationPrincipal final Jwt jwt,
            @Valid @RequestBody final UpdateUserRequestDto request) {
        return userService.updateCurrentUser(UUID.fromString(jwt.getSubject()), request);
    }

    @PatchMapping("me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal final Jwt jwt,
            @Valid @RequestBody final ChangePasswordRequestDto request) {
        userService.changePassword(UUID.fromString(jwt.getSubject()), request);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,
                        accessTokenCookieService.clearAccessTokenCookie().toString(),
                        refreshTokenCookieService.clearRefreshTokenCookie().toString())
                .build();
    }

    @GetMapping("{id}")
    public UserResponseDto getUserById(@PathVariable final UUID id) {
        return userService.getUserById(id);
    }

    @PatchMapping("{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(
            @AuthenticationPrincipal final Jwt jwt,
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateUserStatusRequestDto request) {
        userService.updateUserStatus(UUID.fromString(jwt.getSubject()), id, request.status());
    }
}
