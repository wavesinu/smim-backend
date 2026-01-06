package com.smim.backend.domain.user.api;

import com.smim.backend.domain.user.dto.TokenRefreshRequest;
import com.smim.backend.domain.user.dto.TokenResponse;
import com.smim.backend.domain.user.service.AuthService;
import com.smim.backend.global.auth.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        TokenResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        authService.logout(userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
}
