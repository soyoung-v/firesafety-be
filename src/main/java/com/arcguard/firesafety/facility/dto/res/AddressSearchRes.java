package com.arcguard.firesafety.facility.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주소 검색 결과 항목")
public class AddressSearchRes {

    @Schema(description = "도로명주소(없으면 지번주소)", example = "서울특별시 강남구 테헤란로 123")
    private String address;
    @Schema(description = "우편번호", example = "06134")
    private String zipCode;
    @Schema(description = "건물명(선택)", example = "아크타워빌딩")
    private String buildingName;
}
