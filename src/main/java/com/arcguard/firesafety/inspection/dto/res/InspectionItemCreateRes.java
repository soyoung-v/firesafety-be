package com.arcguard.firesafety.inspection.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "점검 항목 등록 응답")
public class InspectionItemCreateRes {

    @Schema(description = "생성된 항목 ID", example = "1")
    private Long itemId;
}
