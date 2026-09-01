package com.arcguard.firesafety.inspection.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "점검 이력 항목 (REQ-512)")
public class InspectionHistoryRes {

    @Schema(description = "점검 실행 ID", example = "1")
    private Long inspectionId;
    @Schema(description = "점검 일시", example = "2026-07-29T10:00:00")
    private LocalDateTime inspectedAt;
    @Schema(description = "점검자 이름", example = "홍길동")
    private String inspectorName;
    @Schema(description = "항목별 결과 목록")
    private List<InspectionResultItemRes> results;
    @Schema(description = "특이사항", example = "3번 회로 접점 마모 확인됨")
    private String note;
}
