package com.arcguard.firesafety.auth.dto.res;

import com.arcguard.firesafety.auth.model.UserAuditAction;
import com.arcguard.firesafety.auth.model.UserAuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "사용자 변경 감사 로그 항목")
public class UserAuditLogRes {

    @Schema(description = "감사 로그 ID", example = "1")
    private Long auditId;
    @Schema(description = "변경 대상 사용자 ID", example = "10")
    private Long targetUserId;
    @Schema(description = "변경을 수행한 사용자 ID (비인증 흐름이면 null)", example = "1")
    private Long actorUserId;
    @Schema(description = "변경 종류(CREATE/UPDATE/DELETE/RESTORE/PASSWORD_RESET)", example = "DELETE")
    private UserAuditAction action;
    @Schema(description = "변경 종류 한글 라벨", example = "삭제")
    private String actionLabel;
    @Schema(description = "변경 전 데이터(비밀번호/토큰 값 제외)")
    private Object beforeData;
    @Schema(description = "변경 후 데이터(비밀번호/토큰 값 제외)")
    private Object afterData;
    @Schema(description = "기록 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime createdAt;

    public static UserAuditLogRes from(UserAuditLog auditLog, Object beforeData, Object afterData) {
        return new UserAuditLogRes(
                auditLog.getAuditId(),
                auditLog.getTargetUserId(),
                auditLog.getActorUserId(),
                auditLog.getAction(),
                auditLog.getAction().getLabel(),
                beforeData,
                afterData,
                auditLog.getCreatedAt()
        );
    }
}
