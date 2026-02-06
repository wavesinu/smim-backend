package com.smim.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateCefrLevelRequest {

    @NotBlank(message = "CEFR 레벨은 필수입니다")
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)$", message = "CEFR 레벨은 A1~C2 중 하나여야 합니다")
    private String cefrLevel;
}
