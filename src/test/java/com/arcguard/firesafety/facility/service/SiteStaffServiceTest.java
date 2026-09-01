package com.arcguard.firesafety.facility.service;

import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.res.SiteManagedUserPageRes;
import com.arcguard.firesafety.facility.dto.res.SiteManagedUserRes;
import com.arcguard.firesafety.facility.dto.res.SiteStaffContactRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Site;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteStaffServiceTest {

    @Mock
    private SiteMapper siteMapper;

    @Mock
    private AuthMapper authMapper;

    private SiteStaffService siteStaffService;

    @BeforeEach
    void setUp() {
        siteStaffService = new SiteStaffService(siteMapper, authMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 현장 ADMIN과 GENERAL 직원을 함께 조회할 수 있다")
    void superAdminCanReadManagedAdminsAndGenerals() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(authMapper.findActiveSiteUsersByRolesPaged(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null, 20, 0))
                .thenReturn(List.of(user(2L, "관리자", UserRole.ADMIN), user(10L, "직원1", UserRole.GENERAL)));
        when(authMapper.countActiveSiteUsersByRoles(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null))
                .thenReturn(2L);

        // when
        SiteManagedUserPageRes result = siteStaffService.getManagedUsers(1L, null);

        // then
        assertThat(result.getContent()).extracting(SiteManagedUserRes::getRole)
                .containsExactly(UserRole.ADMIN, UserRole.GENERAL);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        verify(siteMapper, never()).existsActiveSiteAssignment(anyLong(), anyLong());
        verify(authMapper).findActiveSiteUsersByRolesPaged(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null, 20, 0);
    }

    @Test
    @DisplayName("ADMIN은 담당 현장의 ADMIN과 GENERAL 직원을 함께 조회할 수 있다")
    void assignedAdminCanReadManagedUsers() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 1L)).thenReturn(true);
        when(authMapper.findActiveSiteUsersByRolesPaged(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null, 20, 0))
                .thenReturn(List.of(user(2L, "관리자", UserRole.ADMIN), user(10L, "직원1", UserRole.GENERAL)));
        when(authMapper.countActiveSiteUsersByRoles(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null))
                .thenReturn(2L);

        // when
        SiteManagedUserPageRes result = siteStaffService.getManagedUsers(1L, null);

        // then
        assertThat(result.getContent()).extracting(SiteManagedUserRes::getRole)
                .containsExactly(UserRole.ADMIN, UserRole.GENERAL);
        verify(authMapper).findActiveSiteUsersByRolesPaged(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null, 20, 0);
    }

    @Test
    @DisplayName("현장 직원 목록 조회는 SUPER_ADMIN 역할을 요청하지 않는다")
    void managedUsersQueryExcludesSuperAdmins() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 1L)).thenReturn(true);
        when(authMapper.findActiveSiteUsersByRolesPaged(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null, 20, 0))
                .thenReturn(List.of());
        when(authMapper.countActiveSiteUsersByRoles(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null))
                .thenReturn(0L);

        // when
        SiteManagedUserPageRes result = siteStaffService.getManagedUsers(1L, null);

        // then
        assertThat(result.getContent()).isEmpty();
        verify(authMapper).findActiveSiteUsersByRolesPaged(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null, 20, 0);
        verify(authMapper, never()).findActiveUsers();
    }

    @Test
    @DisplayName("ADMIN이 미배정 현장의 직원 목록을 조회하면 403을 반환한다")
    void unassignedAdminCannotReadManagedUsers() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> siteStaffService.getManagedUsers(1L, null))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verify(authMapper, never()).findActiveSiteUsersByRolesPaged(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GENERAL은 담당 현장의 ADMIN과 GENERAL 직원을 조회할 수 있다")
    void assignedGeneralCanReadManagedUsers() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(3L, 1L)).thenReturn(true);
        when(authMapper.findActiveSiteUsersByRolesPaged(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null, 20, 0))
                .thenReturn(List.of(user(2L, "관리자", UserRole.ADMIN), user(3L, "직원", UserRole.GENERAL)));
        when(authMapper.countActiveSiteUsersByRoles(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name()), null))
                .thenReturn(2L);

        // when
        SiteManagedUserPageRes result = siteStaffService.getManagedUsers(1L, null);

        // then
        assertThat(result.getContent()).extracting(SiteManagedUserRes::getRole)
                .containsExactly(UserRole.ADMIN, UserRole.GENERAL);
    }

    @Test
    @DisplayName("GENERAL이 미배정 현장의 직원 목록을 조회하면 403을 반환한다")
    void unassignedGeneralCannotReadManagedUsers() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(3L, 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> siteStaffService.getManagedUsers(1L, null))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verify(authMapper, never()).findActiveSiteUsersByRolesPaged(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GENERAL은 같은 현장 직원 연락망을 조회할 수 있고 응답에 이메일이 포함되지 않는다")
    void assignedGeneralCanReadStaffContacts() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(3L, 1L)).thenReturn(true);
        when(authMapper.findActiveSiteUsersByRoles(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name())))
                .thenReturn(List.of(user(10L, "직원1", UserRole.GENERAL), user(2L, "관리자", UserRole.ADMIN)));

        // when
        List<SiteStaffContactRes> result = siteStaffService.getStaffContacts(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSiteName()).isEqualTo("아크타워1");
        assertThat(result.get(0).getPhone()).isEqualTo("01012345678");
        // 응답 DTO 자체에 이메일 필드가 없어야 GENERAL에게 관리용 정보가 새지 않는다
        assertThat(SiteStaffContactRes.class.getDeclaredFields())
                .noneMatch(field -> field.getName().toLowerCase().contains("email"));
    }

    @Test
    @DisplayName("GENERAL이 다른 현장 연락망을 조회하면 403을 반환한다")
    void unassignedGeneralCannotReadStaffContacts() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(2L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(3L, 2L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> siteStaffService.getStaffContacts(2L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verify(authMapper, never()).findActiveSiteUsersByRoles(any(), any());
    }

    @Test
    @DisplayName("삭제된 현장의 연락망을 조회하면 404를 반환한다")
    void deletedSiteStaffContactsFails() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(9L)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> siteStaffService.getStaffContacts(9L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.SITE_NOT_FOUND));
    }

    @Test
    @DisplayName("현장 직원 조회 SQL은 삭제 사용자와 삭제 배정과 삭제 현장을 제외한다")
    void managedUsersSqlExcludesDeletedRows() throws IOException {
        // given
        String sql = authMapperXml();

        // when & then
        assertThat(sql).contains("us.deleted_at IS NULL");
        assertThat(sql).contains("s.deleted_at IS NULL");
        assertThat(sql).contains("u.deleted_at IS NULL");
        assertThat(sql).contains("u.account_status = 'ACTIVE'");
    }

    @Test
    @DisplayName("현장 직원 조회 SQL은 다른 현장 사용자를 포함하지 않고 중복 반환을 방지한다")
    void managedUsersSqlExcludesOtherSitesAndDuplicates() throws IOException {
        // given
        String sql = authMapperXml();

        // when & then
        assertThat(sql).contains("SELECT DISTINCT");
        assertThat(sql).contains("WHERE us.site_id = #{siteId}");
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Site site() {
        Site site = new Site();
        site.setSiteId(1L);
        site.setName("아크타워1");
        site.setAddress("서울시 강남구");
        return site;
    }

    private User user(Long userId, String name, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setName(name);
        user.setEmail(name + "@example.com");
        user.setPhone("01012345678");
        user.setRole(role);
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        return user;
    }

    private String authMapperXml() throws IOException {
        return Files.readString(Path.of("src/main/resources/mapper/auth/AuthMapper.xml"));
    }
}
