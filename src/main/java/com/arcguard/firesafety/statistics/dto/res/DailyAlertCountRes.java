package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "일자별 경보 발생 건수")
public class DailyAlertCountRes {

    @Schema(description = "날짜", example = "2026-07-23")
    private LocalDate date;
    @Schema(description = "해당 날짜 경보 발생 수", example = "5")
    private long count;
}
