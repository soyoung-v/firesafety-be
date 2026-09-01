package com.arcguard.firesafety.facility.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PanelStatus {

    NORMAL("정상"),
    CAUTION("주의"),
    RISK("위험"),
    OFFLINE("오프라인");

    private final String label;
}
