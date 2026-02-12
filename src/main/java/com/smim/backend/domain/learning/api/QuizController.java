package com.smim.backend.domain.learning.api;

import com.smim.backend.domain.learning.QuizService;
import com.smim.backend.domain.learning.dto.QuizGenerateRequest;
import com.smim.backend.domain.learning.dto.QuizGenerateResponse;
import com.smim.backend.domain.learning.dto.QuizSubmitRequest;
import com.smim.backend.domain.learning.dto.QuizSubmitResponse;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QuizGenerateResponse>> generateQuiz(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody QuizGenerateRequest request
    ) {
        QuizGenerateResponse response = quizService.generateQuiz(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<ApiResponse<QuizSubmitResponse>> submitQuiz(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String sessionId,
            @Valid @RequestBody QuizSubmitRequest request
    ) {
        QuizSubmitResponse response = quizService.submitQuiz(userPrincipal.getId(), sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
