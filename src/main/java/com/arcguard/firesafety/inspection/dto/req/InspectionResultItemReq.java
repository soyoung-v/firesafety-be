package com.arcguard.firesafety.inspection.dto.req;

import com.arcguard.firesafety.inspection.model.InspectionResultType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "체크리스트 저장 시 항목 1개의 결과")
public class InspectionResultItemReq {

    @Schema(description = "점검 항목 ID", example = "1")
    private Long itemId;
    @Schema(description = "결과. NORMAL=정상, ABNORMAL=이상, UNCHECKED=미확인", example = "NORMAL")
    private InspectionResultType result;
}
