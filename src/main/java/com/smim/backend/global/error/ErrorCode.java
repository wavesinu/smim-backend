package com.smim.backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import com.smim.backend.global.common.response.ResultCode;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ResultCode {

    // 공통 에러 (CO: Common)
    INTERNAL_SERVER_ERROR("CO001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("CO002", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("CO003", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("CO004", "허용되지 않은 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    VALIDATION_FAILED("CO005", "입력값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),

    // 인증 에러 (AU: Auth)
    UNAUTHORIZED("AU001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AU002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AU003", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AU004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    OAUTH2_AUTHENTICATION_FAILED("AU005", "소셜 로그인에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_PASSWORD("AU006", "비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    SOCIAL_ACCOUNT_LOGIN_REQUIRED("AU007", "소셜 로그인으로 가입된 계정입니다.", HttpStatus.UNAUTHORIZED),

    // 사용자 에러 (US: User)
    USER_NOT_FOUND("US001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("US002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("US003", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    INVALID_USER_STATUS("US004", "유효하지 않은 사용자 상태입니다.", HttpStatus.BAD_REQUEST),

    // CEFR 에러 (CF: CEFR)
    INVALID_CEFR_LEVEL("CF001", "유효하지 않은 CEFR 등급입니다.", HttpStatus.BAD_REQUEST),

    // 알림 에러 (NF: Notification)
    KAKAO_NOT_LINKED("NF002", "카카오 계정이 연동되어 있지 않습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("NF003", "이메일 인증이 필요합니다.", HttpStatus.BAD_REQUEST),

    // 아티클 에러 (AR: Article)
    INVALID_URL("AR001", "유효하지 않은 URL입니다.", HttpStatus.BAD_REQUEST),
    CRAWLING_FAILED("AR002", "웹 페이지 크롤링에 실패했습니다.", HttpStatus.BAD_REQUEST),
    ARTICLE_NOT_FOUND("AR003", "아티클을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
