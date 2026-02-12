package com.smim.backend.domain.vocabularybook.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SourceArticleInfo {
    private Long id;
    private String title;
}
