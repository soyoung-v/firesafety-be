package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.diagnosis.config.AiExplanationProperties;
import com.arcguard.firesafety.diagnosis.dto.req.AiExplanationPredictionReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiExplanationReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiExplanationSensorEvidenceReq;
import com.arcguard.firesafety.diagnosis.dto.res.AiExplanationRes;
import com.arcguard.firesafety.diagnosis.exception.DiagnosisErrorCode;
import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.AiDiagnosisResult;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.facility.model.Circuit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// LLM 진단 설명(analysisSummary) 생성/캐시 전담 서비스. 핵심 ML 진단 흐름(/predict, AiDiagnosisResultSaveService)과
// 완전히 분리되어 있다 - 이 서비스의 실패는 이미 저장된 diagnosis에 어떤 영향도 주지 않는다.
// 사용자 요청 시에만 호출되며, diagnosis 생성/스케줄러에서 자동 호출하지 않는다(ADR-013).
@Slf4j
@Service
@RequiredArgsConstructor
public class AiDiagnosisExplanationService {

    // 비정상적으로 긴 응답(malformed/모델 오작동)을 그대로 저장하지 않기 위한 상한 - 정상 응답(한국어
    // 2~4문장)은 수백 자 수준이라 여유 있게 잡는다.
    private static final int MAX_ANALYSIS_SUMMARY_LENGTH = 2000;

    private final AiDiagnosisResultMapper aiDiagnosisResultMapper;
    private final AiExplanationClient aiExplanationClient;
    private final AiExplanationProperties aiExplanationProperties;

    // circuitId 소속이 확인된 resultId에 대해 설명을 조회하거나(캐시 hit, 외부 호출 0회) 없으면 최초 1회 생성한다.
    public String getOrCreateExplanation(Circuit circuit, Long resultId) {
        AiDiagnosisResult diagnosisResult = findOwnedDiagnosisResult(circuit.getCircuitId(), resultId);

        if (diagnosisResult.getAnalysisSummary() != null) {
            return diagnosisResult.getAnalysisSummary();
        }

        if (!aiExplanationProperties.isEnabled()) {
            throw new BusinessException(DiagnosisErrorCode.AI_EXPLANATION_DISABLED);
        }

        AiExplanationReq request = buildRequest(circuit, diagnosisResult);
        AiExplanationRes response = callExplain(request);
        String analysisSummary = validateSummary(response);

        return persistAndReturn(resultId, analysisSummary);
    }

    // resultId가 실제로 존재하고 요청받은 circuitId 소유인지 확인 - 다른 회로의 진단 결과를 조회/생성하지 못하게 막는다
    private AiDiagnosisResult findOwnedDiagnosisResult(Long circuitId, Long resultId) {
        AiDiagnosisResult diagnosisResult = aiDiagnosisResultMapper.findById(resultId);
        if (diagnosisResult == null || !diagnosisResult.getCircuitId().equals(circuitId)) {
            throw new BusinessException(DiagnosisErrorCode.DIAGNOSIS_RESULT_NOT_FOUND);
        }
        return diagnosisResult;
    }

    // ML 결과는 그대로 인용하고, 센서 근거는 진단 당시 frame_id 시점의 스냅샷만 조회한다(ADR-013) -
    // "현재 최신 센서값"을 임의로 가져와 쓰지 않는다.
    private AiExplanationReq buildRequest(Circuit circuit, AiDiagnosisResult diagnosisResult) {
        if (diagnosisResult.getConfidence() == null || diagnosisResult.getFrameId() == null) {
            throw new BusinessException(DiagnosisErrorCode.AI_EXPLANATION_EVIDENCE_UNAVAILABLE);
        }

        AiExplanationPredictionReq prediction = new AiExplanationPredictionReq(
                resolvePred(diagnosisResult.getVerdict()),
                diagnosisResult.getConfidence().doubleValue(),
                diagnosisResult.getRiskLevel() == null ? null : diagnosisResult.getRiskLevel().name(),
                toDouble(diagnosisResult.getRiskScore()),
                diagnosisResult.getAnomaly(),
                toDouble(diagnosisResult.getAnomalyScore()),
                toDouble(diagnosisResult.getPredictedCurrent())
        );

        AiExplanationSensorEvidenceReq sensorEvidence = aiDiagnosisResultMapper.findSensorEvidence(
                circuit.getCircuitId(), circuit.getPanelId(), diagnosisResult.getFrameId()
        );
        if (sensorEvidence == null) {
            sensorEvidence = new AiExplanationSensorEvidenceReq(null, null, null, null, null);
        }

        return new AiExplanationReq(circuit.getChannelNo(), prediction, sensorEvidence);
    }

    // verdict(NORMAL/ARC) -> pred(0/1). AiPredictionService.resolveVerdict의 역변환과 동일 규칙.
    private int resolvePred(Verdict verdict) {
        return verdict == Verdict.ARC ? 1 : 0;
    }

    private Double toDouble(Float value) {
        return value == null ? null : value.doubleValue();
    }

    // FastAPI 호출 실패(timeout/connection refused/4xx/5xx 전부 RuntimeException으로 올라옴)를 이미
    // 저장된 diagnosis에 영향 없이 502로 변환한다. 내부 예외 메시지는 응답에 노출하지 않는다.
    private AiExplanationRes callExplain(AiExplanationReq request) {
        try {
            return aiExplanationClient.explain(request);
        } catch (RuntimeException e) {
            log.warn("AI 설명 생성 실패 - message={}", e.getMessage());
            throw new BusinessException(DiagnosisErrorCode.AI_EXPLANATION_FAILED);
        }
    }

    // null/blank/비정상적으로 긴 응답은 저장하지 않는다 - fallback 문장을 만들어내지 않는다
    private String validateSummary(AiExplanationRes response) {
        if (response == null || response.getAnalysisSummary() == null) {
            throw new BusinessException(DiagnosisErrorCode.AI_EXPLANATION_FAILED);
        }
        String summary = response.getAnalysisSummary().trim();
        if (summary.isEmpty() || summary.length() > MAX_ANALYSIS_SUMMARY_LENGTH) {
            throw new BusinessException(DiagnosisErrorCode.AI_EXPLANATION_FAILED);
        }
        return summary;
    }

    // 동시 최초 요청은 둘 다 조회 시점에 null을 봤을 수 있다 - analysis_summary IS NULL 조건의 원자적
    // UPDATE로 실제 DB 저장은 한쪽만 성공시킨다. 진 쪽은 그사이 저장된 DB 값을 반환해 DB를 단일 진실
    // 소스로 유지한다. 다만 이 방식이 외부 API 중복 호출 자체를 막지는 못한다(TBD, 최종 보고 21절).
    private String persistAndReturn(Long resultId, String analysisSummary) {
        int updated = aiDiagnosisResultMapper.updateAnalysisSummaryIfAbsent(resultId, analysisSummary);
        if (updated > 0) {
            return analysisSummary;
        }
        AiDiagnosisResult refreshed = aiDiagnosisResultMapper.findById(resultId);
        return refreshed.getAnalysisSummary() != null ? refreshed.getAnalysisSummary() : analysisSummary;
    }
}
