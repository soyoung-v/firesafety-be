package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.auth.service.UserService;
import com.arcguard.firesafety.auth.validation.CredentialPolicy;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.SiteWithAdminCreateReq;
import com.arcguard.firesafety.facility.dto.res.SiteWithAdminCreateRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.mapper.UserSiteMapper;
import com.arcguard.firesafety.facility.model.Site;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteRegistrationServiceTest {

    // 실제 비밀번호가 아닌 형식 검증용 더미값
    private static final String DUMMY_PASSWORD = "testpass1";

    private static final Long SITE_ID = 1L;
    private static final Long ADMIN_USER_ID = 10L;

    @Mock
    private SiteMapper siteMapper;

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private UserSiteMapper userSiteMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SiteRegistrationService siteRegistrationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 통합 등록은 실제 SiteService/UserService/SiteAssignmentService 조합을 그대로 검증한다
        SiteService siteService = new SiteService(siteMapper, panelMapper, objectMapper);
        UserService userService = new UserService(authMapper, passwordEncoder, objectMapper, new CredentialPolicy());
        SiteAssignmentService siteAssignmentService = new SiteAssignmentService(authMapper, userSiteMapper);
        siteRegistrationService = new SiteRegistrationService(siteService, userService, siteAssignmentService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 현장과 현장관리자를 한 번에 등록하고 담당 현장까지 배정된다")
    void superAdminCanCreateSiteWithAdmin() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("site-admin@example.com")).thenReturn(false);
        when(siteMapper.existsActiveSiteByName("아크타워1", null)).thenReturn(false);
        when(siteMapper.findActiveSiteById(any())).thenReturn(savedSite());
        when(passwordEncoder.encode(DUMMY_PASSWORD)).thenReturn("encoded-password");
        stubInsertUserId();
        when(authMapper.findUserById(ADMIN_USER_ID)).thenReturn(savedAdmin());
        when(userSiteMapper.countActiveSitesBySiteIds(List.of(SITE_ID))).thenReturn(1);
        when(userSiteMapper.findAssignmentsByUserId(ADMIN_USER_ID)).thenReturn(List.of());
        when(userSiteMapper.findActiveAssignmentsByUserId(ADMIN_USER_ID)).thenReturn(List.of(savedAssignment()));

        // when
        SiteWithAdminCreateRes result = siteRegistrationService.createSiteWithAdmin(req());

        // then
        assertThat(result.getSiteId()).isEqualTo(SITE_ID);
        assertThat(result.getSiteName()).isEqualTo("아크타워1");
        assertThat(result.getAdminUserId()).isEqualTo(ADMIN_USER_ID);
        assertThat(result.getAdminEmail()).isEqualTo("site-admin@example.com");
        assertThat(result.getAssignedSiteIds()).containsExactly(SITE_ID);
        verify(siteMapper).insertSite(any());
        verify(authMapper).insertUser(any());
        verify(userSiteMapper).insertAssignment(any());
        // 현장/계정 이력이 각각 남는지 확인
        verify(siteMapper).insertFacilityAuditLog(any());
        verify(authMapper).insertUserAuditLog(any());
    }

    @Test
    @DisplayName("ADMIN이 통합 등록을 시도하면 403을 반환하고 아무것도 생성되지 않는다")
    void adminCannotCreateSiteWithAdmin() {
        // given
        loginAs(2L, UserRole.ADMIN);

        // when & then
        assertThatThrownBy(() -> siteRegistrationService.createSiteWithAdmin(req()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verifyNothingCreated();
    }

    @Test
    @DisplayName("GENERAL이 통합 등록을 시도하면 403을 반환하고 아무것도 생성되지 않는다")
    void generalCannotCreateSiteWithAdmin() {
        // given
        loginAs(3L, UserRole.GENERAL);

        // when & then
        assertThatThrownBy(() -> siteRegistrationService.createSiteWithAdmin(req()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verifyNothingCreated();
    }

    @Test
    @DisplayName("현장명이 중복되면 409를 반환하고 현장/계정이 모두 생성되지 않는다")
    void duplicatedSiteNameRollsBackEverything() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("site-admin@example.com")).thenReturn(false);
        when(siteMapper.existsActiveSiteByName("아크타워1", null)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> siteRegistrationService.createSiteWithAdmin(req()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.DUPLICATED_SITE_NAME));
        verifyNothingCreated();
    }

    @Test
    @DisplayName("관리자 이메일이 중복되면 409를 반환하고 현장 row도 만들어지지 않는다")
    void duplicatedAdminEmailRollsBackEverything() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("site-admin@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> siteRegistrationService.createSiteWithAdmin(req()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.DUPLICATED_EMAIL));
        verifyNothingCreated();
    }

    @Test
    @DisplayName("통합 등록은 한 트랜잭션 안에서 처리된다")
    void createSiteWithAdminIsTransactional() throws NoSuchMethodException {
        // given
        var method = SiteRegistrationService.class.getMethod("createSiteWithAdmin", SiteWithAdminCreateReq.class);

        // when
        Transactional transactional = method.getAnnotation(Transactional.class);

        // then
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isFalse();
    }

    private void verifyNothingCreated() {
        verify(siteMapper, never()).insertSite(any());
        verify(authMapper, never()).insertUser(any());
        verify(userSiteMapper, never()).insertAssignment(any());
    }

    // insertUser는 MyBatis useGeneratedKeys로 userId를 채우므로 mock에서도 동일하게 흉내낸다
    private void stubInsertUserId() {
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(ADMIN_USER_ID);
            return null;
        }).when(authMapper).insertUser(any());
    }

    private SiteWithAdminCreateReq req() {
        return new SiteWithAdminCreateReq(
                "아크타워1",
                "서울시 강남구",
                null,
                "06134",
                "김관리",
                "site-admin@example.com",
                DUMMY_PASSWORD,
                "01012345678");
    }

    private Site savedSite() {
        Site site = new Site();
        site.setSiteId(SITE_ID);
        site.setName("아크타워1");
        site.setAddress("서울시 강남구");
        site.setZipCode("06134");
        return site;
    }

    private User savedAdmin() {
        User user = new User();
        user.setUserId(ADMIN_USER_ID);
        user.setEmail("site-admin@example.com");
        user.setName("김관리");
        user.setRole(UserRole.ADMIN);
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        return user;
    }

    private UserSiteAssignment savedAssignment() {
        UserSiteAssignment assignment = new UserSiteAssignment();
        assignment.setMappingId(1L);
        assignment.setUserId(ADMIN_USER_ID);
        assignment.setSiteId(SITE_ID);
        assignment.setSiteName("아크타워1");
        return assignment;
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
