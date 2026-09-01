package com.arcguard.firesafety.alert.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "경보 일괄 확인/조치완료 요청")
public class AlertBulkActionReq {

    @Schema(description = "대상 경보 ID 목록", example = "[1, 2, 3]")
    private List<Long> alertIds;
}
