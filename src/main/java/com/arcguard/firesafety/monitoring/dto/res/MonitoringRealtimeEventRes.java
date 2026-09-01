package com.arcguard.firesafety.monitoring.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "WebSocket(STOMP)으로 방송하는 실시간 갱신 알림. 상세 데이터는 안 담고 새로고침 신호만 보냄")
public class MonitoringRealtimeEventRes {

    @Schema(description = "이벤트 원인", example = "SENSOR_FRAME_RECEIVED")
    private String eventType;
    @Schema(description = "이벤트 발생 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime occurredAt;
}
