package com.arcguard.firesafety.diagnosis.mapper;

import com.arcguard.firesafety.diagnosis.dto.req.AiExplanationSensorEvidenceReq;
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

    // Frame Alignment 보강 - AI Request 하나(context+circuit samples)가 반드시 같은 frame window를
    // 쓰도록, 분전반의 최근 N개 sensor_frame.frame_id를 먼저 확정한다 (오래된 것 -> 최신 순)
    List<Long> findRecentFrameIds(@Param("panelId") Long panelId, @Param("sampleSize") int sampleSize);

    // 확정된 frame_id 목록 안에서만 회로 샘플 조회 - 회로에 그 프레임이 없으면 그만큼 적게 반환되고,
    // 목록 밖의 다른 프레임으로 채워 넣지 않는다(= context와 다른 frame이 섞이는 것을 원천 차단, ADR-011 개정)
    List<AiPredictionSampleReq> findSamplesByFrameIds(@Param("circuitId") Long circuitId,
                                                       @Param("frameIds") List<Long> frameIds);

    // 확정된 frame_id 목록 안에서만 분전반 공통 context 조회
    List<AiPredictionContextSampleReq> findContextSamplesByFrameIds(@Param("panelId") Long panelId,
                                                                     @Param("frameIds") List<Long> frameIds);

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

    // Phase 11: LLM 설명(analysisSummary) 생성/캐시용 - 진단 결과 1건 단건 조회
    AiDiagnosisResult findById(@Param("resultId") Long resultId);

    // 진단이 실제로 사용한 frame_id 시점의 센서 스냅샷 조회(ADR-013) - "현재 최신값"이 아니라 그 진단
    // 당시의 정확한 값. 해당 frame이 없으면(예: 아주 오래된 데이터) null을 반환한다.
    AiExplanationSensorEvidenceReq findSensorEvidence(@Param("circuitId") Long circuitId,
                                                       @Param("panelId") Long panelId,
                                                       @Param("frameId") Long frameId);

    // analysis_summary가 아직 비어 있을 때만 채운다(원자적 조건부 UPDATE) - 동시 최초 요청 중 한쪽만
    // 실제로 저장에 성공하게 해서 DB를 단일 진실 소스로 유지한다. 반환값은 영향받은 row 수(0 또는 1).
    int updateAnalysisSummaryIfAbsent(@Param("resultId") Long resultId, @Param("analysisSummary") String analysisSummary);
}
