package com.arcguard.firesafety.statistics.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "기간별 통계 조회 조건")
public class StatisticsReq {

    @Schema(description = "현장 ID(선택). ADMIN/GENERAL은 담당 현장만 조회 가능", example = "1")
    private Long siteId;

    @Schema(description = "조회 시작일(선택)", example = "2026-07-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @Schema(description = "조회 종료일(선택). 해당 날짜 하루 전체 포함", example = "2026-07-23")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
}
