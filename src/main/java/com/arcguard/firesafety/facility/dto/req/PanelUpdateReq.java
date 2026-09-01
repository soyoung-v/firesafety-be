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
@Schema(description = "분전반 수정 요청")
public class PanelUpdateReq {

    @Schema(description = "분전반 이름", example = "분전반1")
    private String name;
    @Schema(description = "장비 시리얼번호. 다른 분전반과 중복 불가", example = "DEMO-SERIAL-001")
    private String deviceSerial;
    @Schema(description = "장비번호(정확히 5자리 고정폭). 센서 m_no와 일치해야 매핑됨", example = "00099")
    private String mNo;
    @Schema(description = "설치일(선택)", example = "2026-07-23")
    private LocalDate installedAt;
    @Schema(description = "회로 개수. 1~10", example = "3")
    private Integer circuitCount;
    @Schema(description = "누설전류 서버 주의(CAUTION) 기준값, 단위 mA", example = "20.0")
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
