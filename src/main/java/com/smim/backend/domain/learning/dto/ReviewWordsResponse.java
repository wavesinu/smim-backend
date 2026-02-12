package com.smim.backend.domain.learning.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewWordsResponse {
    private List<ReviewWordResponse> reviewWords;
    private int totalCount;
}
