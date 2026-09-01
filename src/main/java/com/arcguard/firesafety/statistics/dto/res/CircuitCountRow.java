package com.arcguard.firesafety.statistics.dto.res;

import lombok.Getter;
import lombok.Setter;

// 회로 진단 커버리지 집계 SQL 1행(내부용) — StatisticsService가 StatisticsDiagnosisRes로 조립한다
@Getter
@Setter
public class CircuitCountRow {

    private long totalCircuitCount;
    private long diagnosedCircuitCount;
}
