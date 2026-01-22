package com.smim.backend.domain.article.service;

import com.smim.backend.global.error.exception.InvalidUrlException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlingService 테스트")
class CrawlingServiceTest {

    @InjectMocks
    private CrawlingService crawlingService;

    @Test
    @DisplayName("유효한 HTTP URL 검증 성공")
    void validateUrl_ValidHttpUrl_Success() {
        // given
        String validUrl = "http://example.com/article";

        // when & then
        assertDoesNotThrow(() -> crawlingService.validateUrl(validUrl));
    }

    @Test
    @DisplayName("유효한 HTTPS URL 검증 성공")
    void validateUrl_ValidHttpsUrl_Success() {
        // given
        String validUrl = "https://example.com/article";

        // when & then
        assertDoesNotThrow(() -> crawlingService.validateUrl(validUrl));
    }

    @Test
    @DisplayName("잘못된 URL 형식 - 예외 발생")
    void validateUrl_MalformedUrl_ThrowsException() {
        // given
        String malformedUrl = "not-a-valid-url";

        // when & then
        assertThatThrownBy(() -> crawlingService.validateUrl(malformedUrl))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("올바른 URL 형식이 아닙니다");
    }

    @Test
    @DisplayName("FTP 프로토콜 - 예외 발생")
    void validateUrl_FtpProtocol_ThrowsException() {
        // given
        String ftpUrl = "ftp://example.com/file";

        // when & then
        assertThatThrownBy(() -> crawlingService.validateUrl(ftpUrl))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("HTTP 또는 HTTPS 프로토콜만 지원합니다");
    }

    @Test
    @DisplayName("localhost 주소 - 예외 발생 (SSRF 방지)")
    void validateUrl_LocalhostUrl_ThrowsException() {
        // given
        String localhostUrl = "http://localhost:8080/api";

        // when & then
        assertThatThrownBy(() -> crawlingService.validateUrl(localhostUrl))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("내부 네트워크 주소는 크롤링할 수 없습니다");
    }

    @Test
    @DisplayName("127.0.0.1 주소 - 예외 발생 (SSRF 방지)")
    void validateUrl_LoopbackUrl_ThrowsException() {
        // given
        String loopbackUrl = "http://127.0.0.1/api";

        // when & then
        assertThatThrownBy(() -> crawlingService.validateUrl(loopbackUrl))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("내부 네트워크 주소는 크롤링할 수 없습니다");
    }

    @Test
    @DisplayName("192.168.x.x 주소 - 예외 발생 (SSRF 방지)")
    void validateUrl_PrivateNetworkUrl_ThrowsException() {
        // given
        String privateUrl = "http://192.168.1.1/admin";

        // when & then
        assertThatThrownBy(() -> crawlingService.validateUrl(privateUrl))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("내부 네트워크 주소는 크롤링할 수 없습니다");
    }

    @Test
    @DisplayName("10.x.x.x 주소 - 예외 발생 (SSRF 방지)")
    void validateUrl_PrivateNetworkClass10_ThrowsException() {
        // given
        String privateUrl = "http://10.0.0.1/api";

        // when & then
        assertThatThrownBy(() -> crawlingService.validateUrl(privateUrl))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("내부 네트워크 주소는 크롤링할 수 없습니다");
    }

    @Test
    @DisplayName("실제 BBC 뉴스 크롤링 테스트")
    void crawl_RealBbcNews_Success() {
        // given
        String bbcUrl = "https://www.bbc.com/news/articles/cgezx40r7d7o";

        // when
        CrawlingService.CrawledArticle result = crawlingService.crawl(bbcUrl);

        // then
        System.out.println("=== BBC 뉴스 크롤링 결과 ===");
        System.out.println("제목: " + result.getTitle());
        System.out.println("도메인: " + result.getDomain());
        System.out.println("본문 길이: " + result.getContent().length() + "자");
        System.out.println("본문 앞부분: " + result.getContent().substring(0, Math.min(200, result.getContent().length())));
        System.out.println("========================");

        assertDoesNotThrow(() -> crawlingService.crawl(bbcUrl));
    }
}
