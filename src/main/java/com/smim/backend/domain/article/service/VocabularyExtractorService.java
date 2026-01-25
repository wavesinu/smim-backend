package com.smim.backend.domain.article.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smim.backend.domain.article.dto.ArticleVocabularyResponse;
import com.smim.backend.global.ai.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 아티클에서 단어를 추출하는 서비스
 * AI 서비스를 이용하여 본문에서 중요한 단어와 의미를 추출합니다.
 * global.ai.AiService를 통해 AI 기능을 사용하며, 응답 파싱은 도메인 내에서 처리합니다.
 */
@Slf4j
@Service
public class VocabularyExtractorService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public VocabularyExtractorService(AiService aiService) {
        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 아티클 본문에서 단어 추출
     *
     * @param content 아티클 본문
     * @param limit   추출할 단어 개수
     * @return 추출된 단어 목록
     */
    public List<ArticleVocabularyResponse> extractVocabulary(String content, int limit) {
        log.info("단어 추출 시작 - 본문 길이: {}, 제한: {}", content.length(), limit);

        try {
            String prompt = buildPrompt(content, limit);
            String response = aiService.generateContent(prompt);

            if (response.isEmpty()) {
                log.warn("AI 응답이 비어있습니다.");
                return List.of();
            }

            return parseResponse(response);

        } catch (Exception e) {
            log.error("단어 추출 실패", e);
            return List.of();
        }
    }

    /**
     * AI에게 전달할 프롬프트 생성
     */
    private String buildPrompt(String content, int limit) {
        return String.format("""
                다음 영어 아티클에서 중요한 단어 %d개를 추출하고, 각 단어에 대해 다음 정보를 JSON 형식으로 반환해 주세요:

                1. word: 추출된 영어 단어
                2. definition: 한글 뜻
                3. contextSentence: 원문에서 해당 단어가 사용된 문장

                반환 형식:
                [
                  {
                    "word": "example",
                    "definition": "예시",
                    "contextSentence": "원문에서 발췌한 문장"
                  },
                  ...
                ]

                아티클 본문:
                %s

                **중요**: JSON 형식으로만 응답해 주세요. 다른 설명은 포함하지 마세요.
                """, limit, content);
    }

    /**
     * AI 응답을 파싱하여 ArticleVocabularyResponse 리스트로 변환
     */
    private List<ArticleVocabularyResponse> parseResponse(String response) {
        try {
            // Gemini 응답에서 텍스트 추출
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidates = rootNode.path("candidates");

            if (candidates.isEmpty()) {
                log.warn("AI 응답에 candidates가 없습니다.");
                return List.of();
            }

            String textContent = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // JSON 배열 부분만 추출 (마크다운 코드 블록 제거)
            String jsonArray = extractJsonArray(textContent);

            return objectMapper.readValue(jsonArray, new TypeReference<>() {});

        } catch (JsonProcessingException e) {
            log.error("AI 응답 파싱 실패: {}", response, e);
            return List.of();
        }
    }

    /**
     * 텍스트에서 JSON 배열 부분만 추출
     * 마크다운 코드 블록이 포함된 경우 처리
     */
    private String extractJsonArray(String text) {
        String trimmed = text.trim();

        // 마크다운 코드 블록 제거
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }
}
