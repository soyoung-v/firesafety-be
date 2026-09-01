package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "현장 목록 항목")
public class SiteListRes {

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
    @Schema(description = "등록일시", example = "2026-07-20T10:00:00")
    private LocalDateTime createdAt;

    public static SiteListRes from(Site site) {
        return new SiteListRes(
                site.getSiteId(),
                site.getName(),
                site.getAddress(),
                site.getAddressDetail(),
                site.getZipCode(),
                site.getCreatedAt()
        );
    }
}
