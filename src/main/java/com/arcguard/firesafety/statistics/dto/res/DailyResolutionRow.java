package com.arcguard.firesafety.statistics.dto.res;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 일자별 주의(CAUTION) 알림 발생/조치완료/예방성공 집계 SQL 1행(내부용) — StatisticsService가 DailyResolutionRateRes로 조립한다
@Getter
@Setter
public class DailyResolutionRow {

    private LocalDate date;
    private long totalCount;
    private long resolvedCount;
    // 조치완료 + 조치완료 후 24시간 내 같은 분전반·유형 RISK로 전환되지 않은 건수(예방 성공)
    private long preventedCount;
}
