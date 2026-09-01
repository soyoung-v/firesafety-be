package com.arcguard.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "통계 화면용 최근 점검 이력 1건")
public class RecentInspectionRes {

    @Schema(description = "분전반 ID", example = "10")
    private Long panelId;
    @Schema(description = "분전반명", example = "1층 분전반")
    private String panelName;
    @Schema(description = "점검일시", example = "2026-07-23T14:35:00")
    private LocalDateTime inspectedAt;
    @Schema(description = "점검자명", example = "홍길동")
    private String inspectorName;
}
