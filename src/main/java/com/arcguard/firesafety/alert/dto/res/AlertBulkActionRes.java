package com.arcguard.firesafety.alert.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "경보 일괄 확인/조치완료 결과")
public class AlertBulkActionRes {

    @Schema(description = "성공 건수", example = "18")
    private int successCount;
    @Schema(description = "실패 건수", example = "2")
    private int failCount;
    @Schema(description = "실패 사유 목록(중복 제거)", example = "[\"확인할 수 없는 경보 상태입니다\"]")
    private List<String> failureReasons;
}
