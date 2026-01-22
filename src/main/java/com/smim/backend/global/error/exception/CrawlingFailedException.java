package com.smim.backend.global.error.exception;

/**
 * 웹 페이지 크롤링이 실패했을 때 발생하는 예외
 */
public class CrawlingFailedException extends RuntimeException {

    public CrawlingFailedException(String message) {
        super(message);
    }

    public CrawlingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
