package com.example.user_service.repository;

import com.example.user_service.common.AuthProvider;
import com.example.user_service.entity.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    Optional<UserIdentity> findByProviderAndSubject(AuthProvider provider, String subject);

    boolean existsByUserIdAndProvider(UUID userId, AuthProvider provider);

    @Modifying
    @Query("delete from UserIdentity i where i.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
