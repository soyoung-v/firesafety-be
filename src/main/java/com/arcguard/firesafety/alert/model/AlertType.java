package com.arcguard.firesafety.alert.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertType {

    ARC("아크"),
    OVERHEAT("과열"),
    LEAKAGE("누전"),
    OVERCURRENT("과전류"),
    HUMIDITY("습도"),
    GAS("가스"),
    FIRE("불꽃"),
    DOOR_OPEN("문열림"),
    DEVICE_ERROR("장비오류"),
    COMM_LOST("통신두절");

    private final String label;
}
