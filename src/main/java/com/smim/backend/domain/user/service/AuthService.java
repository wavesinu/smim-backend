package com.smim.backend.domain.user.service;

import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.domain.user.dto.EmailLoginRequest;
import com.smim.backend.domain.user.dto.EmailSignupRequest;
import com.smim.backend.domain.user.dto.PasswordResetRequestResponse;
import com.smim.backend.domain.user.dto.PasswordResetResponse;
import com.smim.backend.domain.user.dto.TokenResponse;
import com.smim.backend.domain.vocabularybook.service.VocabularyBookService;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.auth.jwt.JwtTokenProvider;
import com.smim.backend.global.error.ErrorCode;
import com.smim.backend.global.error.exception.BusinessException;
import com.smim.backend.global.error.exception.TokenValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 인증 서비스
 * JWT 토큰 갱신 및 로그아웃 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final VocabularyBookService vocabularyBookService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${auth.password-reset-token-expiration:900000}")
    private long passwordResetTokenExpiration;

    /**
     * 이메일 회원가입
     * @param request 회원가입 요청
     * @return Access Token 및 Refresh Token
     */
    @Transactional
    public TokenResponse signup(EmailSignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .provider(Provider.LOCAL)
                .providerId(null)
                .role(Role.USER)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);
        vocabularyBookService.createDefaultBook(user);

        return issueTokens(user);
    }

    /**
     * 이메일 로그인
     * @param request 로그인 요청
     * @return Access Token 및 Refresh Token
     */
    @Transactional(readOnly = true)
    public TokenResponse login(EmailLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (user.getProvider() != Provider.LOCAL) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_LOGIN_REQUIRED);
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return issueTokens(user);
    }

    /**
     * Access Token 갱신
     * Refresh Token을 검증하고 새로운 Access Token과 Refresh Token을 발급합니다.
     * @param refreshToken 클라이언트의 Refresh Token
     * @return 새로운 Access Token과 Refresh Token
     * @throws TokenValidationException Refresh Token이 유효하지 않은 경우
     */
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
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 새로운 Access Token 생성
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        // 새로운 Refresh Token 생성 (선택적 - Refresh Token Rotation)
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        // Redis에 새로운 Refresh Token 저장
        saveRefreshToken(userId, newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃
     * Redis에 저장된 Refresh Token을 삭제하여 토큰을 무효화합니다.
     * @param userId 로그아웃할 사용자 ID
     */
    public void logout(Long userId) {
        String key = "refresh_token:" + userId;
        redisTemplate.delete(key);
        log.info("User {} logged out successfully", userId);
    }

    public PasswordResetRequestResponse requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getProvider() != Provider.LOCAL || user.getPassword() == null) {
                return;
            }
            String resetToken = UUID.randomUUID().toString();
            String key = "password_reset:" + resetToken;
            redisTemplate.opsForValue().set(
                    key,
                    String.valueOf(user.getId()),
                    passwordResetTokenExpiration,
                    TimeUnit.MILLISECONDS
            );
            // TODO: MailService 연동 시 resetToken을 이메일로 발송
            log.info("Password reset token issued for userId={}", user.getId());
        });

        return PasswordResetRequestResponse.builder()
                .requested(true)
                .expiresInSeconds(TimeUnit.MILLISECONDS.toSeconds(passwordResetTokenExpiration))
                .build();
    }

    @Transactional
    public PasswordResetResponse resetPassword(String token, String newPassword) {
        String key = "password_reset:" + token;
        String userIdValue = redisTemplate.opsForValue().get(key);
        if (userIdValue == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdValue);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getProvider() != Provider.LOCAL) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_LOGIN_REQUIRED);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        redisTemplate.delete(key);

        return PasswordResetResponse.builder()
                .reset(true)
                .updatedAt(Instant.now())
                .build();
    }

    private TokenResponse issueTokens(User user) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());
        saveRefreshToken(user.getId(), refreshToken);
        return new TokenResponse(accessToken, refreshToken);
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        String key = "refresh_token:" + userId;
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );
    }
}
