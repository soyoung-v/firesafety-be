package com.arcguard.firesafety.inspection.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "점검 체크리스트 저장 요청 (REQ-511)")
public class InspectionSaveReq {

    @Schema(description = "점검 일시", example = "2026-07-29T10:00:00")
    private LocalDateTime inspectedAt;
    @Schema(description = "항목별 결과 목록")
    private List<InspectionResultItemReq> results;
    @Schema(description = "특이사항(선택)", example = "3번 회로 접점 마모 확인됨")
    private String note;
}
