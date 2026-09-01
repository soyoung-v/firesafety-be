package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Schema(description = "일자별 예방조치 이행률 — 그날 발생한 주의(CAUTION) 알림 중 위험 전환 없이 예방 성공한 비율")
public class DailyResolutionRateRes {

    @Schema(description = "날짜", example = "2026-07-23")
    private LocalDate date;
    @Schema(description = "해당 날짜 발생 주의(CAUTION) 알림 수", example = "5")
    private long totalCount;
    @Schema(description = "그중 예방 성공(조치완료 + 24시간 내 위험 미전환) 수", example = "4")
    private long preventedCount;
    @Schema(description = "예방조치 이행률(0~100, 소수점 첫째자리 반올림). 발생 건수가 0이면 null", example = "80.0")
    private Double rate;
}
