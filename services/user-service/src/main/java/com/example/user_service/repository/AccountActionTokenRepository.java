package com.example.user_service.repository;

import com.example.user_service.common.AccountActionPurpose;
import com.example.user_service.entity.AccountActionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, Long> {

    Optional<AccountActionToken> findByTokenHashAndPurpose(String tokenHash, AccountActionPurpose purpose);

    boolean existsByUserIdAndPurposeAndCreatedAtAfter(UUID userId, AccountActionPurpose purpose, Instant createdAfter);

    @Modifying
    @Query("update AccountActionToken t set t.usedAt = :now where t.user.id = :userId and t.purpose = :purpose and t.usedAt is null")
    int invalidateUnusedForUser(@Param("userId") UUID userId,
                                @Param("purpose") AccountActionPurpose purpose,
                                @Param("now") Instant now);
}
