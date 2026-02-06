package com.smim.backend.global.error;

import com.smim.backend.global.common.response.ApiResponse;
import com.smim.backend.global.error.exception.CrawlingFailedException;
import com.smim.backend.global.error.exception.InvalidUrlException;
import com.smim.backend.global.error.exception.BusinessException;
import com.smim.backend.global.error.exception.TokenValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
            .status(e.getErrorCode().getHttpStatus())
            .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

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
     * 토큰 검증 실패 처리
     */
    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenValidation(TokenValidationException e) {
        return ResponseEntity
            .status(ErrorCode.INVALID_TOKEN.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.INVALID_TOKEN, e.getMessage()));
    }

    /**
     * 검증 실패 처리 (Request Body)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().isEmpty()
                ? ErrorCode.VALIDATION_FAILED.getMessage()
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        return ResponseEntity
            .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, message));
    }

    /**
     * 검증 실패 처리 (Query Parameter 등)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity
            .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, e.getMessage()));
    }

    /**
     * 잘못된 요청 바디 처리 (Enum 역직렬화 실패 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity
            .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getMessage()));
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
