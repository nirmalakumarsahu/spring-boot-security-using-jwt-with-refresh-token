package com.sahu.springboot.security.service;

import com.sahu.springboot.security.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username);

    Optional<RefreshToken> findByToken(String token);

    RefreshToken verifyRefreshToken(RefreshToken refreshToken);
}
