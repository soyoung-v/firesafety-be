package com.arcguard.firesafety.monitoring.mapper;

import com.arcguard.firesafety.monitoring.model.OfflinePanelTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 통신두절 감지용 MyBatis Mapper
@Mapper
public interface CommunicationMonitorMapper {

    // 1분 이상 수신이 없는 활성 분전반 조회
    List<OfflinePanelTarget> findOfflineTargets(@Param("thresholdAt") LocalDateTime thresholdAt);

    // 통신두절 분전반 상태 전환
    int markPanelOffline(@Param("panelId") Long panelId);
}
