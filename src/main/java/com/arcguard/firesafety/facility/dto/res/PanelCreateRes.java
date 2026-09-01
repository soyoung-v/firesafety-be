package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.PanelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Schema(description = "분전반 등록 결과")
public class PanelCreateRes {

    @Schema(description = "생성된 분전반 ID", example = "1")
    private Long panelId;
    @Schema(description = "소속 현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "분전반 이름", example = "분전반1")
    private String name;
    @Schema(description = "장비 시리얼번호", example = "DEMO-SERIAL-001")
    private String deviceSerial;
    @Schema(description = "장비번호. 센서 수신 m_no와 매핑되는 값", example = "00099")
    private String mNo;
    @Schema(description = "설치일", example = "2026-07-23")
    private LocalDate installedAt;
    @Schema(description = "상태(NORMAL/CAUTION/RISK/OFFLINE)", example = "NORMAL")
    private PanelStatus status;
    @Schema(description = "회로 개수", example = "3")
    private Integer circuitCount;
    @Schema(description = "누설전류 서버 주의 기준값(mA)", example = "20.0")
    private BigDecimal leakMaThreshold;
    @Schema(description = "온도 서버 주의 기준값(도)", example = "80.0")
    private BigDecimal tempThreshold;
    @Schema(description = "습도 서버 주의 기준값(%)", example = "80.0")
    private BigDecimal humidityThreshold;
    @Schema(description = "과전류 서버 주의 기준값(A)", example = "30.0")
    private BigDecimal overcurrentThreshold;
    @Schema(description = "가스 서버 주의 기준값(선택, 미입력 시 5000 기본값, 원시값 gas_raw >= 기준값 30초 지속 시 CAUTION)", example = "5000")
    private Integer gasThreshold;
    @Schema(description = "불꽃 서버 주의 기준값(선택, 미입력 시 5000 기본값, 원시값 fire_raw >= 기준값 30초 지속 시 CAUTION)", example = "5000")
    private Integer fireThreshold;

    public static PanelCreateRes from(Panel panel) {
        return new PanelCreateRes(
                panel.getPanelId(),
                panel.getSiteId(),
                panel.getName(),
                panel.getDeviceSerial(),
                panel.getMNo(),
                panel.getInstalledAt(),
                panel.getStatus(),
                panel.getCircuitCount(),
                panel.getLeakMaThreshold(),
                panel.getTempThreshold(),
                panel.getHumidityThreshold(),
                panel.getOvercurrentThreshold(),
                panel.getGasThreshold(),
                panel.getFireThreshold()
        );
    }
}
