package com.example.user_service.entity;

import com.example.user_service.common.AccountActionPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account_action_tokens")
public class AccountActionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private AccountActionPurpose purpose;

    private String tokenHash;

    private String newEmail;

    private Instant expiresAt;

    private Instant usedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public boolean isUsable() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }
}
