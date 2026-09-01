package com.arcguard.firesafety.facility.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "분전반 등록 요청 (siteId는 URL 경로 /api/sites/{siteId}/panels 에 넣음)")
public class PanelCreateReq {

    @Schema(description = "분전반 이름", example = "분전반1")
    private String name;
    @Schema(description = "장비 시리얼번호. 다른 분전반과 중복 불가", example = "DEMO-SERIAL-001")
    private String deviceSerial;
    @Schema(description = "장비번호(정확히 5자리 고정폭). 실제/Mock 센서가 보내는 m_no와 정확히 일치해야 매핑됨", example = "00099")
    private String mNo;
    @Schema(description = "설치일(선택)", example = "2026-07-23")
    private LocalDate installedAt;
    @Schema(description = "회로 개수. 1~10. 이 개수만큼 회로를 따로 등록해야 함", example = "3")
    private Integer circuitCount;
    @Schema(description = "누설전류 서버 주의(CAUTION) 기준값, 단위 mA. 하드웨어 위험 기준이 아님", example = "20.0")
    private BigDecimal leakMaThreshold;
    @Schema(description = "온도 서버 주의(CAUTION) 기준값, 단위 도", example = "80.0")
    private BigDecimal tempThreshold;
    @Schema(description = "습도 서버 주의(CAUTION) 기준값, 단위 %", example = "80.0")
    private BigDecimal humidityThreshold;
    @Schema(description = "과전류 서버 주의(CAUTION) 기준값, 단위 A", example = "30.0")
    private BigDecimal overcurrentThreshold;
    @Schema(description = "가스 서버 주의 기준값(선택, 미입력 시 5000 기본값, 원시값 gas_raw >= 기준값 30초 지속 시 CAUTION)", example = "5000")
    private Integer gasThreshold;
    @Schema(description = "불꽃 서버 주의 기준값(선택, 미입력 시 5000 기본값, 원시값 fire_raw >= 기준값 30초 지속 시 CAUTION)", example = "5000")
    private Integer fireThreshold;
}
