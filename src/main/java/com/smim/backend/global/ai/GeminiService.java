package com.smim.backend.global.ai;

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
 * AiService 인터페이스를 구현하여 텍스트 생성 기능을 제공합니다.
 * 도메인에 종속되지 않는 범용 AI 서비스입니다.
 */
@Slf4j
@Service
public class GeminiService implements AiService {

        private final WebClient webClient;
        private final String apiKey;
        private final String model;
        private final int timeout;

        public GeminiService(
                        @Value("${gemini.api-key}") String apiKey,
                        @Value("${gemini.model}") String model,
                        @Value("${gemini.timeout:30000}") int timeout) {
                this.apiKey = apiKey;
                this.model = model;
                this.timeout = timeout;
                this.webClient = WebClient.builder()
                                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .build();

                log.info("GeminiService 초기화 완료 - Model: {}, Timeout: {}ms", model, timeout);
        }

        /**
         * 프롬프트를 기반으로 텍스트 생성
         *
         * @param prompt 프롬프트 텍스트
         * @return 생성된 응답 텍스트 (실패 시 빈 문자열)
         */
        @Override
        public String generateContent(String prompt) {
                log.info("Gemini API 호출 시작 - 프롬프트 길이: {}", prompt.length());

                try {
                        Map<String, Object> requestBody = Map.of(
                                        "contents", List.of(
                                                        Map.of("parts", List.of(
                                                                        Map.of("text", prompt)))),
                                        "generationConfig", Map.of(
                                                        "temperature", 0.7,
                                                        "topP", 0.95,
                                                        "topK", 40,
                                                        "maxOutputTokens", 2048));

                        String response = webClient.post()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/{model}:generateContent")
                                                        .queryParam("key", apiKey)
                                                        .build(model))
                                        .bodyValue(requestBody)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .timeout(Duration.ofMillis(timeout))
                                        .block();

                        log.info("Gemini API 응답 수신 완료");
                        log.debug("Gemini API 응답: {}", response);

                        return response != null ? response : "";

                } catch (Exception e) {
                        log.error("Gemini API 호출 실패", e);
                        return "";
                }
        }
}
