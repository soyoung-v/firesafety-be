package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.diagnosis.config.AiPredictionProperties;
import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionCircuitReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionContextReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionContextSampleReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionSampleReq;
import com.arcguard.firesafety.diagnosis.dto.res.AiPredictionRes;
import com.arcguard.firesafety.diagnosis.dto.res.AiPredictionResultRes;
import com.arcguard.firesafety.diagnosis.exception.DiagnosisErrorCode;
import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.AiPredictionCircuitTarget;
import com.arcguard.firesafety.diagnosis.model.AiPredictionPanelTarget;
import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.model.Circuit;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.monitoring.service.PanelStatusAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPredictionService {

    private final AiDiagnosisResultMapper aiDiagnosisResultMapper;
    private final AiPredictionClient aiPredictionClient;
    private final AiDiagnosisResultSaveService aiDiagnosisResultSaveService;
    private final PanelStatusAggregationService panelStatusAggregationService;
    private final AiPredictionProperties aiPredictionProperties;

    // AI 예측 대상 분전반 처리
    public int predictReadyPanels() {
        List<AiPredictionPanelTarget> panels = aiDiagnosisResultMapper.findPredictionPanels(
                aiPredictionProperties.getMinSampleSize()
        );

        int savedCount = 0;
        for (AiPredictionPanelTarget panel : panels) {
            savedCount += predictPanel(panel);
        }
        return savedCount;
    }

    // 분전반 단위로 회로 샘플을 묶어 AI 서버에 전송
    private int predictPanel(AiPredictionPanelTarget panel) {
        List<AiPredictionCircuitTarget> circuits = aiDiagnosisResultMapper.findPredictionCircuitTargets(
                panel.getPanelId(),
                aiPredictionProperties.getMinSampleSize()
        );
        if (circuits.isEmpty()) {
            return 0;
        }

        AiPredictionReq request = buildRequest(panel.getPanelId(), panel.getMNo(), circuits);
        AiPredictionRes response = aiPredictionClient.predict(request);

        int savedCount = saveResponse(panel.getPanelId(), circuits, response, DiagnosisTriggerType.AUTO);
        if (savedCount > 0) {
            panelStatusAggregationService.aggregatePanelStatus(panel.getPanelId());
        }
        return savedCount;
    }

    // 회로 1개 수동 진단 실행 (REQ-102)
    // 자동 스케줄러(predictReadyPanels)는 "마지막 판정 이후 새 샘플이 충분히 쌓인" 회로만 도는데,
    // 수동 실행은 운영자가 원하는 시점에 그냥 최근 샘플로 1회 호출하는 것이므로 그 조건 없이 동작한다.
    public void predictCircuit(Panel panel, Circuit circuit) {
        if (!aiPredictionProperties.isReady()) {
            throw new BusinessException(DiagnosisErrorCode.AI_PREDICTION_UNAVAILABLE);
        }

        List<Long> frameIds = aiDiagnosisResultMapper.findRecentFrameIds(
                panel.getPanelId(), aiPredictionProperties.getSampleSize()
        );
        List<AiPredictionSampleReq> samples = aiDiagnosisResultMapper.findSamplesByFrameIds(
                circuit.getCircuitId(), frameIds
        );
        // 샘플이 최소 기준(30개)에 못 미치면 조용히 건너뜀 - 트리거 자체는 항상 성공(requested:true)이고
        // 판정 결과가 새로 생기냐 아니냐만 달라지는 비동기 동작이라 여기서 에러를 던지지 않는다.
        if (samples.size() < aiPredictionProperties.getMinSampleSize()) {
            log.info("AI 수동 진단 스킵 - 샘플 부족: circuitId={}, samples={}", circuit.getCircuitId(), samples.size());
            return;
        }

        AiPredictionCircuitTarget target = new AiPredictionCircuitTarget();
        target.setCircuitId(circuit.getCircuitId());
        target.setChannelNo(circuit.getChannelNo());
        target.setLatestFrameId(aiDiagnosisResultMapper.findLatestFrameId(circuit.getCircuitId()));

        AiPredictionCircuitReq circuitRequest = new AiPredictionCircuitReq(circuit.getChannelNo(), samples);
        AiPredictionContextReq context = buildContext(panel.getPanelId(), frameIds);
        AiPredictionReq request = new AiPredictionReq(panel.getMNo(), List.of(circuitRequest), context);
        AiPredictionRes response = aiPredictionClient.predict(request);

        int savedCount = saveResponse(panel.getPanelId(), List.of(target), response, DiagnosisTriggerType.MANUAL);
        if (savedCount > 0) {
            panelStatusAggregationService.aggregatePanelStatus(panel.getPanelId());
        }
    }

    // AI 요청 DTO 생성 - 회로별 샘플(기존 계약)과 분전반 공통 context(신규 확장)를 함께 담는다.
    // Frame Alignment 보강(ADR-011 개정): 분전반의 frame_id 목록을 먼저 확정한 뒤, context와 회로 샘플
    // 모두 그 목록 안에서만 조회한다 - 두 값이 서로 다른 시간창에서 만들어지는 것을 원천 차단한다.
    private AiPredictionReq buildRequest(Long panelId, String mNo, List<AiPredictionCircuitTarget> circuits) {
        List<Long> frameIds = aiDiagnosisResultMapper.findRecentFrameIds(panelId, aiPredictionProperties.getSampleSize());
        List<AiPredictionCircuitReq> circuitRequests = circuits.stream()
                .map(circuit -> buildCircuitRequest(circuit, frameIds))
                .toList();
        AiPredictionContextReq context = buildContext(panelId, frameIds);
        return new AiPredictionReq(mNo, circuitRequests, context);
    }

    // 확정된 frame_id 목록 안에서만 회로 샘플 조회 후 요청 DTO 생성
    private AiPredictionCircuitReq buildCircuitRequest(AiPredictionCircuitTarget circuit, List<Long> frameIds) {
        List<AiPredictionSampleReq> samples = aiDiagnosisResultMapper.findSamplesByFrameIds(
                circuit.getCircuitId(), frameIds
        );
        return new AiPredictionCircuitReq(circuit.getChannelNo(), samples);
    }

    // 확정된 frame_id 목록 안에서만 분전반 공통 context 조회 - 값이 없으면 null을 그대로 둔다
    // (가짜 값 생성 금지, Phase 9 명세 7절). 신규 확장이 실패해도 기존 ARC 판정 흐름이 죽으면 안 되므로
    // 조회 예외를 여기서 흡수한다.
    private AiPredictionContextReq buildContext(Long panelId, List<Long> frameIds) {
        if (frameIds.isEmpty()) {
            return null;
        }
        try {
            List<AiPredictionContextSampleReq> samples = aiDiagnosisResultMapper.findContextSamplesByFrameIds(
                    panelId, frameIds
            );
            if (samples.isEmpty()) {
                return null;
            }
            return new AiPredictionContextReq(samples);
        } catch (RuntimeException e) {
            log.warn("AI context 조회 실패 - Legacy ARC 판정은 계속 진행: panelId={}", panelId, e);
            return null;
        }
    }

    // AI 응답을 회로별 진단결과로 저장
    private int saveResponse(Long panelId, List<AiPredictionCircuitTarget> circuits, AiPredictionRes response,
                             DiagnosisTriggerType triggerType) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return 0;
        }

        Map<Integer, AiPredictionCircuitTarget> circuitsByChannelNo = circuits.stream()
                .collect(Collectors.toMap(AiPredictionCircuitTarget::getChannelNo, Function.identity()));

        int savedCount = 0;
        for (AiPredictionResultRes result : response.getResults()) {
            AiPredictionCircuitTarget circuit = circuitsByChannelNo.get(result.getCircuit());
            if (circuit == null || result.getPred() == null) {
                continue;
            }

            Verdict verdict = resolveVerdict(result.getPred());
            aiDiagnosisResultSaveService.save(
                    panelId,
                    circuit.getCircuitId(),
                    circuit.getLatestFrameId(),
                    verdict,
                    result.getProba(),
                    result.getNSamples(),
                    result.getWarning(),
                    triggerType,
                    result.getRiskLevel(),
                    result.getRiskScore(),
                    result.getAnomaly(),
                    result.getAnomalyScore(),
                    result.getPredictedCurrent()
            );
            savedCount++;
        }
        return savedCount;
    }

    // AI pred 값 변환: 0=NORMAL, 1=ARC
    private Verdict resolveVerdict(Integer pred) {
        if (pred == 1) {
            return Verdict.ARC;
        }
        if (pred == 0) {
            return Verdict.NORMAL;
        }
        log.warn("지원하지 않는 AI pred 값 - pred={}", pred);
        return Verdict.NORMAL;
    }
}
