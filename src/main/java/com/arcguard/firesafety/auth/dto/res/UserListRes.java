package com.arcguard.firesafety.auth.dto.res;

import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "계정 목록 항목")
public class UserListRes {

    @Schema(description = "사용자 ID", example = "10")
    private Long userId;
    @Schema(description = "이메일", example = "user@example.com")
    private String email;
    @Schema(description = "이름", example = "홍길동")
    private String name;
    @Schema(description = "전화번호", example = "01012345678")
    private String phone;
    @Schema(description = "등급", example = "GENERAL")
    private UserRole role;
    @Schema(description = "계정 상태(ACTIVE/DELETED)", example = "ACTIVE")
    private UserAccountStatus accountStatus;
    @Schema(description = "가입일시", example = "2026-07-20T10:00:00")
    private LocalDateTime createdAt;

    public static UserListRes from(User user) {
        return new UserListRes(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getAccountStatus(),
                user.getCreatedAt()
        );
    }
}
