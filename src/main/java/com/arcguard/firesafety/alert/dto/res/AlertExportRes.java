package com.arcguard.firesafety.alert.dto.res;

import com.arcguard.firesafety.alert.model.AlertSource;
import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import com.arcguard.firesafety.alert.model.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "경보 이력 엑셀 다운로드 한 행")
public class AlertExportRes {

    @Schema(description = "경보 ID", example = "1")
    private Long alertId;
    @Schema(description = "현장 이름", example = "아크타워1")
    private String siteName;
    @Schema(description = "분전반 이름", example = "분전반1")
    private String panelName;
    @Schema(description = "회로 번호(장비 단위 경보는 null)", example = "1")
    private Integer circuitNo;
    @Schema(description = "경보 유형", example = "ARC")
    private AlertType type;
    @Schema(description = "경보 심각도(CAUTION/RISK)", example = "RISK")
    private AlertSeverity severity;
    @Schema(description = "발생 소스(DEVICE/AI/SYSTEM)", example = "DEVICE")
    private AlertSource source;
    @Schema(description = "상태(UNCONFIRMED/CONFIRMED/RESOLVED)", example = "RESOLVED")
    private AlertStatus status;
    @Schema(description = "발생 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime triggeredAt;
    @Schema(description = "확인 처리자 이름", example = "홍길동")
    private String confirmedByName;
    @Schema(description = "확인 처리 시각", example = "2026-07-23T14:35:00")
    private LocalDateTime confirmedAt;
    @Schema(description = "조치완료 시각", example = "2026-07-23T15:00:00")
    private LocalDateTime resolvedAt;
    @Schema(description = "조치완료 처리자 이름", example = "홍길동")
    private String resolvedByName;
    @Schema(description = "조치 메모(비고)", example = "케이블 재접속")
    private String resolutionNote;
}
