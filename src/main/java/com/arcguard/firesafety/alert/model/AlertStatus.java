package com.arcguard.firesafety.alert.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertStatus {

    UNCONFIRMED("미확인"),
    CONFIRMED("확인됨"),
    RESOLVED("조치됨");

    private final String label;
}
