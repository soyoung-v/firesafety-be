package com.arcguard.firesafety.sensor.service;

import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.sensor.config.SensorMockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockSensorFrameScheduler {

    private final SensorMockProperties sensorMockProperties;
    private final PanelMapper panelMapper;
    private final MockSensorFrameGenerator mockSensorFrameGenerator;
    private final SensorIngestService sensorIngestService;

    // 데모용 가짜 센서 스케줄러. 기존 수신 흐름(저장→경보→집계→WebSocket)을 그대로 탄다.
    // HTTP 재호출 없이 SensorIngestService.ingest()를 바로 호출.
    // 하드웨어 연결되면 SENSOR_MOCK_ENABLED=false로 끄기.
    @Scheduled(
            fixedDelayString = "${constants.sensor-mock.delay-ms:5000}",
            initialDelayString = "${constants.sensor-mock.delay-ms:5000}"
    )
    public void generateMockFrames() {
        if (!sensorMockProperties.isEnabled()) {
            return;
        }

        List<Panel> activePanels = panelMapper.findAllActivePanels();
        if (activePanels.isEmpty()) {
            log.info("Mock 센서 스케줄러 - 활성 분전반이 없어 이번 주기는 건너뜀");
            return;
        }

        for (Panel panel : activePanels) {
            sendMockFrame(panel);
        }
    }

    // 한 분전반 실패가 나머지에 영향 안 주게 개별 처리
    private void sendMockFrame(Panel panel) {
        try {
            Map<String, String> params = mockSensorFrameGenerator.generate(panel);
            sensorIngestService.ingest(params);
        } catch (BusinessException e) {
            log.warn("Mock 센서 프레임 수신 실패 - panelId={}, message={}", panel.getPanelId(), e.getMessage());
        }
    }
}
