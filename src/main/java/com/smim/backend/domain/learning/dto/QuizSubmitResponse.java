package com.smim.backend.domain.learning.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizSubmitResponse {
    private int correctCount;
    private int totalCount;
    private double accuracy;
    private List<QuizResultItem> results;
    private List<UpdatedScheduleItem> updatedSchedules;
}
