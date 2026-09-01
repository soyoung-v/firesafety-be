package com.arcguard.firesafety.inspection.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 점검 1회 실행 단위 (inspection_result)
@Getter
@Setter
public class InspectionResult {

    private Long inspectionId;
    private Long panelId;
    private LocalDateTime inspectedAt;
    private Long inspectorId;
    private String note;
    private LocalDateTime createdAt;
}
