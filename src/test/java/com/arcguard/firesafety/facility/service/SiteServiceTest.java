package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.SiteCreateReq;
import com.arcguard.firesafety.facility.dto.res.SiteCreateRes;
import com.arcguard.firesafety.facility.dto.res.SiteDetailRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock
    private SiteMapper siteMapper;

    @Mock
    private PanelMapper panelMapper;

    private SiteService siteService;

    @BeforeEach
    void setUp() {
        siteService = new SiteService(siteMapper, panelMapper, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FAC-003: SUPER_ADMIN은 현장을 등록할 수 있다")
    void superAdminCanCreateSite() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        SiteCreateReq req = new SiteCreateReq("아크타워1", "서울시 강남구", null, "06134");
        when(siteMapper.findActiveSiteById(any())).thenReturn(savedSite());

        // when
        SiteCreateRes result = siteService.createSite(req);

        // then
        assertThat(result.getName()).isEqualTo("아크타워1");
        verify(siteMapper).insertSite(any());
        verify(siteMapper).insertFacilityAuditLog(any());
    }

    @Test
    @DisplayName("FAC-003: ADMIN이 현장 등록을 시도하면 403을 반환한다")
    void adminCannotCreateSite() {
        // given
        loginAs(2L, UserRole.ADMIN);
        SiteCreateReq req = new SiteCreateReq("아크타워1", "서울시 강남구", null, "06134");

        // when & then
        assertThatThrownBy(() -> siteService.createSite(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("FAC-003: GENERAL이 현장 등록을 시도하면 403을 반환한다")
    void generalCannotCreateSite() {
        // given
        loginAs(3L, UserRole.GENERAL);
        SiteCreateReq req = new SiteCreateReq("아크타워1", "서울시 강남구", null, "06134");

        // when & then
        assertThatThrownBy(() -> siteService.createSite(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("현장 이름이 없으면 400을 반환한다")
    void blankNameFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        SiteCreateReq req = new SiteCreateReq("", "서울시 강남구", null, "06134");

        // when & then
        assertThatThrownBy(() -> siteService.createSite(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.SITE_NAME_REQUIRED));
    }

    @Test
    @DisplayName("현장 주소가 없으면 400을 반환한다")
    void blankAddressFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        SiteCreateReq req = new SiteCreateReq("아크타워1", "", null, "06134");

        // when & then
        assertThatThrownBy(() -> siteService.createSite(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.SITE_ADDRESS_REQUIRED));
    }

    @Test
    @DisplayName("활성 현장과 이름이 겹치면 409를 반환하고 현장을 저장하지 않는다")
    void duplicatedActiveSiteNameFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        SiteCreateReq req = new SiteCreateReq("아크타워1", "서울시 강남구", null, "06134");
        when(siteMapper.existsActiveSiteByName("아크타워1", null)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> siteService.createSite(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.DUPLICATED_SITE_NAME));
        verify(siteMapper, never()).insertSite(any());
    }

    @Test
    @DisplayName("SUPER_ADMIN은 배정 여부와 관계없이 현장 상세를 조회할 수 있다")
    void superAdminCanReadSiteDetail() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(savedSite());
        when(panelMapper.countActivePanelsBySiteId(1L)).thenReturn(3);

        // when
        SiteDetailRes result = siteService.getSite(1L);

        // then
        assertThat(result.getSiteId()).isEqualTo(1L);
        assertThat(result.getPanelCount()).isEqualTo(3);
        verify(siteMapper, never()).existsActiveSiteAssignment(any(), any());
    }

    @Test
    @DisplayName("배정된 ADMIN은 현장 상세를 조회할 수 있다")
    void assignedAdminCanReadSiteDetail() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(savedSite());
        when(siteMapper.existsActiveSiteAssignment(2L, 1L)).thenReturn(true);
        when(panelMapper.countActivePanelsBySiteId(1L)).thenReturn(0);

        // when
        SiteDetailRes result = siteService.getSite(1L);

        // then
        assertThat(result.getName()).isEqualTo("아크타워1");
    }

    @Test
    @DisplayName("미배정 현장의 상세를 조회하면 403을 반환한다")
    void unassignedUserCannotReadSiteDetail() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(savedSite());
        when(siteMapper.existsActiveSiteAssignment(3L, 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> siteService.getSite(1L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("삭제된 현장의 상세를 조회하면 404를 반환한다")
    void deletedSiteDetailFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(siteMapper.findActiveSiteById(9L)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> siteService.getSite(9L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.SITE_NOT_FOUND));
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Site savedSite() {
        Site site = new Site();
        site.setSiteId(1L);
        site.setName("아크타워1");
        site.setAddress("서울시 강남구");
        return site;
    }
}
