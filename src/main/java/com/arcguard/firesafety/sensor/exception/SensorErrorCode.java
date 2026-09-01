package com.arcguard.firesafety.sensor.exception;

import com.arcguard.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SensorErrorCode implements ErrorCode {

    INVALID_FRAME_PARAMETER("SENSOR-001", "센서 데이터 형식이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    PANEL_NOT_FOUND("SENSOR-002", "장비번호에 해당하는 분전반을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    CIRCUIT_NOT_FOUND("SENSOR-003", "분전반에 등록된 회로 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
