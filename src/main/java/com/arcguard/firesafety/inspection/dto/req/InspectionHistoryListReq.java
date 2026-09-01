package com.arcguard.firesafety.inspection.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "점검 이력 조회 조건 (REQ-512)")
public class InspectionHistoryListReq {

    @Schema(description = "조회 시작일(선택)", example = "2026-07-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @Schema(description = "조회 종료일(선택)", example = "2026-07-29")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
    @Schema(description = "페이지 번호. 0부터 시작(선택)", example = "0")
    private Integer page;
    @Schema(description = "페이지 크기(선택)", example = "20")
    private Integer size;
}
