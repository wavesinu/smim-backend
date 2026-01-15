package com.smim.backend.global.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements ResultCode {

    // 공통
    OK("200", "요청이 성공적으로 처리되었습니다.", HttpStatus.OK),
    CREATED("201", "리소스가 성공적으로 생성되었습니다.", HttpStatus.CREATED),

    // 인증
    LOGIN_SUCCESS("200", "로그인에 성공했습니다.", HttpStatus.OK),
    LOGOUT_SUCCESS("200", "로그아웃에 성공했습니다.", HttpStatus.OK),
    TOKEN_REFRESHED("200", "토큰이 갱신되었습니다.", HttpStatus.OK),

    // 사용자
    USER_FOUND("200", "사용자 조회에 성공했습니다.", HttpStatus.OK),
    USER_UPDATED("200", "사용자 정보가 수정되었습니다.", HttpStatus.OK),
    USER_DELETED("200", "사용자가 삭제되었습니다.", HttpStatus.OK);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
