package com.arcguard.firesafety.auth.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    SUPER_ADMIN("플랫폼관리자"),
    ADMIN("현장관리자"),
    GENERAL("일반직원");

    private final String label;
}
