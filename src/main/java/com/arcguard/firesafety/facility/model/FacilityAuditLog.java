package com.arcguard.firesafety.facility.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacilityAuditLog {

    private Long auditId;
    private FacilityAuditTargetType targetType;
    private Long targetId;
    private Long actorUserId;
    private FacilityAuditAction action;
    private String beforeData;
    private String afterData;
    private LocalDateTime createdAt;
}
