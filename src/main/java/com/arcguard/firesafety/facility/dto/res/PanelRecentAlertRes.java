package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.alert.model.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "분전반 상세의 최근 경보 항목")
public class PanelRecentAlertRes {

    @Schema(description = "경보 ID", example = "25")
    private Long alertId;
    @Schema(description = "경보 유형", example = "OVERCURRENT")
    private AlertType type;
    @Schema(description = "발생 시각", example = "2026-07-24T00:33:00")
    private LocalDateTime triggeredAt;
}
