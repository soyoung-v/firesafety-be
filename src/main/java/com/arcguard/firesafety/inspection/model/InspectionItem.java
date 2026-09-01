package com.arcguard.firesafety.inspection.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 현장 공용 점검 항목 카탈로그 정의 (inspection_item) — 분전반은 panel_inspection_item으로 골라 적용한다
@Getter
@Setter
public class InspectionItem {

    private Long itemId;
    private Long siteId;
    private String itemName;
    private String description;
    private LocalDateTime createdAt;
}
