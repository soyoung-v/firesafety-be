package com.arcguard.firesafety.facility.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "설비 감사 이력 목록(페이지)")
public class FacilityAuditLogPageRes {

    @Schema(description = "이번 페이지 항목 목록")
    private List<FacilityAuditLogRes> content;
    @Schema(description = "전체 항목 수", example = "42")
    private long totalElements;
    @Schema(description = "페이지 번호(0부터)", example = "0")
    private int page;
    @Schema(description = "페이지 크기", example = "20")
    private int size;
}
