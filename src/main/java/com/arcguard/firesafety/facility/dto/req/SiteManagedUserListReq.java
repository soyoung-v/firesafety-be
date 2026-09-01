package com.arcguard.firesafety.facility.dto.req;

import com.arcguard.firesafety.auth.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "현장 담당 직원 목록 조회 조건")
public class SiteManagedUserListReq {

    @Schema(description = "이름/이메일/전화번호 검색어(선택, 부분일치)", example = "홍길동")
    private String keyword;
    @Schema(description = "역할 필터(선택). ADMIN/GENERAL만 허용", example = "GENERAL")
    private UserRole role;
    @Schema(description = "페이지 번호. 0부터 시작(선택)", example = "0")
    private Integer page;
    @Schema(description = "페이지 크기(선택)", example = "20")
    private Integer size;
}
