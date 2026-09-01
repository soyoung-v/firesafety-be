package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.FacilityAuditLogSearchReq;
import com.arcguard.firesafety.facility.dto.res.FacilityAuditLogPageRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.FacilityAuditAction;
import com.arcguard.firesafety.facility.model.FacilityAuditLog;
import com.arcguard.firesafety.facility.model.FacilityAuditTargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityAuditServiceTest {

    @Mock
    private SiteMapper siteMapper;

    private FacilityAuditService facilityAuditService;

    @BeforeEach
    void setUp() {
        facilityAuditService = new FacilityAuditService(siteMapper, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FAC-009: SUPER_ADMIN은 설비 감사 이력을 필터링해서 조회할 수 있다")
    void superAdminCanSearchAuditLogs() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        FacilityAuditLogSearchReq req = new FacilityAuditLogSearchReq();
        req.setTargetType(FacilityAuditTargetType.SITE);
        req.setActorUserId(1L);
        req.setAction(FacilityAuditAction.CREATE);
        req.setFrom(LocalDate.of(2026, 7, 23));
        req.setTo(LocalDate.of(2026, 7, 23));
        req.setPage(0);
        req.setSize(10);

        LocalDateTime fromAt = LocalDateTime.of(2026, 7, 23, 0, 0);
        LocalDateTime toAt = LocalDateTime.of(2026, 7, 24, 0, 0);
        FacilityAuditLog auditLog = auditLog();

        when(siteMapper.findFacilityAuditLogs("SITE", null, 1L, "CREATE", fromAt, toAt, 10, 0))
                .thenReturn(List.of(auditLog));
        when(siteMapper.countFacilityAuditLogs("SITE", null, 1L, "CREATE", fromAt, toAt))
                .thenReturn(1L);

        // when
        FacilityAuditLogPageRes result = facilityAuditService.getAuditLogs(req);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(((Map<?, ?>) result.getContent().get(0).getAfterData()).get("name")).isEqualTo("아크타워 본사");
        verify(siteMapper).findFacilityAuditLogs("SITE", null, 1L, "CREATE", fromAt, toAt, 10, 0);
    }

    @Test
    @DisplayName("FAC-009: SUPER_ADMIN이 아니면 설비 감사 이력을 조회할 수 없다")
    void nonSuperAdminCannotSearchAuditLogs() {
        // given
        loginAs(2L, UserRole.ADMIN);

        // when & then
        assertThatThrownBy(() -> facilityAuditService.getAuditLogs(new FacilityAuditLogSearchReq()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("FAC-009: 잘못된 페이징 조건이면 400을 반환한다")
    void invalidPageConditionFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        FacilityAuditLogSearchReq req = new FacilityAuditLogSearchReq();
        req.setPage(-1);

        // when & then
        assertThatThrownBy(() -> facilityAuditService.getAuditLogs(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_PAGE));
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private FacilityAuditLog auditLog() {
        FacilityAuditLog auditLog = new FacilityAuditLog();
        auditLog.setAuditId(1L);
        auditLog.setTargetType(FacilityAuditTargetType.SITE);
        auditLog.setTargetId(1L);
        auditLog.setActorUserId(1L);
        auditLog.setAction(FacilityAuditAction.CREATE);
        auditLog.setBeforeData(null);
        auditLog.setAfterData("{\"siteId\":1,\"name\":\"아크타워 본사\"}");
        auditLog.setCreatedAt(LocalDateTime.of(2026, 7, 23, 10, 0));
        return auditLog;
    }
}
