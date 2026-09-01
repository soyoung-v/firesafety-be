package com.arcguard.firesafety.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 요청")
public class LoginReq {

    @Schema(description = "로그인 아이디(이메일)", example = "user@example.com")
    private String email;

    @Schema(description = "비밀번호", example = "password1234")
    private String password;
}
