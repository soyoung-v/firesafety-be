package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.alert.service.AiAlertService;
import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.AiDiagnosisResult;
import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.RiskLevel;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiDiagnosisResultSaveService {

    private final AiDiagnosisResultMapper aiDiagnosisResultMapper;
    private final AiAlertService aiAlertService;

    // AI 판정 저장 후 ARC이면 AI 소스 경보 생성
    // nSamples/warning은 AI 서버가 이번 판정에 실제 사용한 샘플 수·경고(예: 샘플 부족)다 - 이전엔 받아놓고 버렸음
    // riskLevel/riskScore/anomaly/anomalyScore/predictedCurrent는 신규 확장 결과 - verdict(기존 ARC 판정)와는
    // 별개 의미이므로 DANGER나 anomaly=true를 ARC 경보로 강제 변환하지 않는다(ADR-002). 전부 nullable이며
    // context가 없어 AI 서버가 null을 반환해도 기존 ARC 결과 저장은 그대로 성공해야 한다.
    @Transactional
    public void save(Long panelId, Long circuitId, Long frameId, Verdict verdict, Double confidence,
                      Integer nSamples, String warning, DiagnosisTriggerType triggerType,
                      RiskLevel riskLevel, Double riskScore, Boolean anomaly, Double anomalyScore,
                      Double predictedCurrent) {
        AiDiagnosisResult diagnosisResult = new AiDiagnosisResult();
        diagnosisResult.setCircuitId(circuitId);
        diagnosisResult.setFrameId(frameId);
        diagnosisResult.setVerdict(verdict);
        diagnosisResult.setConfidence(toFloat(confidence));
        diagnosisResult.setNSamples(nSamples);
        diagnosisResult.setWarning(warning);
        diagnosisResult.setTriggerType(triggerType == null ? DiagnosisTriggerType.UNKNOWN : triggerType);
        diagnosisResult.setRiskLevel(riskLevel);
        diagnosisResult.setRiskScore(toFloat(riskScore));
        diagnosisResult.setAnomaly(anomaly);
        diagnosisResult.setAnomalyScore(toFloat(anomalyScore));
        diagnosisResult.setPredictedCurrent(toFloat(predictedCurrent));

        aiDiagnosisResultMapper.insertAiDiagnosisResult(diagnosisResult);

        if (verdict == Verdict.ARC) {
            aiAlertService.createArcAlert(panelId, circuitId, diagnosisResult.getResultId());
        }
    }

    // AI proba/score가 없으면 NULL로 저장
    private Float toFloat(Double value) {
        if (value == null) {
            return null;
        }
        return value.floatValue();
    }
}
