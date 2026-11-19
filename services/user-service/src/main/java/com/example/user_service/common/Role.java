package com.example.user_service.common;

import java.util.Collection;
import java.util.List;

public enum Role {
    USER,
    RIDER,
    ADMIN;

    public static List<String> sortedNames(Collection<Role> roles) {
        return roles.stream().map(Role::name).sorted().toList();
    }
}
