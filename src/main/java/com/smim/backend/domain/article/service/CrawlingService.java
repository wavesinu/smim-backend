package com.smim.backend.domain.article.service;

import com.smim.backend.global.error.exception.CrawlingFailedException;
import com.smim.backend.global.error.exception.InvalidUrlException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 웹 페이지 크롤링 서비스
 * Jsoup을 이용하여 웹 페이지의 제목, 본문, 도메인을 추출합니다.
 */
@Slf4j
@Service
public class CrawlingService {

    private static final int TIMEOUT_MS = 10000; // 10초
    private static final int MAX_CONTENT_LENGTH = 100000; // 최대 본문 길이 100,000자
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; SMIM-Crawler/1.0)";

    /**
     * URL 유효성 검증
     * @param urlString 검증할 URL 문자열
     * @throws InvalidUrlException URL이 유효하지 않을 경우
     */
    public void validateUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            String host = url.getHost();

            // HTTP/HTTPS 프로토콜만 허용
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new InvalidUrlException("HTTP 또는 HTTPS 프로토콜만 지원합니다.");
            }

            // localhost 및 내부 네트워크 차단 (SSRF 방지)
            if (host.equals("localhost") || host.equals("127.0.0.1") ||
                host.startsWith("192.168.") || host.startsWith("10.") ||
                host.startsWith("172.16.") || host.equals("0.0.0.0")) {
                throw new InvalidUrlException("내부 네트워크 주소는 크롤링할 수 없습니다.");
            }

        } catch (MalformedURLException e) {
            throw new InvalidUrlException("올바른 URL 형식이 아닙니다.", e);
        }
    }

    /**
     * 웹 페이지 크롤링
     * @param url 크롤링할 URL
     * @return 크롤링된 아티클 데이터 (제목, 본문, 도메인)
     * @throws CrawlingFailedException 크롤링 실패 시
     */
    public CrawledArticle crawl(String url) {
        validateUrl(url);

        try {
            log.info("크롤링 시작: {}", url);

            // Jsoup으로 HTML 파싱
            Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();

            // 제목 추출
            String title = extractTitle(doc);
            if (title == null || title.isBlank()) {
                throw new CrawlingFailedException("페이지 제목을 찾을 수 없습니다.");
            }

            // 본문 추출
            String content = extractContent(doc);
            if (content == null || content.isBlank()) {
                throw new CrawlingFailedException("페이지 본문을 찾을 수 없습니다.");
            }

            // 본문 길이 제한
            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH);
                log.warn("본문이 최대 길이를 초과하여 잘렸습니다: {}", url);
            }

            // 도메인 추출
            String domain = new URL(url).getHost();

            log.info("크롤링 완료: {} (제목: {}, 본문 길이: {})", url, title, content.length());

            return new CrawledArticle(title, content, domain);

        } catch (IOException e) {
            log.error("크롤링 실패: {}", url, e);
            throw new CrawlingFailedException("웹 페이지를 가져올 수 없습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 제목 추출
     * 우선순위: <title> 태그 → meta og:title
     */
    private String extractTitle(Document doc) {
        String title = doc.title();

        if (title == null || title.isBlank()) {
            // meta og:title 시도
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null) {
                title = ogTitle.attr("content");
            }
        }

        return title != null ? title.trim() : null;
    }

    /**
     * 본문 추출
     * 우선순위: <article> 태그 → <main> 태그 → 모든 <p> 태그
     */
    private String extractContent(Document doc) {
        String content = null;

        // 전략 1: <article> 태그
        Element article = doc.selectFirst("article");
        if (article != null) {
            content = article.text();
        }

        // 전략 2: <main> 태그
        if (content == null || content.isBlank()) {
            Element main = doc.selectFirst("main");
            if (main != null) {
                content = main.text();
            }
        }

        // 전략 3: 모든 <p> 태그
        if (content == null || content.isBlank()) {
            Elements paragraphs = doc.select("p");
            if (!paragraphs.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Element p : paragraphs) {
                    sb.append(p.text()).append(" ");
                }
                content = sb.toString();
            }
        }

        return content != null ? content.trim() : null;
    }

    /**
     * 크롤링된 아티클 데이터
     */
    @Getter
    @AllArgsConstructor
    public static class CrawledArticle {
        private String title;
        private String content;
        private String domain;
    }
}
