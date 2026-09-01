package com.arcguard.firesafety.inspection.dto.res;

import com.arcguard.firesafety.inspection.model.InspectionResultType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "점검 이력 상세의 항목별 결과")
public class InspectionResultItemRes {

    @Schema(description = "점검 항목 ID", example = "1")
    private Long itemId;
    @Schema(description = "항목명", example = "누전차단기 동작 확인")
    private String itemName;
    @Schema(description = "결과", example = "NORMAL")
    private InspectionResultType result;
}
