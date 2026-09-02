package com.arcguard.firesafety.diagnosis.dto.res;

import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.diagnosis.model.DiagnosisTriggerType;
import com.arcguard.firesafety.diagnosis.model.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "회로별 AI 진단 결과")
public class DiagnosisResultRes {

    @Schema(description = "AI 진단 결과 ID", example = "1")
    private Long resultId;

    @Schema(description = "회로 ID", example = "10")
    private Long circuitId;

    @Schema(description = "AI 판정에 사용된 센서 프레임 ID", example = "100")
    private Long frameId;

    @Schema(description = "AI 이진 분류 결과. NORMAL=정상, ARC=아크", example = "ARC")
    private Verdict verdict;

    @Schema(description = "verdict에 대한 확신도(0~1). DB에는 AI 서버 원본 proba(아크일 확률)를 저장하지만, " +
            "verdict=NORMAL인 응답은 조회 시 (1-proba)로 변환해서 내려준다 — 즉 이 값은 항상 " +
            "'표시된 verdict가 맞을 확률'이다", example = "0.92")
    private Float confidence;

    @Schema(description = "AI 서버가 이번 판정에 실제 사용한 샘플 개수", example = "60")
    private Integer nSamples;

    @Schema(description = "AI 서버 경고 메시지(예: 샘플 부족). 없으면 null", example = "샘플 45개 — 60개 이상 권장 (정확도 저하 가능)")
    private String warning;

    @Schema(description = "진단 실행 방식. AUTO=자동 스케줄러, MANUAL=수동 실행, MOCK=데모, UNKNOWN=기존 데이터", example = "AUTO")
    private DiagnosisTriggerType triggerType;

    @Schema(description = "AI 진단 저장 시각", example = "2026-07-23T14:35:00")
    private LocalDateTime diagnosedAt;

    // 신규 확장 결과 - verdict/confidence(기존 ARC 판정)와는 별개 의미. context 없이 판정됐으면 전부 null.
    @Schema(description = "신규 위험도 3단계(NORMAL/WARNING/DANGER). 없으면 null", example = "NORMAL")
    private RiskLevel riskLevel;

    @Schema(description = "신규 위험도 점수(0~1, 화재 발생 확률 아님). 없으면 null", example = "0.11")
    private Float riskScore;

    @Schema(description = "정상 패턴 이탈 여부(비지도 판정). 없으면 null", example = "false")
    private Boolean anomaly;

    @Schema(description = "이상치 점수(0~1, 높을수록 정상과 다름). 없으면 null", example = "0.43")
    private Float anomalyScore;

    @Schema(description = "다음 sample의 예상 전류값(A). 없으면 null", example = "5.19")
    private Float predictedCurrent;
}
