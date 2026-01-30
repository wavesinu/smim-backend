package com.smim.backend.global.ai;

/**
 * AI 서비스 인터페이스
 * 텍스트 생성 기능을 추상화하여 AI 제공자(Gemini, OpenAI, etc.) 교체를 용이하게 합니다.
 */
public interface AiService {

    /**
     * 프롬프트를 기반으로 텍스트 생성
     *
     * @param prompt 프롬프트 텍스트
     * @return 생성된 응답 텍스트
     */
    String generateContent(String prompt);
}
