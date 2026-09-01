package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "항목별(상태/유형/소스 등) 집계 1건")
public class StatisticsCountRes {

    @Schema(description = "항목 키(enum 이름)", example = "ARC")
    private String key;
    @Schema(description = "항목 한글 라벨", example = "아크")
    private String label;
    @Schema(description = "개수", example = "5")
    private long count;
}
