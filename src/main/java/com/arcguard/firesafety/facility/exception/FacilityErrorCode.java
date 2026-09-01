package com.arcguard.firesafety.facility.exception;

import com.arcguard.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FacilityErrorCode implements ErrorCode {

    FORBIDDEN_ROLE("FACILITY-001", "권한이 없습니다", HttpStatus.FORBIDDEN),
    SITE_NOT_FOUND("FACILITY-002", "현장을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    PANEL_NOT_FOUND("FACILITY-003", "분전반을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    DUPLICATED_DEVICE_SERIAL("FACILITY-004", "이미 등록된 장비 시리얼입니다", HttpStatus.CONFLICT),
    INVALID_CIRCUIT_COUNT("FACILITY-005", "회로 개수는 1~10 사이여야 합니다", HttpStatus.BAD_REQUEST),
    CIRCUIT_NOT_FOUND("FACILITY-006", "회로를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INVALID_CHANNEL_NO("FACILITY-007", "회로 번호가 분전반 회로 범위를 벗어났습니다", HttpStatus.BAD_REQUEST),
    DUPLICATED_CHANNEL_NO("FACILITY-008", "이미 등록된 회로 번호입니다", HttpStatus.CONFLICT),
    INVALID_M_NO("FACILITY-009", "장비번호는 정확히 5자리여야 합니다", HttpStatus.BAD_REQUEST),
    SITE_NAME_REQUIRED("FACILITY-010", "현장 이름을 입력해주세요", HttpStatus.BAD_REQUEST),
    PANEL_NAME_REQUIRED("FACILITY-011", "분전반 이름을 입력해주세요", HttpStatus.BAD_REQUEST),
    DEVICE_SERIAL_REQUIRED("FACILITY-012", "장비 시리얼을 입력해주세요", HttpStatus.BAD_REQUEST),
    CHANNEL_NO_REQUIRED("FACILITY-013", "회로 번호를 입력해주세요", HttpStatus.BAD_REQUEST),
    LOAD_TYPE_TOO_LONG("FACILITY-014", "부하 종류는 50자 이하로 입력해주세요", HttpStatus.BAD_REQUEST),
    SITE_IDS_REQUIRED("FACILITY-015", "담당 현장 목록 값이 없습니다", HttpStatus.BAD_REQUEST),
    INVALID_SITE_ID("FACILITY-016", "현장 ID 값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    DUPLICATED_SITE_ID("FACILITY-017", "중복된 현장이 포함되어 있습니다", HttpStatus.BAD_REQUEST),
    ADDRESS_SEARCH_KEYWORD_REQUIRED("FACILITY-018", "검색어를 입력해주세요", HttpStatus.BAD_REQUEST),
    EXTERNAL_ADDRESS_API_ERROR("FACILITY-019", "주소 검색 중 오류가 발생했습니다", HttpStatus.SERVICE_UNAVAILABLE),
    SITE_ADDRESS_REQUIRED("FACILITY-020", "현장 주소를 입력해주세요", HttpStatus.BAD_REQUEST),
    DUPLICATED_SITE_NAME("FACILITY-021", "이미 등록된 현장 이름입니다", HttpStatus.CONFLICT),
    DUPLICATED_M_NO("FACILITY-022", "이미 등록된 장비번호입니다", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
