package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.UserSiteAssignment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "담당 현장 배정 항목")
public class SiteAssignmentRes {

    @Schema(description = "배정 매핑 ID", example = "1")
    private Long mappingId;
    @Schema(description = "사용자 ID", example = "10")
    private Long userId;
    @Schema(description = "배정된 현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "현장 이름", example = "아크타워1")
    private String siteName;
    @Schema(description = "현장 주소", example = "서울시 강남구")
    private String siteAddress;
    @Schema(description = "배정 시각", example = "2026-07-23T10:00:00")
    private LocalDateTime assignedAt;

    public static SiteAssignmentRes from(UserSiteAssignment assignment) {
        return new SiteAssignmentRes(
                assignment.getMappingId(),
                assignment.getUserId(),
                assignment.getSiteId(),
                assignment.getSiteName(),
                assignment.getSiteAddress(),
                assignment.getAssignedAt()
        );
    }
}
