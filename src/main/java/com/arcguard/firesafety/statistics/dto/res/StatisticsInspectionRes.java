package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "점검 현황 통계. 활성 분전반 기준, 기간 내 점검 이력으로 집계")
public class StatisticsInspectionRes {

    @Schema(description = "활성 분전반 전체 수", example = "10")
    private long totalPanelCount;
    @Schema(description = "기간 내 점검을 1회 이상 수행한 분전반 수", example = "6")
    private long inspectedPanelCount;
    @Schema(description = "기간 내 점검을 한 번도 안 한 분전반 수", example = "4")
    private long uninspectedPanelCount;
    @Schema(description = "기간 내 전체 점검 이력 건수(분전반 합산)", example = "15")
    private long totalInspectionCount;
    @Schema(description = "최근 점검 이력 최대 5건(점검일시 최신순)")
    private List<RecentInspectionRes> recentInspections;
}
