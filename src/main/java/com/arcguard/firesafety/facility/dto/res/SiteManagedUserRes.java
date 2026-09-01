package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "현장 담당 직원 목록 항목. 관리 목적이라 이메일까지 포함")
public class SiteManagedUserRes {

    @Schema(description = "사용자 ID", example = "10")
    private Long userId;
    @Schema(description = "이름", example = "홍길동")
    private String name;
    @Schema(description = "이메일", example = "user@example.com")
    private String email;
    @Schema(description = "전화번호", example = "01012345678")
    private String phone;
    @Schema(description = "등급", example = "GENERAL")
    private UserRole role;
    @Schema(description = "가입일시", example = "2026-07-20T10:00:00")
    private LocalDateTime createdAt;

    public static SiteManagedUserRes from(User user) {
        return new SiteManagedUserRes(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
