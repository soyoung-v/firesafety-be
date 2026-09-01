package com.arcguard.firesafety.facility.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityAuditTargetType {

    SITE("현장"),
    PANEL("분전반"),
    CIRCUIT("회로");

    private final String label;
}
