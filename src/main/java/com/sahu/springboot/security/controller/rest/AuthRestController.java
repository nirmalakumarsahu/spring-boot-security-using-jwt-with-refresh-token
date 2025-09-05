package com.sahu.springboot.security.controller.rest;

import com.sahu.springboot.security.constant.AuthConstants;
import com.sahu.springboot.security.dto.*;
import com.sahu.springboot.security.model.RefreshToken;
import com.sahu.springboot.security.model.User;
import com.sahu.springboot.security.security.dto.CustomUserDetails;
import com.sahu.springboot.security.security.util.JwtTokenProvider;
import com.sahu.springboot.security.security.util.SecurityUtil;
import com.sahu.springboot.security.service.RefreshTokenService;
import com.sahu.springboot.security.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        if (authentication.isAuthenticated()) {
            SecurityContextHolder.getContext().setAuthentication(authentication);

            CustomUserDetails customUserDetails = SecurityUtil.getCurrentUser();
            String token = jwtTokenProvider.generateToken(customUserDetails.getUsername());

            return ApiResponse.success(HttpStatus.OK, "User Login Successfully",
                    LoginResponse.builder()
                            .token(token)
                            .expirationDate(jwtTokenProvider.getExpirationDate(token))
                            .tokenType(AuthConstants.TOKEN_TYPE_BEARER)
                            .refreshToken(refreshTokenService.createRefreshToken(loginRequest.username()).getToken())
                            .build()
            );
        }

        return ApiResponse.failure(HttpStatus.UNAUTHORIZED, "Invalid username or password",
                null);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody UserRequest userRequest) {
        log.debug("Registration process started for user: {}", userRequest.username());

        //Check if the user already exists
        if (userService.existsByUsername(userRequest.username())) {
            return ApiResponse.failure(HttpStatus.CONFLICT, "Username already exists",
                    null);
        }

        if (userService.existsByEmail(userRequest.email())) {
            return ApiResponse.failure(HttpStatus.CONFLICT, "Email already exists",
                    null);
        }

        //Add the user
        User user = userService.addUser(userRequest);
        if (Objects.nonNull(user)) {
            return ApiResponse.success(HttpStatus.CREATED, "Registration successful!",
                    UserResponse.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .build()
            );
        }

        return ApiResponse.failure(HttpStatus.BAD_REQUEST, "Registration failed. Please try again.",
                null);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return refreshTokenService.findByToken(refreshTokenRequest.token())
                .map(refreshTokenService::verifyRefreshToken)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtTokenProvider.generateToken(user.getUsername());
                    return ApiResponse.success(HttpStatus.OK, "Token refreshed successfully",
                            LoginResponse.builder()
                                    .token(token)
                                    .expirationDate(jwtTokenProvider.getExpirationDate(token))
                                    .tokenType(AuthConstants.TOKEN_TYPE_BEARER)
                                    .refreshToken(refreshTokenRequest.token())
                                    .build()
                    );
                }).orElse(
                        ApiResponse.failure(HttpStatus.UNAUTHORIZED, "Invalid refresh token",
                                null)
                );
    }

}
