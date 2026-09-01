package com.arcguard.firesafety.alert.dto.res;

import com.arcguard.firesafety.alert.model.AlertStatus;
import com.arcguard.firesafety.alert.model.AlertType;
import com.arcguard.firesafety.alert.model.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "미처리 조치 목록 항목 (REQ-306)")
public class AlertPendingRes {

    @Schema(description = "경보 ID", example = "1")
    private Long alertId;
    @Schema(description = "분전반 ID", example = "1")
    private Long panelId;
    @Schema(description = "분전반 이름", example = "분전반1")
    private String panelName;
    @Schema(description = "경보 유형", example = "ARC")
    private AlertType type;
    @Schema(description = "경보 심각도(CAUTION/RISK)", example = "RISK")
    private AlertSeverity severity;
    @Schema(description = "상태(UNCONFIRMED/CONFIRMED만 존재)", example = "UNCONFIRMED")
    private AlertStatus status;
    @Schema(description = "발생 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime triggeredAt;
    @Schema(description = "발생 후 경과 시간(시간 단위, DB 서버 시각 기준 계산)", example = "26")
    private Long elapsedHours;
}
