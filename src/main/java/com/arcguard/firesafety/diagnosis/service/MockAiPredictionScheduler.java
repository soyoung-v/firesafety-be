package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.AiPredictionCircuitTarget;
import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.sensor.config.SensorMockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

// 데모용 가짜 AI 판정. 실제 AI 서버(AiPredictionScheduler) 없이 회로 하나를 골라 가끔 ARC로 저장한다.
// 하드웨어 연결/AI 서버 연동되면 SENSOR_MOCK_ENABLED=false로 같이 꺼진다.
@Slf4j
@Component
@RequiredArgsConstructor
public class MockAiPredictionScheduler {

    // 매 주기 분전반 하나가 ARC로 뽑힐 확률
    private static final double ARC_PROBABILITY = 0.15;
    // 새 샘플이 1개만 있어도 대상으로 잡는다 (실제 AI 서버용 minSampleSize 기준과 다름)
    private static final int MIN_SAMPLE_SIZE = 1;
    private static final double MIN_CONFIDENCE = 0.8;
    private static final double CONFIDENCE_RANGE = 0.19;

    private final SensorMockProperties sensorMockProperties;
    private final PanelMapper panelMapper;
    private final AiDiagnosisResultMapper aiDiagnosisResultMapper;
    private final AiDiagnosisResultSaveService aiDiagnosisResultSaveService;

    private final SecureRandom random = new SecureRandom();

    @Scheduled(
            fixedDelayString = "${constants.sensor-mock.ai-delay-ms:20000}",
            initialDelayString = "${constants.sensor-mock.ai-delay-ms:20000}"
    )
    public void generateMockArcVerdicts() {
        if (!sensorMockProperties.isEnabled()) {
            return;
        }

        List<Panel> activePanels = panelMapper.findAllActivePanels();
        for (Panel panel : activePanels) {
            if (random.nextDouble() < ARC_PROBABILITY) {
                predictOneCircuit(panel);
            }
        }
    }

    // 분전반 하나에서 무작위 회로 하나만 ARC로 판정 (실제 AI 응답 저장 흐름 재사용)
    private void predictOneCircuit(Panel panel) {
        List<AiPredictionCircuitTarget> circuits = aiDiagnosisResultMapper.findPredictionCircuitTargets(
                panel.getPanelId(), MIN_SAMPLE_SIZE
        );
        if (circuits.isEmpty()) {
            return;
        }

        AiPredictionCircuitTarget circuit = circuits.get(random.nextInt(circuits.size()));
        double confidence = MIN_CONFIDENCE + random.nextDouble() * CONFIDENCE_RANGE;
        try {
            aiDiagnosisResultSaveService.save(
                    panel.getPanelId(), circuit.getCircuitId(), circuit.getLatestFrameId(), Verdict.ARC, confidence,
                    null, null, DiagnosisTriggerType.MOCK
            );
        } catch (RuntimeException e) {
            log.warn("Mock AI 판정 저장 실패 - panelId={}, circuitId={}, message={}",
                    panel.getPanelId(), circuit.getCircuitId(), e.getMessage());
        }
    }
}
