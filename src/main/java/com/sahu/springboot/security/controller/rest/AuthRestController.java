package com.sahu.springboot.security.controller.rest;

import com.sahu.springboot.security.constant.AuthConstants;
import com.sahu.springboot.security.dto.*;
import com.sahu.springboot.security.model.User;
import com.sahu.springboot.security.security.util.JwtTokenProvider;
import com.sahu.springboot.security.service.RefreshTokenService;
import com.sahu.springboot.security.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO loginRequestDTO, HttpServletRequest httpServletRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.username(), loginRequestDTO.password()));

        if (authentication.isAuthenticated()) {
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = jwtTokenProvider.generateToken();

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "User Login Successfully",
                    LoginResponseDTO.builder()
                            .token(token)
                            .expirationDate(jwtTokenProvider.getExpirationDate(token))
                            .tokenType(AuthConstants.TOKEN_TYPE_BEARER)
                            .refreshToken(refreshTokenService.createRefreshToken(loginRequestDTO.username()).getToken())
                            .build(),
                    httpServletRequest.getRequestURI()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(HttpStatus.UNAUTHORIZED, "Invalid username or password",
                null,
                httpServletRequest.getRequestURI()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody UserRequestDTO userRequestDTO, HttpServletRequest request) {
        log.debug("Registration process started for user: {}", userRequestDTO.username());

        //Check if the user already exists
        if (userService.existsByUsername(userRequestDTO.username())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(HttpStatus.CONFLICT, "Username already exists",
                    null,
                    request.getRequestURI()));
        }

        if (userService.existsByEmail(userRequestDTO.email())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(HttpStatus.CONFLICT, "Email already exists",
                    null,
                    request.getRequestURI()));
        }

        //Add the user
        User user = userService.addUser(userRequestDTO);
        if (Objects.nonNull(user)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, "Registration successful!",
                    UserResponseDTO.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .build(),
                    request.getRequestURI()));

        }

        return ResponseEntity.badRequest().body(ApiResponse.failure(HttpStatus.BAD_REQUEST, "Registration failed. Please try again.",
                null,
                request.getRequestURI()));
    }

}
