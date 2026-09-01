package com.arcguard.firesafety.sensor.dto.res;

import com.arcguard.firesafety.sensor.model.SensorFrame;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "센서 데이터 수신 결과")
public class SensorFrameIngestRes {

    @Schema(description = "저장된 센서 프레임 ID", example = "1")
    private Long frameId;

    @Schema(description = "서버 수신 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime receivedAt;

    public static SensorFrameIngestRes from(SensorFrame sensorFrame) {
        return new SensorFrameIngestRes(sensorFrame.getFrameId(), sensorFrame.getReceivedAt());
    }
}
