package com.arcguard.firesafety.monitoring.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "대시보드 요약 조회 조건")
public class DashboardSummaryReq {

    @Schema(description = "현장 ID(선택). ADMIN/GENERAL은 담당 현장이 아니면 거부됨. SUPER_ADMIN이 비워두면 전체 집계", example = "1")
    private Long siteId;
}
