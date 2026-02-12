package com.smim.backend.domain.user.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smim.backend.domain.user.dto.PasswordResetRequestResponse;
import com.smim.backend.domain.user.dto.PasswordResetResponse;
import com.smim.backend.domain.user.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController WebMvc 테스트")
class AuthControllerWebMvcTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 API 성공")
    void requestPasswordReset_success() throws Exception {
        PasswordResetRequestResponse response = PasswordResetRequestResponse.builder()
                .requested(true)
                .expiresInSeconds(900L)
                .build();
        given(authService.requestPasswordReset("user@example.com")).willReturn(response);

        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RequestBody("user@example.com", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.requested").value(true))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(900));
    }

    @Test
    @DisplayName("비밀번호 재설정 API 성공")
    void resetPassword_success() throws Exception {
        PasswordResetResponse response = PasswordResetResponse.builder()
                .reset(true)
                .updatedAt(Instant.parse("2026-02-12T00:00:00Z"))
                .build();
        given(authService.resetPassword("token-123", "NewPass1!")).willReturn(response);

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RequestBody(null, "token-123", "NewPass1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.reset").value(true))
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    private record RequestBody(String email, String token, String newPassword) {
    }
}
