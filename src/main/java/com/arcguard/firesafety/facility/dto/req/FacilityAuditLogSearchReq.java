package com.arcguard.firesafety.facility.dto.req;

import com.arcguard.firesafety.facility.model.FacilityAuditAction;
import com.arcguard.firesafety.facility.model.FacilityAuditTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "설비 감사 이력 조회 조건. SUPER_ADMIN 전용")
public class FacilityAuditLogSearchReq {

    @Schema(description = "대상종류 필터(SITE/PANEL/CIRCUIT, 선택)", example = "PANEL")
    private FacilityAuditTargetType targetType;
    @Schema(description = "대상 ID 필터(선택). targetType에 따라 site_id/panel_id/circuit_id 중 하나", example = "1")
    private Long targetId;
    @Schema(description = "처리자(사용자) ID 필터(선택)", example = "1")
    private Long actorUserId;
    @Schema(description = "처리 종류 필터(CREATE/UPDATE/DELETE, 선택)", example = "CREATE")
    private FacilityAuditAction action;

    @Schema(description = "조회 시작일(선택)", example = "2026-07-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @Schema(description = "조회 종료일(선택)", example = "2026-07-23")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    @Schema(description = "페이지 번호. 0부터 시작(선택)", example = "0")
    private Integer page;
    @Schema(description = "페이지 크기(선택)", example = "20")
    private Integer size;
}
