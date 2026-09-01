package com.arcguard.firesafety.auth.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "사용자 일괄 소프트 삭제 결과")
public class UserBulkDeleteRes {

    @Schema(description = "실제로 삭제된 사용자 ID 목록", example = "[10, 11, 12]")
    private List<Long> deletedUserIds;

    public static UserBulkDeleteRes from(List<Long> deletedUserIds) {
        return new UserBulkDeleteRes(deletedUserIds);
    }
}
