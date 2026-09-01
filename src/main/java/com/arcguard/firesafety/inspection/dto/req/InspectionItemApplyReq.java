package com.arcguard.firesafety.inspection.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "분전반에 적용할 점검 항목 전체교체 요청 — 현재 선택 상태 전체를 이 목록으로 교체한다")
public class InspectionItemApplyReq {

    @Schema(description = "적용할 항목 ID 목록. 빈 배열이면 전체 해제")
    private List<Long> itemIds;
}
