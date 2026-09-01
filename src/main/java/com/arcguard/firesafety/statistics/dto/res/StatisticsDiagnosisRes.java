package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "AI 진단 통계")
public class StatisticsDiagnosisRes {

    @Schema(description = "조회 기간 내 전체 AI 판정 수", example = "120")
    private long totalCount;
    @Schema(description = "판정별(NORMAL/ARC) 개수")
    private List<StatisticsCountRes> verdictCounts;
    @Schema(description = "현장 내 전체 회로 수(진단 대상 모수)", example = "40")
    private long totalCircuitCount;
    @Schema(description = "조회 기간 내 1회 이상 진단된 회로 수", example = "18")
    private long diagnosedCircuitCount;
}
