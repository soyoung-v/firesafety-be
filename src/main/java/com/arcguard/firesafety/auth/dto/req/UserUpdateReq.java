package com.arcguard.firesafety.auth.dto.req;

import com.arcguard.firesafety.auth.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "계정 수정 요청")
public class UserUpdateReq {

    @Schema(description = "이메일", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
    private String email;

    @Schema(description = "사용자 이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @Schema(description = "전화번호(선택)", example = "01012345678")
    private String phone;

    @Schema(description = "등급. SUPER_ADMIN/ADMIN은 ADMIN/GENERAL로 수정 가능", example = "GENERAL")
    @NotNull(message = "역할은 필수입니다")
    private UserRole role;
}
