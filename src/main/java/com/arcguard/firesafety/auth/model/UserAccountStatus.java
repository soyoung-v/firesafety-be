package com.arcguard.firesafety.auth.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserAccountStatus {

    ACTIVE("활성"),
    DELETED("삭제");

    private final String label;
}
