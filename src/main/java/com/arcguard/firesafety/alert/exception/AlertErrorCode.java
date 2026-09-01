package com.arcguard.firesafety.alert.exception;

import com.arcguard.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AlertErrorCode implements ErrorCode {

    ALERT_NOT_FOUND("ALERT-001", "경보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ALERT_CANNOT_CONFIRM("ALERT-002", "확인할 수 없는 경보 상태입니다", HttpStatus.CONFLICT),
    ALERT_NOT_CONFIRMED("ALERT-003", "확인 처리 후 조치 가능합니다", HttpStatus.CONFLICT),
    ALERT_EXPORT_FAILED("ALERT-004", "경보 이력 엑셀 파일을 생성할 수 없습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ALERT_ID("ALERT-005", "경보 ID 값이 올바르지 않습니다", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
