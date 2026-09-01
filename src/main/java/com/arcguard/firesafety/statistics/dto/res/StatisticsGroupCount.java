package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "항목별 집계 1건(키+개수만)")
public class StatisticsGroupCount {

    @Schema(description = "항목 키", example = "ARC")
    private String key;
    @Schema(description = "개수", example = "5")
    private long count;
}
