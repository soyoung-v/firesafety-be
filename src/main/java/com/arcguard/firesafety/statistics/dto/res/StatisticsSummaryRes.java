package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Schema(description = "기간별 통계 조회 응답")
public class StatisticsSummaryRes {

    @Schema(description = "조회 시작일(요청값 그대로)", example = "2026-07-01")
    private LocalDate from;
    @Schema(description = "조회 종료일(요청값 그대로)", example = "2026-07-23")
    private LocalDate to;
    @Schema(description = "조회 현장 ID(전체 조회면 null)", example = "1")
    private Long siteId;
    @Schema(description = "경보 통계")
    private StatisticsAlertRes alerts;
    @Schema(description = "AI 진단 통계")
    private StatisticsDiagnosisRes diagnoses;
    @Schema(description = "분전반 상태 통계")
    private StatisticsPanelRes panels;
    @Schema(description = "점검 현황 통계")
    private StatisticsInspectionRes inspections;
}
