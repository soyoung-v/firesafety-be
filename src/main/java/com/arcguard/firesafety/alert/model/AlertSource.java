package com.arcguard.firesafety.alert.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertSource {

    DEVICE("하드웨어"),
    AI("AI분석"),
    SYSTEM("시스템");

    private final String label;
}
