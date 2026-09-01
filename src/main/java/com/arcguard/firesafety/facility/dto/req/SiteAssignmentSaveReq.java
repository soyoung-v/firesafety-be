package com.arcguard.firesafety.facility.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "담당 현장 배정 저장 요청. 보낸 목록으로 배정 상태를 통째로 동기화(배정/해제/재배정)")
public class SiteAssignmentSaveReq {

    @Schema(description = "이 사용자가 담당할 현장 ID 목록. 목록에 없는 기존 배정은 해제됨", example = "[1, 2]")
    private List<Long> siteIds;
}
