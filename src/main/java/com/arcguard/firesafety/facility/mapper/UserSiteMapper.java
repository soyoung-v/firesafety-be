package com.arcguard.firesafety.facility.mapper;

import com.arcguard.firesafety.facility.model.UserSiteAssignment;
import com.arcguard.firesafety.facility.model.UserSite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 사용자별 담당 현장(user_site) 배정/조회용 MyBatis Mapper
@Mapper
public interface UserSiteMapper {

    // 사용자에게 현재 배정된 활성 현장 조회
    List<UserSiteAssignment> findActiveAssignmentsByUserId(@Param("userId") Long userId);

    // ADMIN이 관리 가능한 현장 범위 안에서 대상 사용자의 담당 현장 조회
    List<UserSiteAssignment> findActiveAssignmentsByUserIdWithinManagerSites(@Param("userId") Long userId,
                                                                              @Param("managerUserId") Long managerUserId);

    // 사용자에게 생성된 모든 담당 현장 row 조회
    List<UserSite> findAssignmentsByUserId(@Param("userId") Long userId);

    // 요청된 현장이 모두 활성 현장인지 확인
    int countActiveSitesBySiteIds(@Param("siteIds") List<Long> siteIds);

    // ADMIN이 요청된 현장 전체에 접근 가능한지 확인
    int countActiveAssignmentsByUserIdAndSiteIds(@Param("userId") Long userId,
                                                 @Param("siteIds") List<Long> siteIds);

    // 신규 담당 현장 배정
    void insertAssignment(UserSite userSite);

    // 해제된 담당 현장 재배정
    int reactivateAssignment(@Param("userId") Long userId, @Param("siteId") Long siteId);

    // 선택 목록에서 빠진 기존 담당 현장 해제
    int softDeleteAssignmentsNotIn(@Param("userId") Long userId, @Param("siteIds") List<Long> siteIds);

    // ADMIN이 관리 가능한 현장 범위 안에서만 담당 현장 해제
    int softDeleteAssignmentsNotInWithinManagerSites(@Param("userId") Long userId,
                                                     @Param("siteIds") List<Long> siteIds,
                                                     @Param("managerUserId") Long managerUserId);
}
