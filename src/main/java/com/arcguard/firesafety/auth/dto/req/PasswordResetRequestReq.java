package com.arcguard.firesafety.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 재설정 요청(메일 발송)")
public class PasswordResetRequestReq {

    @Schema(description = "비밀번호를 재설정할 계정 이메일. 존재 여부와 관계없이 응답은 동일함", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다")
    private String email;
}
