package com.arcguard.firesafety.monitoring.dto.res;

import com.arcguard.firesafety.facility.model.PanelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "대시보드 분전반 목록 항목. OFFLINE→RISK→CAUTION→NORMAL 순으로 정렬됨")
public class DashboardPanelRes {

    @Schema(description = "분전반 ID", example = "1")
    private Long panelId;
    @Schema(description = "소속 현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "현장 이름", example = "아크타워1")
    private String siteName;
    @Schema(description = "분전반 이름", example = "분전반1")
    private String name;
    @Schema(description = "상태(NORMAL/CAUTION/RISK/OFFLINE)", example = "RISK")
    private PanelStatus status;
    @Schema(description = "마지막 수신 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime lastCommunicatedAt;
    @Schema(description = "미확인 경보 개수", example = "2")
    private Long unconfirmedAlertCount;
}
