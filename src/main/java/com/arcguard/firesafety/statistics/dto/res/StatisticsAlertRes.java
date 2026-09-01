package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "경보 통계")
public class StatisticsAlertRes {

    @Schema(description = "조회 기간 내 전체 경보 수", example = "42")
    private long totalCount;
    @Schema(description = "처리상태별(UNCONFIRMED/CONFIRMED/RESOLVED) 개수")
    private List<StatisticsCountRes> statusCounts;
    @Schema(description = "유형별(ARC/OVERHEAT 등) 개수")
    private List<StatisticsCountRes> typeCounts;
    @Schema(description = "소스별(DEVICE/AI/SYSTEM) 개수")
    private List<StatisticsCountRes> sourceCounts;
    @Schema(description = "일자별 경보 발생 수 목록")
    private List<DailyAlertCountRes> dailyCounts;
    @Schema(description = "조회 기간 내 주의(CAUTION) 알림 발생 수", example = "12")
    private long cautionAlertCount;
    @Schema(description = "조회 기간 내 주의(CAUTION) 알림 조치완료 수", example = "9")
    private long cautionResolvedCount;
    @Schema(description = "조치완료 후 24시간 안에 같은 분전반·유형이 위험(RISK)으로 전환된 수(예방 실패)", example = "1")
    private long cautionEscalatedCount;
    @Schema(description = "일자별 예방조치 이행률 추이 — 화면 목표선은 90%")
    private List<DailyResolutionRateRes> dailyResolutionRates;
}
