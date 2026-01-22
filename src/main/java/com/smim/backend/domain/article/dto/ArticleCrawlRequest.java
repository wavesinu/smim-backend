package com.smim.backend.domain.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * 아티클 크롤링 요청 DTO
 */
@Getter
@NoArgsConstructor
public class ArticleCrawlRequest {

    @NotBlank(message = "URL은 필수입니다.")
    @URL(message = "올바른 URL 형식이 아닙니다.")
    @Size(max = 2000, message = "URL은 2000자를 초과할 수 없습니다.")
    private String url;
}
