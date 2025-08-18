package com.sahu.springboot.security.service;

import com.sahu.springboot.security.model.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username);
}
