package com.arcguard.firesafety.inspection.model;

import lombok.Getter;
import lombok.Setter;

// 점검 1회당 항목별 결과 (inspection_result_item)
@Getter
@Setter
public class InspectionResultItem {

    private Long resultId;
    private Long inspectionId;
    private Long itemId;
    private InspectionResultType result;
}
