package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.PanelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "분전반 목록 항목")
public class PanelListRes {

    @Schema(description = "분전반 ID", example = "1")
    private Long panelId;
    @Schema(description = "소속 현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "분전반 이름", example = "분전반1")
    private String name;
    @Schema(description = "장비 시리얼번호", example = "DEMO-SERIAL-001")
    private String deviceSerial;
    @Schema(description = "장비번호", example = "00099")
    private String mNo;
    @Schema(description = "상태(NORMAL/CAUTION/RISK/OFFLINE)", example = "NORMAL")
    private PanelStatus status;
    @Schema(description = "통신 온라인 여부", example = "true")
    private Boolean isOnline;
    @Schema(description = "마지막 수신 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime lastCommunicatedAt;

    public static PanelListRes from(Panel panel) {
        return new PanelListRes(
                panel.getPanelId(),
                panel.getSiteId(),
                panel.getName(),
                panel.getDeviceSerial(),
                panel.getMNo(),
                panel.getStatus(),
                panel.getIsOnline(),
                panel.getLastCommunicatedAt()
        );
    }
}
