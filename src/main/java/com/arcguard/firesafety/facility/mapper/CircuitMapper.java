package com.arcguard.firesafety.facility.mapper;

import com.arcguard.firesafety.facility.model.Circuit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 회로(circuit) 등록/조회 및 채널 중복 검사용 MyBatis Mapper
@Mapper
public interface CircuitMapper {

    // 회로 등록
    void insertCircuit(Circuit circuit);

    // 등록 직후 생성된 회로 조회
    Circuit findActiveCircuitById(@Param("circuitId") Long circuitId);

    // 분전반 회로 목록 조회
    List<Circuit> findActiveCircuitsByPanelId(@Param("panelId") Long panelId);

    // 디바이스 수신값 저장용 회로 조회
    Circuit findActiveCircuitByPanelIdAndChannelNo(@Param("panelId") Long panelId,
                                                   @Param("channelNo") Integer channelNo);

    // 회로 소프트 삭제
    int softDeleteCircuit(@Param("circuitId") Long circuitId);

    // 삭제 여부와 무관하게 분전반+채널번호로 회로 조회 (재등록 시 삭제된 회로 재활성화 대상 확인용)
    Circuit findCircuitByPanelIdAndChannelNo(@Param("panelId") Long panelId, @Param("channelNo") Integer channelNo);

    // 삭제됐던 회로를 같은 채널번호로 재등록할 때 deleted_at을 되돌려 재활성화
    int reactivateCircuit(@Param("circuitId") Long circuitId, @Param("loadType") String loadType);

    // 회로 부하종류 수정
    int updateCircuitLoadType(@Param("circuitId") Long circuitId, @Param("loadType") String loadType);
}
