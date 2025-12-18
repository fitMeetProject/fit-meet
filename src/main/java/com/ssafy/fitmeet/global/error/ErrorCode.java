package com.ssafy.fitmeet.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 0번대: 성공
    SUCCESS(0, HttpStatus.OK, "성공"),

    // 1000번대: 인증/인가 관련
    AUTH_INVALID_TOKEN(1001, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_EXPIRED_TOKEN(1002, HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    AUTH_ACCESS_DENIED(1003, HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    AUTH_BAD_CREDENTIALS(1004, HttpStatus.UNAUTHORIZED, "아이디 혹은 비밀번호가 틀렸습니다."),
    AUTH_INVALID_REFRESH_TOKEN(1005, HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
    AUTH_NON_SAVED_REFRESH_TOKEN(1006, HttpStatus.UNAUTHORIZED, "저장된 리프레시 토큰 정보가 없습니다."),
    AUTH_INVALID_PASSWORD(1012, HttpStatus.BAD_REQUEST, "비밀번호 형식이 맞지 않습니다."),

    AUTH_PASSWORD_RESET_TOKEN_INVALID(1007, HttpStatus.BAD_REQUEST, "비밀번호 재설정 토큰이 유효하지 않습니다."),
    AUTH_PASSWORD_RESET_TOKEN_EXPIRED(1008, HttpStatus.BAD_REQUEST, "비밀번호 재설정 토큰이 만료되었습니다."),
    AUTH_PASSWORD_RESET_TOKEN_USED(1009, HttpStatus.BAD_REQUEST, "이미 사용된 비밀번호 재설정 토큰입니다."),
    AUTH_PASSWORD_RESET_EMAIL_NOT_FOUND(1010, HttpStatus.BAD_REQUEST, "해당 이메일을 가진 계정을 찾을 수 없습니다."),
    AUTH_PASSWORD_RESET_FAIL(1011, HttpStatus.BAD_REQUEST, "비밀번호 재설정에 실패했습니다."),

    // 2000번대: 유저 관련
    USER_NOT_FOUND(2001, HttpStatus.BAD_REQUEST, "유저를 찾을 수 없습니다."),
    USER_EMAIL_DUPLICATED(2002, HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),

    // 3000번대: 식단 관련
    DIET_NOT_FOUND(3001, HttpStatus.BAD_REQUEST, "식단을 찾을 수 없습니다."),
    MEAL_NOT_FOUND(3002, HttpStatus.BAD_REQUEST, "공공데이터에서 해당 식품을 찾을 수 없습니다"),
    MEAL_NOT_FOUND2(3003, HttpStatus.BAD_REQUEST, "공공데이터 영양정보를 가져올 수 없습니다."),
    DIET_FOOD_CODE_REQUIRED(3004, HttpStatus.BAD_REQUEST, "API/IMAGE 모드에서는 foodCode가 필요합니다."),

    DIET_PREVIEW_FOODS_REQUIRED(3005, HttpStatus.BAD_REQUEST, "미리보기 foods는 비어 있을 수 없습니다."),
    DIET_INTAKE_GRAM_REQUIRED(3006, HttpStatus.BAD_REQUEST, "섭취량(intakeGram)은 0보다 커야 합니다."),

    // 4000번대: 파일 업로드 관련
    FILE_EMPTY(4001, HttpStatus.BAD_REQUEST, "업로드된 파일이 없습니다."),
    FILE_INVALID_TYPE(4002, HttpStatus.BAD_REQUEST, "허용되지 않은 파일 형식입니다."),
    FILE_UPLOAD_FAIL(4003, HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_STORAGE_ERROR(4004, HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."),

    // 5000번대: 커뮤니티 관련
    POST_NOT_FOUND(5001, HttpStatus.BAD_REQUEST, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(5002, HttpStatus.BAD_REQUEST, "댓글을 찾을 수 없습니다."),
    TAG_NOT_FOUND(5003, HttpStatus.BAD_REQUEST, "태그를 찾을 수 없습니다."),
    LIKE_ALREADY_EXISTS(5004, HttpStatus.BAD_REQUEST, "이미 좋아요한 게시글입니다."),
    LIKE_NOT_FOUND(5005, HttpStatus.BAD_REQUEST, "좋아요 정보가 없습니다."),

    // 6000번대 : 운동영상 관련
    RESOURCE_NOT_FOUND(6001, HttpStatus.BAD_REQUEST, "영상 정보가 없습니다."),

    // 7000번대: AI / 외부 API
    AI_RATE_LIMITED(7001, HttpStatus.TOO_MANY_REQUESTS, "AI 요청이 많아 잠시 후 다시 시도해주세요."),
    AI_TIMEOUT(7002, HttpStatus.GATEWAY_TIMEOUT, "AI 응답 시간이 초과되었습니다."),
    AI_UPSTREAM_AUTH_FAILED(7003, HttpStatus.BAD_GATEWAY, "AI 연동 인증에 실패했습니다."),
    AI_INVALID_REQUEST(7004, HttpStatus.BAD_REQUEST, "AI 요청 형식이 올바르지 않습니다."),
    AI_INVALID_RESPONSE(7005, HttpStatus.BAD_GATEWAY, "AI 응답 형식이 올바르지 않습니다."),
    AI_UPSTREAM_ERROR(7006, HttpStatus.BAD_GATEWAY, "AI 서버 오류가 발생했습니다."),

    // 8000번대: DB 관련
    DB_DUPLICATE_KEY(8001, HttpStatus.CONFLICT, "이미 존재하는 데이터입니다."),
    DB_ERROR(8002, HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류가 발생했습니다."),

    // 9000번대: 서버/기타
    INTERNAL_SERVER_ERROR(9000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
