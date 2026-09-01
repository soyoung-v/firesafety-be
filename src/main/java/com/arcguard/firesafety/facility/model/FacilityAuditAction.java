package com.arcguard.firesafety.facility.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityAuditAction {

    CREATE("등록"),
    UPDATE("수정"),
    DELETE("삭제");

    private final String label;
}
