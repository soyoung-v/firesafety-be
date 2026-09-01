package com.arcguard.firesafety.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 일괄 소프트 삭제 요청")
public class UserBulkDeleteReq {

    @Schema(description = "삭제할 사용자 ID 목록. 자기 자신/권한 밖 대상/이미 삭제된 사용자가 하나라도 있으면 전체 실패", example = "[10, 11, 12]")
    private List<Long> userIds;
}
