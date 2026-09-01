package com.arcguard.firesafety.facility.service;

import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.SiteAssignmentSaveReq;
import com.arcguard.firesafety.facility.dto.res.SiteAssignmentRes;
import com.arcguard.firesafety.facility.mapper.UserSiteMapper;
import com.arcguard.firesafety.facility.model.UserSite;
import com.arcguard.firesafety.facility.model.UserSiteAssignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteAssignmentServiceTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private UserSiteMapper userSiteMapper;

    private SiteAssignmentService siteAssignmentService;

    @BeforeEach
    void setUp() {
        siteAssignmentService = new SiteAssignmentService(authMapper, userSiteMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FAC-004: ADMIN은 담당 현장 안에서 GENERAL의 담당 현장을 배정한다")
    void adminAssignsSitesToGeneral() {
        // given
        loginAs(1L, UserRole.ADMIN);
        when(authMapper.findUserById(10L)).thenReturn(activeUser(10L, UserRole.GENERAL));
        when(userSiteMapper.countActiveSitesBySiteIds(List.of(1L))).thenReturn(1);
        when(userSiteMapper.countActiveAssignmentsByUserIdAndSiteIds(1L, List.of(1L))).thenReturn(1);
        when(userSiteMapper.findAssignmentsByUserId(10L)).thenReturn(List.of());
        when(userSiteMapper.findActiveAssignmentsByUserIdWithinManagerSites(10L, 1L))
                .thenReturn(List.of(assignment(1L, 10L, 1L)));

        SiteAssignmentSaveReq req = new SiteAssignmentSaveReq();
        req.setSiteIds(List.of(1L));

        // when
        List<SiteAssignmentRes> result = siteAssignmentService.saveSiteAssignments(10L, req);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSiteId()).isEqualTo(1L);
        verify(userSiteMapper).insertAssignment(any(UserSite.class));
        verify(userSiteMapper).softDeleteAssignmentsNotInWithinManagerSites(10L, List.of(1L), 1L);
    }

    @Test
    @DisplayName("FAC-004: ADMIN은 본인 담당이 아닌 현장을 다른 사용자에게 배정할 수 없다")
    void adminCannotAssignSiteOutsideOwnScope() {
        // given
        loginAs(1L, UserRole.ADMIN);
        when(authMapper.findUserById(10L)).thenReturn(activeUser(10L, UserRole.GENERAL));
        when(userSiteMapper.countActiveSitesBySiteIds(List.of(2L))).thenReturn(1);
        when(userSiteMapper.countActiveAssignmentsByUserIdAndSiteIds(1L, List.of(2L))).thenReturn(0);

        SiteAssignmentSaveReq req = new SiteAssignmentSaveReq();
        req.setSiteIds(List.of(2L));

        // when & then
        assertThatThrownBy(() -> siteAssignmentService.saveSiteAssignments(10L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(com.arcguard.firesafety.facility.exception.FacilityErrorCode.FORBIDDEN_ROLE));
        verify(userSiteMapper, never()).insertAssignment(any(UserSite.class));
    }

    @Test
    @DisplayName("ADMIN은 담당 현장 안에서 ADMIN의 담당 현장을 배정한다")
    void adminAssignsSitesToAdmin() {
        // given
        loginAs(1L, UserRole.ADMIN);
        when(authMapper.findUserById(20L)).thenReturn(activeUser(20L, UserRole.ADMIN));
        when(userSiteMapper.countActiveSitesBySiteIds(List.of(1L))).thenReturn(1);
        when(userSiteMapper.countActiveAssignmentsByUserIdAndSiteIds(1L, List.of(1L))).thenReturn(1);
        when(userSiteMapper.findAssignmentsByUserId(20L)).thenReturn(List.of());
        when(userSiteMapper.findActiveAssignmentsByUserIdWithinManagerSites(20L, 1L))
                .thenReturn(List.of(assignment(1L, 20L, 1L)));

        SiteAssignmentSaveReq req = new SiteAssignmentSaveReq();
        req.setSiteIds(List.of(1L));

        // when
        List<SiteAssignmentRes> result = siteAssignmentService.saveSiteAssignments(20L, req);

        // then
        assertThat(result).hasSize(1);
        verify(userSiteMapper).insertAssignment(any(UserSite.class));
        verify(userSiteMapper).softDeleteAssignmentsNotInWithinManagerSites(20L, List.of(1L), 1L);
    }

    @Test
    @DisplayName("ADMIN이 담당현장을 저장해도 본인 범위 밖 기존 배정은 보존한다")
    void adminPreservesOutOfScopeAssignmentsWhenSavingVisibleSites() {
        // given
        loginAs(1L, UserRole.ADMIN);
        when(authMapper.findUserById(20L)).thenReturn(activeUser(20L, UserRole.ADMIN));
        when(userSiteMapper.countActiveSitesBySiteIds(List.of(1L))).thenReturn(1);
        when(userSiteMapper.countActiveAssignmentsByUserIdAndSiteIds(1L, List.of(1L))).thenReturn(1);
        when(userSiteMapper.findAssignmentsByUserId(20L))
                .thenReturn(List.of(assignmentRow(20L, 1L), assignmentRow(20L, 2L)));
        when(userSiteMapper.findActiveAssignmentsByUserIdWithinManagerSites(20L, 1L))
                .thenReturn(List.of(assignment(1L, 20L, 1L)));

        SiteAssignmentSaveReq req = new SiteAssignmentSaveReq();
        req.setSiteIds(List.of(1L));

        // when
        siteAssignmentService.saveSiteAssignments(20L, req);

        // then
        verify(userSiteMapper).softDeleteAssignmentsNotInWithinManagerSites(20L, List.of(1L), 1L);
        verify(userSiteMapper, never()).softDeleteAssignmentsNotIn(any(), any());
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User activeUser(Long userId, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setRole(role);
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        return user;
    }

    private UserSiteAssignment assignment(Long mappingId, Long userId, Long siteId) {
        UserSiteAssignment assignment = new UserSiteAssignment();
        assignment.setMappingId(mappingId);
        assignment.setUserId(userId);
        assignment.setSiteId(siteId);
        assignment.setSiteName("아크타워1");
        return assignment;
    }

    private UserSite assignmentRow(Long userId, Long siteId) {
        UserSite userSite = new UserSite();
        userSite.setUserId(userId);
        userSite.setSiteId(siteId);
        return userSite;
    }
}
