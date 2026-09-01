package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "분전반 상태 통계. 활성 설비 기준으로 집계")
public class StatisticsPanelRes {

    @Schema(description = "활성 분전반 전체 수", example = "10")
    private long totalCount;
    @Schema(description = "상태별(NORMAL/CAUTION/RISK/OFFLINE) 개수")
    private List<StatisticsCountRes> statusCounts;
}
