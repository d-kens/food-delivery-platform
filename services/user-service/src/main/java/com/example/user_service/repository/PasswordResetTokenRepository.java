package com.example.user_service.repository;

import com.example.user_service.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndCreatedAtAfter(UUID userId, Instant createdAfter);

    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int invalidateUnusedForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
