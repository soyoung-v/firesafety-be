package com.arcguard.firesafety.diagnosis.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Verdict {

    NORMAL("정상"),
    ARC("아크");

    private final String label;
}
