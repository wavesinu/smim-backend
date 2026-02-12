package com.smim.backend.domain.user.service;

import com.smim.backend.domain.user.Provider;
import com.smim.backend.domain.user.Role;
import com.smim.backend.domain.user.User;
import com.smim.backend.domain.user.UserRepository;
import com.smim.backend.domain.user.dto.PasswordResetRequestResponse;
import com.smim.backend.domain.user.dto.PasswordResetResponse;
import com.smim.backend.domain.vocabularybook.service.VocabularyBookService;
import com.smim.backend.global.auth.jwt.JwtTokenProvider;
import com.smim.backend.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VocabularyBookService vocabularyBookService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
        ReflectionTestUtils.setField(authService, "passwordResetTokenExpiration", 900000L);
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 시 LOCAL 계정 토큰 저장")
    void requestPasswordReset_localAccount_savesResetToken() {
        User user = localUser(1L, "user@example.com");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

        PasswordResetRequestResponse response = authService.requestPasswordReset("user@example.com");

        assertThat(response.isRequested()).isTrue();
        assertThat(response.getExpiresInSeconds()).isEqualTo(900L);
        verify(valueOperations).set(
                startsWith("password_reset:"),
                eq("1"),
                eq(900000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 시 소셜 계정은 토큰을 발급하지 않음")
    void requestPasswordReset_socialAccount_doesNotIssueToken() {
        User user = socialUser(2L, "social@example.com");
        given(userRepository.findByEmail("social@example.com")).willReturn(Optional.of(user));

        PasswordResetRequestResponse response = authService.requestPasswordReset("social@example.com");

        assertThat(response.isRequested()).isTrue();
        verify(valueOperations, never()).set(anyString(), anyString(), eq(900000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("비밀번호 재설정 완료 성공")
    void resetPassword_success() {
        String token = "reset-token";
        User user = localUser(3L, "local@example.com");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("password_reset:" + token)).willReturn("3");
        given(userRepository.findById(3L)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPass1!")).willReturn("encoded-password");

        PasswordResetResponse response = authService.resetPassword(token, "NewPass1!");

        assertThat(response.isReset()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(redisTemplate).delete("password_reset:" + token);
    }

    @Test
    @DisplayName("유효하지 않은 reset token이면 예외 발생")
    void resetPassword_invalidToken_throwsException() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("password_reset:invalid")).willReturn(null);

        assertThatThrownBy(() -> authService.resetPassword("invalid", "NewPass1!"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("잘못된 요청");
    }

    private User localUser(Long id, String email) {
        User user = User.builder()
                .email(email)
                .name("Local User")
                .provider(Provider.LOCAL)
                .providerId(null)
                .role(Role.USER)
                .password("old-password")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User socialUser(Long id, String email) {
        User user = User.builder()
                .email(email)
                .name("Social User")
                .provider(Provider.GOOGLE)
                .providerId("google-id")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
