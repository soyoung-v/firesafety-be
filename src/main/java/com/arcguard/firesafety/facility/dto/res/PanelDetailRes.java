package com.arcguard.firesafety.facility.dto.res;

import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.PanelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "분전반 상세")
public class PanelDetailRes {

    @Schema(description = "분전반 ID", example = "1")
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
    @Schema(description = "통신 온라인 여부", example = "true")
    private Boolean isOnline;
    @Schema(description = "마지막 수신 시각. 1분 이상 지나면 통신두절 처리", example = "2026-07-23T14:30:00")
    private LocalDateTime lastCommunicatedAt;
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

    @Schema(description = "최신 전체전류(A). 최근 수신 프레임이 없으면 null", example = "77.0")
    private BigDecimal totalCurrent;
    @Schema(description = "최신 누설전류(mA)", example = "15.0")
    private BigDecimal leakMa;
    @Schema(description = "최신 전압(V)", example = "220.0")
    private BigDecimal voltV;
    @Schema(description = "최신 전체전력(W)", example = "555")
    private Integer totalPower;
    @Schema(description = "최신 도어 상태. true=열림, false=닫힘", example = "false")
    private Boolean doorStatus;
    @Schema(description = "하드웨어 도어 알람 비트 활성 여부(aerror ALARM byte bit5). true면 도어를 하드웨어 위험으로 판단한 상태", example = "false")
    private Boolean doorAlarm;
    @Schema(description = "최신 온도(도)", example = "35.0")
    private BigDecimal temperature;
    @Schema(description = "최신 습도(%)", example = "35.35")
    private BigDecimal humidity;
    @Schema(description = "최신 불꽃센서 원시값", example = "7777")
    private Integer fireRaw;
    @Schema(description = "최신 가스센서 원시값", example = "8888")
    private Integer gasRaw;
    @Schema(description = "하드웨어 누설전류 알람 비트 활성 여부(aerror ALARM byte). true면 서버 기준값과 무관하게 하드웨어가 위험으로 판단한 상태", example = "false")
    private Boolean leakageAlarm;
    @Schema(description = "하드웨어 과열(온도) 알람 비트 활성 여부", example = "false")
    private Boolean overheatAlarm;
    @Schema(description = "하드웨어 습도 알람 비트 활성 여부", example = "false")
    private Boolean humidityAlarm;
    @Schema(description = "하드웨어 가스 알람 비트 활성 여부", example = "false")
    private Boolean gasAlarm;
    @Schema(description = "하드웨어 불꽃 알람 비트 활성 여부", example = "false")
    private Boolean fireAlarm;
    @Schema(description = "하드웨어 과전류 알람 비트 활성 여부", example = "false")
    private Boolean overcurrentAlarm;
    @Schema(description = "회로별 상태 목록")
    private List<PanelCircuitStatusRes> circuits;
    @Schema(description = "최근 경보 목록(최신순)")
    private List<PanelRecentAlertRes> recentAlerts;

    public static PanelDetailRes from(Panel panel,
                                       BigDecimal totalCurrent,
                                       BigDecimal leakMa,
                                       BigDecimal voltV,
                                       Integer totalPower,
                                       Boolean doorStatus,
                                       Boolean doorAlarm,
                                       BigDecimal temperature,
                                       BigDecimal humidity,
                                       Integer fireRaw,
                                       Integer gasRaw,
                                       Boolean leakageAlarm,
                                       Boolean overheatAlarm,
                                       Boolean humidityAlarm,
                                       Boolean gasAlarm,
                                       Boolean fireAlarm,
                                       Boolean overcurrentAlarm,
                                       List<PanelCircuitStatusRes> circuits,
                                       List<PanelRecentAlertRes> recentAlerts) {
        return new PanelDetailRes(
                panel.getPanelId(),
                panel.getSiteId(),
                panel.getName(),
                panel.getDeviceSerial(),
                panel.getMNo(),
                panel.getInstalledAt(),
                panel.getStatus(),
                panel.getIsOnline(),
                panel.getLastCommunicatedAt(),
                panel.getCircuitCount(),
                panel.getLeakMaThreshold(),
                panel.getTempThreshold(),
                panel.getHumidityThreshold(),
                panel.getOvercurrentThreshold(),
                panel.getGasThreshold(),
                panel.getFireThreshold(),
                totalCurrent,
                leakMa,
                voltV,
                totalPower,
                doorStatus,
                doorAlarm,
                temperature,
                humidity,
                fireRaw,
                gasRaw,
                leakageAlarm,
                overheatAlarm,
                humidityAlarm,
                gasAlarm,
                fireAlarm,
                overcurrentAlarm,
                circuits,
                recentAlerts
        );
    }
}
