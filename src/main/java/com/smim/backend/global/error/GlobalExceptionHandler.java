package com.smim.backend.global.error;

import com.smim.backend.global.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    // TODO: 커스텀 예외(BusinessException) 추가 시 핸들러 구현 필요
    /*
     * @ExceptionHandler(BusinessException.class)
     * public ResponseEntity<ApiResponse<Void>>
     * handleBusinessException(BusinessException e) {
     * log.warn("Business Exception: {}", e.getMessage());
     * return ResponseEntity
     * .status(e.getErrorCode().getHttpStatus())
     * .body(ApiResponse.fail(e.getErrorCode()));
     * }
     */
}
