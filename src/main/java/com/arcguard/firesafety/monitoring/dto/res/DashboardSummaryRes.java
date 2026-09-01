package com.arcguard.firesafety.monitoring.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "대시보드 요약")
public class DashboardSummaryRes {

    @Schema(description = "전체 분전반 수", example = "10")
    private long totalPanelCount;
    @Schema(description = "정상(NORMAL) 분전반 수", example = "7")
    private long normalPanelCount;
    @Schema(description = "주의(CAUTION) 분전반 수", example = "1")
    private long cautionPanelCount;
    @Schema(description = "위험(RISK) 분전반 수", example = "1")
    private long riskPanelCount;
    @Schema(description = "통신두절(OFFLINE) 분전반 수", example = "1")
    private long offlinePanelCount;
    @Schema(description = "미확인 경보 수", example = "3")
    private long unconfirmedAlertCount;
    @Schema(description = "미조치 경보 수", example = "5")
    private long unresolvedAlertCount;
    @Schema(description = "분전반 목록. OFFLINE→RISK→CAUTION→NORMAL 순 정렬")
    private List<DashboardPanelRes> panels;
}
