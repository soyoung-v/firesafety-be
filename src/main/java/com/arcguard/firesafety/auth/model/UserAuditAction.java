package com.arcguard.firesafety.auth.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserAuditAction {

    CREATE("생성"),
    UPDATE("수정"),
    DELETE("삭제"),
    RESTORE("복구"),
    PASSWORD_RESET("비밀번호변경");

    private final String label;
}
