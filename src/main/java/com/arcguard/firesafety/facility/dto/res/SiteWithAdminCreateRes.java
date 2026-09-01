package com.arcguard.firesafety.facility.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "현장 + 현장관리자 통합 등록 결과")
public class SiteWithAdminCreateRes {

    @Schema(description = "생성된 현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "현장 이름", example = "아크타워1")
    private String siteName;
    @Schema(description = "생성된 현장관리자 사용자 ID", example = "10")
    private Long adminUserId;
    @Schema(description = "현장관리자 이름", example = "홍길동")
    private String adminName;
    @Schema(description = "현장관리자 이메일", example = "site-admin@example.com")
    private String adminEmail;
    @Schema(description = "현장관리자에게 배정된 현장 ID 목록", example = "[1]")
    private List<Long> assignedSiteIds;
}
