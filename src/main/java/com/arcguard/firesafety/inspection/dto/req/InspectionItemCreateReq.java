package com.arcguard.firesafety.inspection.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "점검 항목 등록 요청 (REQ-511)")
public class InspectionItemCreateReq {

    @Schema(description = "항목명", example = "누전차단기 동작 확인")
    private String itemName;
    @Schema(description = "설명(선택)", example = "테스트 버튼으로 정상 차단되는지 확인")
    private String description;
}
