package com.arcguard.firesafety.facility.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현장 + 현장관리자(ADMIN) 통합 등록 요청. SUPER_ADMIN 전용")
public class SiteWithAdminCreateReq {

    @Schema(description = "현장(사업장) 이름. 활성 현장끼리 중복 불가", example = "아크타워1")
    private String name;
    @Schema(description = "주소(필수)", example = "서울시 강남구 테헤란로 123")
    private String address;
    @Schema(description = "상세주소(선택, 동/호수 등 검색 결과에 없는 나머지 주소)", example = "5층 501호")
    private String addressDetail;
    @Schema(description = "우편번호(선택)", example = "06134")
    private String zipCode;

    @Schema(description = "현장관리자 이름", example = "홍길동")
    private String adminName;
    @Schema(description = "현장관리자 로그인 이메일. 중복 불가", example = "site-admin@example.com")
    private String adminEmail;
    @Schema(description = "현장관리자 초기 비밀번호. 공백 없이 영문+숫자 8자 이상")
    private String adminPassword;
    @Schema(description = "현장관리자 전화번호(선택)", example = "01012345678")
    private String adminPhone;
}
