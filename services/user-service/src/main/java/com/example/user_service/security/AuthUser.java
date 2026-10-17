package com.example.user_service.security;

import com.example.user_service.common.Role;

import java.util.List;

public record AuthUser(
        String userId,
        List<Role> roles
) {}
