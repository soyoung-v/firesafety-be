package com.arcguard.firesafety.alert.mapper;

import com.arcguard.firesafety.alert.dto.res.AlertExportRes;
import com.arcguard.firesafety.alert.dto.res.AlertListRes;
import com.arcguard.firesafety.alert.dto.res.AlertPendingRes;
import com.arcguard.firesafety.alert.model.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 경보(alert) 생성/조회 및 상태 전이용 MyBatis Mapper
@Mapper
public interface AlertMapper {

    // 경보 발생 이력 저장
    void insertAlert(Alert alert);

    // 경보 목록 조회
    List<AlertListRes> findAlerts(@Param("userId") Long userId,
                                  @Param("superAdmin") boolean superAdmin,
                                  @Param("status") String status,
                                  @Param("type") String type,
                                  @Param("siteId") Long siteId,
                                  @Param("panelId") Long panelId,
                                  @Param("fromAt") LocalDateTime fromAt,
                                  @Param("toAt") LocalDateTime toAt,
                                  @Param("size") int size,
                                  @Param("offset") int offset);

    // 경보 목록 전체 개수 조회
    long countAlerts(@Param("userId") Long userId,
                     @Param("superAdmin") boolean superAdmin,
                     @Param("status") String status,
                     @Param("type") String type,
                     @Param("siteId") Long siteId,
                     @Param("panelId") Long panelId,
                     @Param("fromAt") LocalDateTime fromAt,
                     @Param("toAt") LocalDateTime toAt);

    // 경보 이력 엑셀 다운로드용 전체/선택 목록 조회
    List<AlertExportRes> findAlertExportRows(@Param("userId") Long userId,
                                             @Param("superAdmin") boolean superAdmin,
                                             @Param("status") String status,
                                             @Param("type") String type,
                                             @Param("siteId") Long siteId,
                                             @Param("fromAt") LocalDateTime fromAt,
                                             @Param("toAt") LocalDateTime toAt,
                                             @Param("alertIds") List<Long> alertIds);

    // 권한 범위 안의 경보 단건 조회
    Alert findAccessibleAlertById(@Param("userId") Long userId,
                                  @Param("superAdmin") boolean superAdmin,
                                  @Param("alertId") Long alertId);

    // 미확인 경보를 확인 상태로 전환
    int confirmAlert(@Param("alertId") Long alertId, @Param("userId") Long userId);

    // 확인된 경보를 조치완료 상태로 전환
    int resolveAlert(@Param("alertId") Long alertId, @Param("userId") Long userId, @Param("resolutionNote") String resolutionNote);

    // 같은 대상의 미조치 경보 존재 여부 확인
    boolean existsUnresolvedAlert(@Param("panelId") Long panelId,
                                  @Param("source") String source,
                                  @Param("type") String type,
                                  @Param("severity") String severity);

    // 같은 회로의 미조치 경보 존재 여부 확인
    boolean existsUnresolvedCircuitAlert(@Param("circuitId") Long circuitId,
                                         @Param("source") String source,
                                         @Param("type") String type,
                                         @Param("severity") String severity);

    // 경보가 속한 분전반의 현장 ID 조회
    Long findSiteIdByPanelId(@Param("panelId") Long panelId);

    // 미처리(UNCONFIRMED/CONFIRMED) 경보 목록 조회 - 기간 필터 없음 (REQ-306)
    List<AlertPendingRes> findPendingAlerts(@Param("userId") Long userId,
                                            @Param("superAdmin") boolean superAdmin,
                                            @Param("siteId") Long siteId,
                                            @Param("size") int size,
                                            @Param("offset") int offset);

    // 미처리 경보 전체 개수 조회 (REQ-306)
    long countPendingAlerts(@Param("userId") Long userId,
                            @Param("superAdmin") boolean superAdmin,
                            @Param("siteId") Long siteId);
}
