package com.arcguard.firesafety.monitoring.mapper;

import com.arcguard.firesafety.monitoring.dto.res.DashboardPanelRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 대시보드 요약 조회용 MyBatis Mapper
@Mapper
public interface DashboardMapper {

    // 접근 가능한 활성 분전반 전체 개수 조회
    long countAccessiblePanels(@Param("userId") Long userId,
                               @Param("superAdmin") boolean superAdmin,
                               @Param("siteId") Long siteId);

    // 접근 가능한 활성 분전반 상태별 개수 조회
    long countAccessiblePanelsByStatus(@Param("userId") Long userId,
                                       @Param("superAdmin") boolean superAdmin,
                                       @Param("siteId") Long siteId,
                                       @Param("status") String status);

    // 접근 가능한 활성 분전반의 미확인 경보 개수 조회
    long countUnconfirmedAlerts(@Param("userId") Long userId,
                                @Param("superAdmin") boolean superAdmin,
                                @Param("siteId") Long siteId);

    // 접근 가능한 활성 분전반의 미조치 경보 개수 조회
    long countUnresolvedAlerts(@Param("userId") Long userId,
                               @Param("superAdmin") boolean superAdmin,
                               @Param("siteId") Long siteId);

    // 대시보드 분전반 목록 조회
    List<DashboardPanelRes> findDashboardPanels(@Param("userId") Long userId,
                                                @Param("superAdmin") boolean superAdmin,
                                                @Param("siteId") Long siteId);
}
