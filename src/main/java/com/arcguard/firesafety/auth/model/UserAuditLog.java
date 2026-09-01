package com.arcguard.firesafety.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAuditLog {

    private Long auditId;
    private Long targetUserId;
    private Long actorUserId;
    private UserAuditAction action;
    private String beforeData;
    private String afterData;
    private LocalDateTime createdAt;
}
