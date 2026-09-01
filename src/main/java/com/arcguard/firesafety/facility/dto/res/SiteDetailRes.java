package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "현장 상세")
public class SiteDetailRes {

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
    @Schema(description = "이 현장의 활성 분전반 수", example = "3")
    private int panelCount;
    @Schema(description = "등록일시", example = "2026-07-20T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "수정일시", example = "2026-07-21T10:00:00")
    private LocalDateTime updatedAt;

    public static SiteDetailRes from(Site site, int panelCount) {
        return new SiteDetailRes(
                site.getSiteId(),
                site.getName(),
                site.getAddress(),
                site.getAddressDetail(),
                site.getZipCode(),
                panelCount,
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }
}
