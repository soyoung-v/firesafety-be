package com.arcguard.firesafety.facility.dto.req;

import com.arcguard.firesafety.facility.model.PanelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "분전반 목록 조회 조건")
public class PanelListReq {

    @Schema(description = "현장 ID 필터(선택). ADMIN/GENERAL은 담당 현장이 아니면 거부됨", example = "1")
    private Long siteId;
    @Schema(description = "상태 필터(선택). NORMAL/CAUTION/RISK/OFFLINE", example = "RISK")
    private PanelStatus status;
    @Schema(description = "분전반명 검색어(선택, 부분일치)", example = "1층")
    private String keyword;
    @Schema(description = "페이지 번호. 0부터 시작(선택)", example = "0")
    private Integer page;
    @Schema(description = "페이지 크기(선택)", example = "20")
    private Integer size;
}
