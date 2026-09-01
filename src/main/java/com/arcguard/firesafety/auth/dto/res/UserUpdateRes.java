package com.arcguard.firesafety.auth.dto.res;

import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "계정 수정 결과")
public class UserUpdateRes {

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

    public static UserUpdateRes from(User user) {
        return new UserUpdateRes(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getAccountStatus()
        );
    }
}
