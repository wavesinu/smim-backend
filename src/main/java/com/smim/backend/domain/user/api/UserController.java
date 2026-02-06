package com.smim.backend.domain.user.api;

import com.smim.backend.domain.user.dto.UpdateCefrLevelRequest;
import com.smim.backend.domain.user.dto.UpdateCefrLevelResponse;
import com.smim.backend.domain.user.dto.UpdateUserRequest;
import com.smim.backend.domain.user.dto.UserResponse;
import com.smim.backend.domain.user.service.UserService;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 프로필 API 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserResponse response = userService.getMyProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내 프로필 수정
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateMyProfile(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * CEFR 레벨 설정
     */
    @PatchMapping("/me/cefr-level")
    public ResponseEntity<ApiResponse<UpdateCefrLevelResponse>> updateCefrLevel(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateCefrLevelRequest request
    ) {
        UpdateCefrLevelResponse response = userService.updateCefrLevel(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
