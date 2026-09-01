package com.arcguard.firesafety.monitoring.mapper;

import com.arcguard.firesafety.monitoring.model.CircuitStatusSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 분전반 상태 집계용 MyBatis Mapper
@Mapper
public interface PanelStatusAggregationMapper {

    // 분전반의 최신 수신 프레임 error_bits 조회
    String findLatestErrorBitsByPanelId(@Param("panelId") Long panelId);

    // 집계 전 현재 저장된 분전반 상태 조회
    String findCurrentPanelStatus(@Param("panelId") Long panelId);

    // 분전반의 최신 도어 상태 조회
    Boolean findLatestDoorStatusByPanelId(@Param("panelId") Long panelId);

    // 분전반 회로별 최신 하드웨어/AI 판정 조회
    List<CircuitStatusSnapshot> findCircuitStatusSnapshots(@Param("panelId") Long panelId);

    // 서버 수치 기준값이 30초 이상 지속됐는지 확인
    boolean hasSustainedThresholdCaution(@Param("panelId") Long panelId,
                                         @Param("thresholdAt") LocalDateTime thresholdAt);

    // 서버 수치 기준값을 30초 이상 넘긴 주의 항목 조회
    List<String> findSustainedThresholdCautionTypes(@Param("panelId") Long panelId,
                                                    @Param("thresholdAt") LocalDateTime thresholdAt);

    // 집계된 분전반 상태 저장
    int updatePanelStatus(@Param("panelId") Long panelId, @Param("status") String status);
}
