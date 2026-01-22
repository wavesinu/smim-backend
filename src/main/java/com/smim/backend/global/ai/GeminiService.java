package com.smim.backend.global.ai;

import com.smim.backend.domain.article.dto.ArticleVocabularyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini AI 서비스
 * 아티클 본문에서 단어를 추출하는 AI 서비스
 */
@Slf4j
@Service
public class GeminiService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.timeout:30000}") int timeout
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("GeminiService 초기화 완료 - Model: {}, Timeout: {}ms", model, timeout);
    }

    /**
     * 아티클 본문에서 단어 추출
     * Gemini API를 호출하여 본문에서 중요한 단어와 그 의미를 추출합니다.
     *
     * @param content 아티클 본문
     * @param limit   추출할 단어 개수 (기본 10개)
     * @return 추출된 단어 목록
     */
    public List<ArticleVocabularyResponse> extractVocabulary(String content, int limit) {
        log.info("Gemini API 단어 추출 시작 - 본문 길이: {}, 제한: {}", content.length(), limit);

        try {
            // 프롬프트 생
            String prompt = buildPrompt(content, limit);

            // Gemini API 요청 바디
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "topP", 0.95,
                            "topK", 40,
                            "maxOutputTokens", 2048
                    )
            );

            // API 호출
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            log.info("Gemini API 응답 수신 완료");
            log.debug("Gemini API 응답: {}", response);

            // 응답 파싱 (Phase 5에서 완성 예정 - 현재는 빈 리스트 반환)
            // TODO: JSON 파싱하여 ArticleVocabularyResponse 리스트로 변환
            return List.of();

        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            // 실패 시 빈 리스트 반환 (서비스 계속 동작)
            return List.of();
        }
    }

    /**
     * Gemini에게 전달할 프롬프트 생성
     */
    private String buildPrompt(String content, int limit) {
        return String.format("""
                다음 영어 아티클에서 중요한 단어 %d개를 추출하고, 각 단어에 대해 다음 정보를 JSON 형식으로 반환해 주세요:

                1. word: 추출된 영어 단어
                2. meaning: 한글 뜻
                3. example: 예문 (영어)
                4. originalSentence: 원문에서 해당 단어가 사용된 문장

                반환 형식:
                [
                  {
                    "word": "example",
                    "meaning": "예시",
                    "example": "This is an example sentence.",
                    "originalSentence": "원문에서 발췌한 문장"
                  },
                  ...
                ]

                아티클 본문:
                %s

                **중요**: JSON 형식으로만 응답해 주세요. 다른 설명은 포함하지 마세요.
                """, limit, content);
    }
}
