package com.smim.backend.domain.user.service;

import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.domain.user.dto.TokenResponse;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.auth.jwt.JwtTokenProvider;
import com.smim.backend.global.error.exception.TokenValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public TokenResponse refreshAccessToken(String refreshToken) {
        // Refresh Token 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new TokenValidationException("Invalid refresh token");
        }

        // Refresh Token에서 사용자 ID 추출
        Long userId = tokenProvider.getUserIdFromToken(refreshToken);

        // Redis에서 저장된 Refresh Token 조회
        String key = "refresh_token:" + userId;
        String storedRefreshToken = redisTemplate.opsForValue().get(key);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new TokenValidationException("Refresh token not found or mismatched");
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 새로운 Access Token 생성
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        // 새로운 Refresh Token 생성 (선택적 - Refresh Token Rotation)
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        // Redis에 새로운 Refresh Token 저장
        redisTemplate.opsForValue().set(
                key,
                newRefreshToken,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId) {
        String key = "refresh_token:" + userId;
        redisTemplate.delete(key);
        log.info("User {} logged out successfully", userId);
    }
}
