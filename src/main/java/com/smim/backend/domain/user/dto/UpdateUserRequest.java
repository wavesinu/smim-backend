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

}
