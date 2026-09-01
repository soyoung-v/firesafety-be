package com.arcguard.firesafety.facility.mapper;

import com.arcguard.firesafety.facility.model.FacilityAuditLog;
import com.arcguard.firesafety.facility.model.Site;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 현장(site) 등록/조회용 MyBatis Mapper
@Mapper
public interface SiteMapper {

    // 현장 등록
    void insertSite(Site site);

    // 등록 직후 생성된 현장 조회
    Site findActiveSiteById(@Param("siteId") Long siteId);

    // SUPER_ADMIN 현장 목록 조회
    List<Site> findActiveSites();

    // ADMIN/GENERAL 담당 현장 목록 조회
    List<Site> findActiveSitesByUserId(@Param("userId") Long userId);

    // ADMIN/GENERAL 담당 현장 배정 여부 확인
    boolean existsActiveSiteAssignment(@Param("userId") Long userId, @Param("siteId") Long siteId);

    // 현장명 중복 확인. 삭제된 현장 이름은 재사용 가능하므로 활성 현장만 비교
    boolean existsActiveSiteByName(@Param("name") String name, @Param("excludeSiteId") Long excludeSiteId);

    // 현장 기본 정보 수정
    int updateSite(Site site);

    // 현장 소프트 삭제
    int softDeleteSite(@Param("siteId") Long siteId);

    // 현장/분전반/회로 변경 감사 로그 저장
    void insertFacilityAuditLog(FacilityAuditLog auditLog);

    // 현장/분전반/회로 변경 감사 로그 조회
    List<FacilityAuditLog> findFacilityAuditLogs(@Param("targetType") String targetType,
                                                 @Param("targetId") Long targetId,
                                                 @Param("actorUserId") Long actorUserId,
                                                 @Param("action") String action,
                                                 @Param("fromAt") LocalDateTime fromAt,
                                                 @Param("toAt") LocalDateTime toAt,
                                                 @Param("size") int size,
                                                 @Param("offset") int offset);

    // 현장/분전반/회로 변경 감사 로그 전체 개수 조회
    long countFacilityAuditLogs(@Param("targetType") String targetType,
                                @Param("targetId") Long targetId,
                                @Param("actorUserId") Long actorUserId,
                                @Param("action") String action,
                                @Param("fromAt") LocalDateTime fromAt,
                                @Param("toAt") LocalDateTime toAt);
}
