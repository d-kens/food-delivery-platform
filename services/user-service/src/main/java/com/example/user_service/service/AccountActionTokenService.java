package com.example.user_service.service;

import com.example.user_service.common.AccountActionPurpose;
import com.example.user_service.common.UserStatus;
import com.example.user_service.config.AccountActionProperties;
import com.example.user_service.entity.AccountActionToken;
import com.example.user_service.entity.User;
import com.example.user_service.exception.InvalidAccountActionTokenException;
import com.example.user_service.repository.AccountActionTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountActionTokenService {

    private final AccountActionTokenRepository tokenRepository;
    private final TokenGenerator tokenGenerator;
    private final AccountActionProperties properties;

    @Transactional
    public Optional<String> create(final User user, final AccountActionPurpose purpose, final String newEmail) {
        var now = Instant.now();
        if (tokenRepository.existsByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose, now.minus(properties.requestInterval()))) {
            return Optional.empty();
        }

        tokenRepository.invalidateUnusedForUser(user.getId(), purpose, now);

        var rawToken = tokenGenerator.generate();
        tokenRepository.save(AccountActionToken.builder()
                .user(user)
                .purpose(purpose)
                .tokenHash(tokenGenerator.hash(rawToken))
                .newEmail(newEmail)
                .expiresAt(now.plus(properties.ttl()))
                .build());

        return Optional.of(rawToken);
    }

    @Transactional
    public AccountActionToken consume(final String rawToken, final AccountActionPurpose purpose) {
        var token = tokenRepository.findByTokenHashAndPurpose(tokenGenerator.hash(rawToken), purpose)
                .filter(AccountActionToken::isUsable)
                .filter(found -> found.getUser().getStatus() == UserStatus.ACTIVE)
                .orElseThrow(InvalidAccountActionTokenException::new);

        token.setUsedAt(Instant.now());
        return token;
    }
}
