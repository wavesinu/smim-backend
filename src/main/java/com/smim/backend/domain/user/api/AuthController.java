package com.smim.backend.domain.user.api;

import com.smim.backend.domain.user.dto.EmailLoginRequest;
import com.smim.backend.domain.user.dto.EmailSignupRequest;
import com.smim.backend.domain.user.dto.TokenRefreshRequest;
import com.smim.backend.domain.user.dto.TokenResponse;
import com.smim.backend.domain.user.service.AuthService;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러
 * JWT 토큰 갱신 및 로그아웃 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 이메일 회원가입
     * @param request 회원가입 요청
     * @return Access Token과 Refresh Token
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(
            @Valid @RequestBody EmailSignupRequest request
    ) {
        TokenResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 이메일 로그인
     * @param request 로그인 요청
     * @return Access Token과 Refresh Token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody EmailLoginRequest request
    ) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Access Token 갱신
     * @param request Refresh Token을 포함한 요청
     * @return 새로운 Access Token과 Refresh Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        TokenResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 로그아웃
     * Redis에 저장된 Refresh Token을 삭제합니다.
     * @param userPrincipal 현재 로그인한 사용자 정보
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        authService.logout(userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
}
