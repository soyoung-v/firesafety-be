package com.arcguard.firesafety.diagnosis.model;

public enum DiagnosisTriggerType {
    AUTO("자동"),
    MANUAL("수동"),
    MOCK("데모"),
    UNKNOWN("알 수 없음");

    private final String label;

    DiagnosisTriggerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
