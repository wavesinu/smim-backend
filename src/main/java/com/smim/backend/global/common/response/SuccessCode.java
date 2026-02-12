package com.smim.backend.global.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements ResultCode {

    // 공통 성공 코드 (API Specification 기준)
    OK("S001", "요청이 성공적으로 처리되었습니다.", HttpStatus.OK),
    CREATED("S001", "요청이 성공적으로 처리되었습니다.", HttpStatus.CREATED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
