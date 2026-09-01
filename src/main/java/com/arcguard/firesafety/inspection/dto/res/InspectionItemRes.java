package com.arcguard.firesafety.inspection.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "점검 항목 조회 응답 — 현장 카탈로그 조회(GET /sites/{siteId}/inspection-items)와 분전반 적용 항목 조회(GET /panels/{panelId}/inspection-items) 공용")
public class InspectionItemRes {

    @Schema(description = "항목 ID", example = "1")
    private Long itemId;
    @Schema(description = "현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "항목명", example = "누전차단기 동작 확인")
    private String itemName;
    @Schema(description = "설명", example = "테스트 버튼으로 정상 차단되는지 확인")
    private String description;
}
