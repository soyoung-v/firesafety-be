package com.arcguard.firesafety.system.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemReleaseType {

    SOFTWARE("소프트웨어"),
    MODEL("AI 모델");

    private final String label;
}
