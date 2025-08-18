package com.sahu.springboot.security.service.impl;

import com.sahu.springboot.security.dto.RefreshTokenRequest;
import com.sahu.springboot.security.model.RefreshToken;
import com.sahu.springboot.security.repository.RefreshTokenRepository;
import com.sahu.springboot.security.repository.UserRepository;
import com.sahu.springboot.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.jwt.refreshTokenExpiration}")
    private long refreshTokenDuration;

    @Override
    public RefreshToken createRefreshToken(String username) {
        log.debug("Creating refresh token for user: {}", username);

        return userRepository.findByUsername(username).map(user -> {
            // Delete existing refresh token if it exists
            refreshTokenRepository.findByUserId(user.getId()).ifPresent(refreshTokenRepository::delete);

            return refreshTokenRepository.save(
                    RefreshToken.builder()
                            .user(user)
                            .token(UUID.randomUUID().toString())
                            .expiryDate(Instant.now().plusMillis(refreshTokenDuration))
                            .build());
        }).orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken verifyRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);

            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

}
