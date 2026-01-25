package com.smim.backend.global.auth.oauth2;

import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.auth.jwt.JwtTokenProvider;
import com.smim.backend.global.config.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2 인증 성공 핸들러
 * OAuth2 로그인 성공 시 실행되어 JWT 토큰을 생성하고 프론트엔드로 리다이렉트합니다.
 * - Access Token과 Refresh Token 생성
 * - Refresh Token을 Redis에 저장
 * - Query Parameter로 토큰을 전달하여 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final AppProperties appProperties;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 리다이렉트 URL 생성
     * JWT 토큰을 생성하고 query parameter로 포함시킵니다.
     * TODO: authorizedRedirectUris 배열에서 적절한 URI를 선택하는 로직 추가
     */
    protected String determineTargetUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        List<String> authorizedRedirectUris = appProperties.getOauth2().getAuthorizedRedirectUris();
        // TODO: 리다이렉트 URI 검증 로직 추가
        String targetUrl = authorizedRedirectUris.get(0); // 기본값 사용

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId());

        // Refresh Token을 Redis에 저장
        saveRefreshToken(userPrincipal.getId(), refreshToken);

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();
    }

    /**
     * Refresh Token을 Redis에 저장
     * key: "refresh_token:{userId}", value: refreshToken
     */
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
