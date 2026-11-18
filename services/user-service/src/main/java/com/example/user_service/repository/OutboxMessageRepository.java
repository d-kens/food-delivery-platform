package com.example.user_service.repository;

import com.example.user_service.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findTop100ByOrderByCreatedAtAsc();

    @Query(value = "SELECT pg_try_advisory_xact_lock(:key)", nativeQuery = true)
    boolean tryAdvisoryXactLock(@Param("key") long key);
}
