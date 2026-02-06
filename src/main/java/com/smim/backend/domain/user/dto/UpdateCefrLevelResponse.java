package com.smim.backend.domain.user.dto;

import com.smim.backend.domain.user.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UpdateCefrLevelResponse {
    private CefrLevel cefrLevel;
    private Instant updatedAt;
}
