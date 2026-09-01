package com.arcguard.firesafety.alert.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "미처리 조치 목록 조회 조건 (REQ-306) - 기간 필터 없이 UNCONFIRMED/CONFIRMED 상태만 대상")
public class AlertPendingReq {

    @Schema(description = "현장 ID 필터(선택)", example = "1")
    private Long siteId;
    @Schema(description = "지연 판단 기준 시간(선택, 기본 24시간). 이 시간 이상 경과한 건은 프론트가 지연 배지로 표시", example = "24")
    private Integer overdueHours;
    @Schema(description = "페이지 번호. 0부터 시작(선택)", example = "0")
    private Integer page;
    @Schema(description = "페이지 크기(선택)", example = "20")
    private Integer size;
}
