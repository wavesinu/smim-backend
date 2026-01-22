package com.smim.backend.global.error;

import com.smim.backend.global.common.response.ApiResponse;
import com.smim.backend.global.error.exception.CrawlingFailedException;
import com.smim.backend.global.error.exception.InvalidUrlException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * URL이 유효하지 않을 때 발생하는 예외 처리
     */
    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidUrl(InvalidUrlException e) {
        return ResponseEntity
            .status(ErrorCode.INVALID_URL.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.INVALID_URL, e.getMessage()));
    }

    /**
     * 크롤링이 실패했을 때 발생하는 예외 처리
     */
    @ExceptionHandler(CrawlingFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCrawlingFailed(CrawlingFailedException e) {
        return ResponseEntity
            .status(ErrorCode.CRAWLING_FAILED.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.CRAWLING_FAILED, e.getMessage()));
    }

    /**
     * 그 외 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
