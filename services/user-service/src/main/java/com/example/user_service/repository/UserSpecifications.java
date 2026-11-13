package com.example.user_service.repository;

import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> role == null ? null : cb.isMember(role, root.get("roles"));
    }

    public static Specification<User> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("email")), pattern, '\\'),
                    cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("lastName")), pattern, '\\')
            );
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
