package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.alert.service.AiAlertService;
import com.arcguard.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.arcguard.firesafety.diagnosis.model.AiDiagnosisResult;
import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
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
    @Transactional
    public void save(Long panelId, Long circuitId, Long frameId, Verdict verdict, Double confidence,
                      Integer nSamples, String warning, DiagnosisTriggerType triggerType) {
        AiDiagnosisResult diagnosisResult = new AiDiagnosisResult();
        diagnosisResult.setCircuitId(circuitId);
        diagnosisResult.setFrameId(frameId);
        diagnosisResult.setVerdict(verdict);
        diagnosisResult.setConfidence(toFloat(confidence));
        diagnosisResult.setNSamples(nSamples);
        diagnosisResult.setWarning(warning);
        diagnosisResult.setTriggerType(triggerType == null ? DiagnosisTriggerType.UNKNOWN : triggerType);

        aiDiagnosisResultMapper.insertAiDiagnosisResult(diagnosisResult);

        if (verdict == Verdict.ARC) {
            aiAlertService.createArcAlert(panelId, circuitId, diagnosisResult.getResultId());
        }
    }

    // AI proba가 없으면 confidence는 NULL로 저장
    private Float toFloat(Double confidence) {
        if (confidence == null) {
            return null;
        }
        return confidence.floatValue();
    }
}
