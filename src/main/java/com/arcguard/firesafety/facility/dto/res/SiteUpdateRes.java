package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "현장 수정 결과")
public class SiteUpdateRes {

    @Schema(description = "현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "현장 이름", example = "아크타워1")
    private String name;
    @Schema(description = "주소", example = "서울시 강남구")
    private String address;
    @Schema(description = "상세주소", example = "5층 501호")
    private String addressDetail;
    @Schema(description = "우편번호", example = "06134")
    private String zipCode;

    public static SiteUpdateRes from(Site site) {
        return new SiteUpdateRes(
                site.getSiteId(),
                site.getName(),
                site.getAddress(),
                site.getAddressDetail(),
                site.getZipCode()
        );
    }
}
