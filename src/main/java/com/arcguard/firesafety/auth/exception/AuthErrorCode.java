package com.arcguard.firesafety.auth.exception;

import com.arcguard.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_LOGIN("AUTH-001", "아이디 또는 비밀번호가 일치하지 않습니다", HttpStatus.UNAUTHORIZED),
    EXPIRED_AUTH("AUTH-002", "인증이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN_ROLE("AUTH-003", "권한이 없습니다", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND("AUTH-004", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    DUPLICATED_EMAIL("AUTH-005", "이미 사용 중인 이메일입니다", HttpStatus.CONFLICT),
    SELF_DELETE_NOT_ALLOWED("AUTH-006", "본인 계정은 삭제할 수 없습니다", HttpStatus.FORBIDDEN),
    USER_ALREADY_DELETED("AUTH-007", "이미 삭제된 사용자입니다", HttpStatus.CONFLICT),
    BULK_DELETE_EMPTY("AUTH-008", "삭제할 사용자를 선택해주세요", HttpStatus.BAD_REQUEST),
    BULK_DELETE_DUPLICATED("AUTH-009", "중복된 사용자가 포함되어 있습니다", HttpStatus.BAD_REQUEST),
    BULK_DELETE_FORBIDDEN_TARGET("AUTH-010", "삭제 권한이 없는 사용자가 포함되어 있습니다", HttpStatus.FORBIDDEN),
    USER_NOT_DELETED("AUTH-011", "삭제된 사용자만 복구할 수 있습니다", HttpStatus.CONFLICT),
    PASSWORD_RESET_TOKEN_INVALID("AUTH-012", "비밀번호 재설정 토큰이 유효하지 않습니다", HttpStatus.UNAUTHORIZED),
    PASSWORD_RESET_TOKEN_EXPIRED("AUTH-013", "비밀번호 재설정 토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    PASSWORD_RESET_RATE_LIMIT("AUTH-014", "비밀번호 재설정 요청이 너무 많습니다", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_EMAIL_FORMAT("AUTH-015", "이메일 형식이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_FORMAT("AUTH-016", "비밀번호는 공백 없이 영문과 숫자를 포함하여 8자 이상 입력해 주세요", HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_MAIL_SEND_FAILED("AUTH-017", "비밀번호 재설정 메일 발송에 실패했습니다", HttpStatus.SERVICE_UNAVAILABLE),
    EMAIL_REQUIRED("AUTH-018", "이메일을 입력해주세요", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED("AUTH-019", "비밀번호를 입력해주세요", HttpStatus.BAD_REQUEST),
    NAME_REQUIRED("AUTH-020", "이름을 입력해주세요", HttpStatus.BAD_REQUEST),
    ROLE_REQUIRED("AUTH-021", "역할을 선택해주세요", HttpStatus.BAD_REQUEST),
    FCM_TOKEN_REQUIRED("AUTH-022", "FCM 토큰 값이 없습니다", HttpStatus.BAD_REQUEST),
    BULK_DELETE_INVALID_ID("AUTH-023", "삭제 대상 ID 값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    EMAIL_UPDATE_FORBIDDEN("AUTH-024", "이메일은 슈퍼관리자만 변경할 수 있습니다", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
