package com.arcguard.firesafety.inspection.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "점검 체크리스트 저장 응답")
public class InspectionSaveRes {

    @Schema(description = "생성된 점검 실행 ID", example = "1")
    private Long inspectionId;
}
