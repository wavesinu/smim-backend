package com.smim.backend.domain.notification.api;

import com.smim.backend.domain.notification.NotificationService;
import com.smim.backend.domain.notification.dto.NotificationSettingsResponse;
import com.smim.backend.domain.notification.dto.NotificationSettingsUpdateRequest;
import com.smim.backend.domain.notification.dto.NotificationSettingsUpdateResponse;
import com.smim.backend.domain.notification.dto.NotificationTestRequest;
import com.smim.backend.domain.notification.dto.NotificationTestResponse;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getSettings(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        NotificationSettingsResponse response = notificationService.getSettings(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingsUpdateResponse>> updateSettings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody NotificationSettingsUpdateRequest request
    ) {
        NotificationSettingsUpdateResponse response = notificationService.updateSettings(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<NotificationTestResponse>> sendTest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody NotificationTestRequest request
    ) {
        NotificationTestResponse response = notificationService.sendTest(userPrincipal.getId(), request.getChannel());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
