package com.arcguard.firesafety.inspection.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "점검 이력(페이지) (REQ-512)")
public class InspectionHistoryPageRes {

    @Schema(description = "이번 페이지 항목 목록")
    private List<InspectionHistoryRes> content;
    @Schema(description = "전체 항목 수", example = "12")
    private long totalElements;
}
