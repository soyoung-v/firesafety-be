package com.arcguard.firesafety.alert.dto.req;

import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Schema(description = "경보 목록/엑셀 다운로드 조회 조건")
public class AlertListReq {

    @Schema(description = "상태 필터(선택). UNCONFIRMED=미확인, CONFIRMED=확인됨, RESOLVED=조치됨", example = "UNCONFIRMED")
    private AlertStatus status;
    @Schema(description = "유형 필터(선택). ARC/OVERHEAT/LEAKAGE/OVERCURRENT/HUMIDITY/GAS/FIRE/DOOR_OPEN/DEVICE_ERROR/COMM_LOST", example = "ARC")
    private AlertType type;
    @Schema(description = "현장 ID 필터(선택)", example = "1")
    private Long siteId;
    @Schema(description = "분전반 ID 필터(선택). 위험 팝업 '상세보기' 진입 시 해당 분전반의 최신 미확인 경보를 찾는 용도", example = "1")
    private Long panelId;

    @Schema(description = "조회 시작일(선택)", example = "2026-07-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @Schema(description = "조회 종료일(선택)", example = "2026-07-23")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    @Schema(description = "페이지 번호. 0부터 시작(선택)", example = "0")
    private Integer page;
    @Schema(description = "페이지 크기(선택)", example = "20")
    private Integer size;
    @Schema(description = "엑셀 다운로드에서 특정 경보만 선택 출력할 때 쓰는 ID 목록(선택). 없으면 필터 조건 전체 출력", example = "[1, 2, 3]")
    private List<Long> alertIds;
}
