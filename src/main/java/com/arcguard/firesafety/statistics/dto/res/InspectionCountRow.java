package com.arcguard.firesafety.statistics.dto.res;

import lombok.Getter;
import lombok.Setter;

// 점검 현황 집계 SQL 1행(내부용) — StatisticsService가 StatisticsInspectionRes로 조립한다
@Getter
@Setter
public class InspectionCountRow {

    private long totalPanelCount;
    private long inspectedPanelCount;
    private long totalInspectionCount;
}
