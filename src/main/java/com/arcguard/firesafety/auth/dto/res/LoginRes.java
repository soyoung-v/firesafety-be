package com.arcguard.firesafety.auth.dto.res;

import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로그인 성공 응답. 토큰은 body가 아니라 Set-Cookie(at, rt)로 전달됨")
public class LoginRes {

    @Schema(description = "사용자 ID", example = "3")
    private Long userId;
    @Schema(description = "사용자 이름", example = "박소영")
    private String name;
    @Schema(description = "등급", example = "ADMIN")
    private UserRole role;

    // 로그인 응답 body에는 화면 표시용 사용자 정보만 포함
    public static LoginRes from(User user) {
        return new LoginRes(user.getUserId(), user.getName(), user.getRole());
    }
}
