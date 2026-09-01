package com.arcguard.firesafety.alert.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertSeverity {

    CAUTION("주의"),
    RISK("위험");

    private final String label;
}
