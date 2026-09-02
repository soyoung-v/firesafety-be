package com.arcguard.firesafety.diagnosis.mapper;

import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionContextSampleReq;
import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionSampleReq;
import com.arcguard.firesafety.diagnosis.dto.res.DiagnosisResultRes;
import com.arcguard.firesafety.diagnosis.dto.res.PanelDiagnosisRecentRes;
import com.arcguard.firesafety.diagnosis.dto.res.PanelDiagnosisSampleStatusRes;
import com.arcguard.firesafety.diagnosis.model.AiDiagnosisResult;
import com.arcguard.firesafety.diagnosis.model.AiPredictionCircuitTarget;
import com.arcguard.firesafety.diagnosis.model.AiPredictionPanelTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// AI 판정 결과 저장/조회용
@Mapper
public interface AiDiagnosisResultMapper {

    // AI 판정 결과 저장
    void insertAiDiagnosisResult(AiDiagnosisResult aiDiagnosisResult);

    // 새 샘플이 충분히 쌓인 분전반 조회
    List<AiPredictionPanelTarget> findPredictionPanels(@Param("minSampleSize") int minSampleSize);

    // 분전반 안에서 AI 호출 대상 회로 조회
    List<AiPredictionCircuitTarget> findPredictionCircuitTargets(@Param("panelId") Long panelId,
                                                                 @Param("minSampleSize") int minSampleSize);

    // AI 요청에 사용할 회로별 최근 샘플 조회
    List<AiPredictionSampleReq> findRecentSamples(@Param("circuitId") Long circuitId,
                                                  @Param("sampleSize") int sampleSize);

    // 신규 확장 context용 - 분전반의 최근 sensor_frame 시계열(오래된 것 -> 최신 순) 조회
    List<AiPredictionContextSampleReq> findRecentContextSamples(@Param("panelId") Long panelId,
                                                                 @Param("sampleSize") int sampleSize);

    // 수동 진단 실행 시 저장용으로 쓸 회로의 가장 최근 프레임ID 조회 (없으면 null)
    Long findLatestFrameId(@Param("circuitId") Long circuitId);

    // 회로별 AI 판정 이력 조회
    List<DiagnosisResultRes> findDiagnosisResults(@Param("circuitId") Long circuitId,
                                                  @Param("size") int size,
                                                  @Param("offset") int offset);

    // 회로별 AI 판정 이력 개수 조회
    long countDiagnosisResults(@Param("circuitId") Long circuitId);

    // 분전반 AI 진단 현황 요약용 집계
    LocalDateTime findLatestDiagnosedAtByPanelId(@Param("panelId") Long panelId);

    long countActiveCircuitsByPanelId(@Param("panelId") Long panelId);

    long countDiagnosedCircuitsByPanelId(@Param("panelId") Long panelId);

    long countPanelDiagnosesSince(@Param("panelId") Long panelId,
                                  @Param("fromAt") LocalDateTime fromAt,
                                  @Param("verdict") String verdict);

    List<PanelDiagnosisRecentRes> findRecentPanelDiagnosisResults(@Param("panelId") Long panelId,
                                                                  @Param("fromAt") LocalDateTime fromAt,
                                                                  @Param("verdict") String verdict,
                                                                  @Param("limit") int limit);

    List<PanelDiagnosisSampleStatusRes> findSampleInsufficientCircuits(@Param("panelId") Long panelId,
                                                                       @Param("minSampleSize") int minSampleSize);
}
