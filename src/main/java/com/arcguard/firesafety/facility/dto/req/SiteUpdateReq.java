package com.arcguard.firesafety.facility.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현장 수정 요청. SUPER_ADMIN 전용")
public class SiteUpdateReq {

    @Schema(description = "현장(사업장) 이름", example = "아크타워1")
    private String name;
    @Schema(description = "주소(필수, 도로명주소 검색 API 결과 선택 또는 직접 입력)", example = "서울시 강남구 테헤란로 123")
    private String address;
    @Schema(description = "상세주소(선택, 동/호수 등 검색 결과에 없는 나머지 주소)", example = "5층 501호")
    private String addressDetail;
    @Schema(description = "우편번호(선택, 도로명주소 검색 API 결과의 zipCode)", example = "06134")
    private String zipCode;
}
