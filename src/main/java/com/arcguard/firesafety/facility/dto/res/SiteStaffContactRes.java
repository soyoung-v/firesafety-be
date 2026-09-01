package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.facility.model.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

// GENERAL도 조회하는 연락망이라 이메일/생성자/삭제정보 같은 관리용 필드는 담지 않는다
@Getter
@AllArgsConstructor
@Schema(description = "같은 현장 직원 연락망 항목. 연락에 필요한 최소 정보만 포함")
public class SiteStaffContactRes {

    @Schema(description = "사용자 ID", example = "10")
    private Long userId;
    @Schema(description = "이름", example = "홍길동")
    private String name;
    @Schema(description = "전화번호", example = "01012345678")
    private String phone;
    @Schema(description = "등급", example = "GENERAL")
    private UserRole role;
    @Schema(description = "현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "현장 이름", example = "아크타워1")
    private String siteName;

    public static SiteStaffContactRes from(User user, Site site) {
        return new SiteStaffContactRes(
                user.getUserId(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                site.getSiteId(),
                site.getName()
        );
    }
}
