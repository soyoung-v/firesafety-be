package com.arcguard.firesafety.facility.service;

import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.SiteAssignmentSaveReq;
import com.arcguard.firesafety.facility.dto.res.SiteAssignmentRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.UserSiteMapper;
import com.arcguard.firesafety.facility.model.UserSite;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteAssignmentService {

    // 사용자 상태와 역할 확인
    private final AuthMapper authMapper;

    // user_site 담당 현장 배정 조회
    private final UserSiteMapper userSiteMapper;

    // 담당 현장 조회
    // 1. 현재 사용자 확인 → 2. 대상 사용자 확인 → 3. 관리 권한 확인 → 4. 권한 범위 안의 활성 배정 조회
    @Transactional(readOnly = true)
    public List<SiteAssignmentRes> getSiteAssignments(Long userId) {
        UserPrincipal actor = getCurrentUser();
        validateUserId(userId);

        User targetUser = findActiveTargetUser(userId);
        validateManageableTarget(actor, targetUser);

        return findVisibleAssignments(actor, userId);
    }

    // 담당 현장 저장
    // 1. 현재 사용자 확인 → 2. 대상 사용자/권한 확인 → 3. 현장 목록 검증 → 4. 신규/재배정/해제 처리 → 5. 결과 재조회
    @Transactional
    public List<SiteAssignmentRes> saveSiteAssignments(Long userId, SiteAssignmentSaveReq req) {
        UserPrincipal actor = getCurrentUser();
        validateUserId(userId);

        User targetUser = findActiveTargetUser(userId);
        validateManageableTarget(actor, targetUser);

        List<Long> requestedSiteIds = validateSaveRequest(req);
        validateActiveSites(requestedSiteIds);
        validateAssignableSites(actor, requestedSiteIds);

        Map<Long, UserSite> existingAssignments = userSiteMapper.findAssignmentsByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserSite::getSiteId, Function.identity()));

        for (Long siteId : requestedSiteIds) {
            saveOneAssignment(userId, siteId, existingAssignments.get(siteId));
        }

        softDeleteUnselectedAssignments(actor, userId, requestedSiteIds);

        return findVisibleAssignments(actor, userId);
    }

    // 조회자는 자신의 권한 범위 안에 있는 배정만 볼 수 있음
    private List<SiteAssignmentRes> findVisibleAssignments(UserPrincipal actor, Long userId) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return userSiteMapper.findActiveAssignmentsByUserId(userId)
                    .stream()
                    .map(SiteAssignmentRes::from)
                    .toList();
        }

        return userSiteMapper.findActiveAssignmentsByUserIdWithinManagerSites(userId, actor.getUserId())
                .stream()
                .map(SiteAssignmentRes::from)
                .toList();
    }

    // 담당 현장 저장 요청값 확인
    private List<Long> validateSaveRequest(SiteAssignmentSaveReq req) {
        if (req == null || req.getSiteIds() == null) {
            throw new BusinessException(FacilityErrorCode.SITE_IDS_REQUIRED);
        }

        if (req.getSiteIds().stream().anyMatch(siteId -> siteId == null)) {
            throw new BusinessException(FacilityErrorCode.INVALID_SITE_ID);
        }

        Set<Long> uniqueSiteIds = new LinkedHashSet<>(req.getSiteIds());
        if (uniqueSiteIds.size() != req.getSiteIds().size()) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_SITE_ID);
        }

        return req.getSiteIds();
    }

    // 요청된 현장이 모두 삭제되지 않은 현장인지 확인
    private void validateActiveSites(List<Long> siteIds) {
        if (siteIds.isEmpty()) {
            return;
        }

        int activeSiteCount = userSiteMapper.countActiveSitesBySiteIds(siteIds);
        if (activeSiteCount != siteIds.size()) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }
    }

    // ADMIN은 본인에게 배정된 현장만 다른 사용자에게 배정 가능
    private void validateAssignableSites(UserPrincipal actor, List<Long> siteIds) {
        if (siteIds.isEmpty() || UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return;
        }

        int accessibleSiteCount = userSiteMapper.countActiveAssignmentsByUserIdAndSiteIds(actor.getUserId(), siteIds);
        if (accessibleSiteCount != siteIds.size()) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // 현장 하나를 신규 배정하거나 기존 삭제 row를 재활성화
    private void saveOneAssignment(Long userId, Long siteId, UserSite existingAssignment) {
        if (existingAssignment == null) {
            UserSite userSite = new UserSite();
            userSite.setUserId(userId);
            userSite.setSiteId(siteId);
            userSiteMapper.insertAssignment(userSite);
            return;
        }

        if (existingAssignment.getDeletedAt() != null) {
            userSiteMapper.reactivateAssignment(userId, siteId);
        }
    }

    // 선택 목록에서 빠진 기존 담당 현장은 해제 처리
    private void softDeleteUnselectedAssignments(UserPrincipal actor, Long userId, List<Long> requestedSiteIds) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            userSiteMapper.softDeleteAssignmentsNotIn(userId, requestedSiteIds);
            return;
        }

        userSiteMapper.softDeleteAssignmentsNotInWithinManagerSites(userId, requestedSiteIds, actor.getUserId());
    }

    // 대상 사용자 ID 확인
    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
    }

    // 삭제되지 않은 사용자만 담당 현장 관리 대상
    private User findActiveTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null || targetUser.getDeletedAt() != null || targetUser.getAccountStatus() != UserAccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        return targetUser;
    }

    // 담당 현장 관리는 사용자 관리 권한과 같은 등급 규칙을 사용
    private void validateManageableTarget(UserPrincipal actor, User targetUser) {
        if (canManageTargetRole(actor, targetUser.getRole())) {
            return;
        }
        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    // SUPER_ADMIN은 ADMIN/GENERAL, ADMIN은 본인 배정 가능 현장 안에서 ADMIN/GENERAL 관리 가능
    private boolean canManageTargetRole(UserPrincipal actor, UserRole targetRole) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN) {
            return targetRole != UserRole.SUPER_ADMIN;
        }

        return actorRole == UserRole.ADMIN && targetRole != UserRole.SUPER_ADMIN;
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }
}
