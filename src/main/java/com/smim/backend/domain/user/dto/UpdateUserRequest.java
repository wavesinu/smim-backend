package com.smim.backend.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * 사용자 프로필 수정 요청 DTO
 * API Spec: 6.2.2 Update My Profile Request
 */
@Getter
@NoArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
    private String name;

    @URL(protocol = "https", message = "올바른 URL 형식이어야 합니다")
    @Pattern(regexp = "^https://.*", message = "HTTPS 프로토콜을 지원하는 URL이어야 합니다")
    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다")
    private String profileImage;

    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)$", message = "CEFR 레벨은 A1~C2 중 하나여야 합니다")
    private String targetCefrLevel;

    private Boolean notificationEnabled;

    @Pattern(regexp = "^(EMAIL|KAKAO|BOTH|NONE)$", message = "알림 채널은 EMAIL, KAKAO, BOTH, NONE 중 하나여야 합니다")
    private String notificationChannel;

    @Pattern(regexp = "^([01]\\d|2[0-3]):(00|30)$", message = "알림 시간은 HH:mm 형식의 30분 단위여야 합니다")
    private String notificationTime;

}
