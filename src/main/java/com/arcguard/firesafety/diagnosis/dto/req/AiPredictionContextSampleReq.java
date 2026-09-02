package com.arcguard.firesafety.diagnosis.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "AI 서버로 보내는 분전반 공통 센서값 1개 프레임분 (내부용, 외부 API 아님)")
public class AiPredictionContextSampleReq {

    @Schema(description = "전압(sensor_frame.volt_v)", example = "224.0")
    private BigDecimal voltage;

    @Schema(description = "누설전류, mA(sensor_frame.leak_ma)", example = "2.0")
    @JsonProperty("leakage_current")
    private BigDecimal leakageCurrent;

    @Schema(description = "온도, ℃(sensor_frame.temperature)", example = "27.2")
    private BigDecimal temperature;

    @Schema(description = "습도, %(sensor_frame.humidity)", example = "48.4")
    private BigDecimal humidity;

    @Schema(description = "불꽃 감지 raw 값(sensor_frame.fire_raw)", example = "500")
    @JsonProperty("fire_raw")
    private Integer fireRaw;

    @Schema(description = "가스 농도 raw 값(sensor_frame.gas_raw)", example = "500")
    @JsonProperty("gas_raw")
    private Integer gasRaw;

    @Schema(description = "도어 열림 여부(sensor_frame.door_status)", example = "false")
    @JsonProperty("door_open")
    private Boolean doorOpen;

    @Schema(description = "분전반 전체 전류, A(sensor_frame.total_current)", example = "10.0")
    @JsonProperty("total_current")
    private BigDecimal totalCurrent;

    @Schema(description = "분전반 전체 전력(sensor_frame.total_power)", example = "2000")
    @JsonProperty("total_power")
    private Integer totalPower;
}
