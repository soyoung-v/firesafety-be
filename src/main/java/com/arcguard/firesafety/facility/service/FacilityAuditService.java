package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.FacilityAuditLogSearchReq;
import com.arcguard.firesafety.facility.dto.res.FacilityAuditLogPageRes;
import com.arcguard.firesafety.facility.dto.res.FacilityAuditLogRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.FacilityAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacilityAuditService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    // facility_audit_log 조회는 기존 SiteMapper의 공통 감사 로그 Mapper를 사용
    private final SiteMapper siteMapper;

    // DB에 문자열로 저장한 before/after JSON을 응답용 JsonNode로 변환
    private final ObjectMapper objectMapper;

    // 설비 감사 이력 조회
    // 1. 현재 사용자 확인 → 2. SUPER_ADMIN 권한 확인 → 3. 필터 계산 → 4. 목록/개수 조회
    @Transactional(readOnly = true)
    public FacilityAuditLogPageRes getAuditLogs(FacilityAuditLogSearchReq req) {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);

        FacilityAuditLogSearchReq searchReq = normalizeReq(req);
        int page = resolvePage(searchReq);
        int size = resolveSize(searchReq);
        int offset = page * size;

        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);
        validateDateRange(searchReq);

        String targetType = searchReq.getTargetType() == null ? null : searchReq.getTargetType().name();
        String action = searchReq.getAction() == null ? null : searchReq.getAction().name();

        List<FacilityAuditLog> auditLogs = siteMapper.findFacilityAuditLogs(
                targetType,
                searchReq.getTargetId(),
                searchReq.getActorUserId(),
                action,
                fromAt,
                toAt,
                size,
                offset
        );
        long totalElements = siteMapper.countFacilityAuditLogs(
                targetType,
                searchReq.getTargetId(),
                searchReq.getActorUserId(),
                action,
                fromAt,
                toAt
        );

        List<FacilityAuditLogRes> content = auditLogs.stream()
                .map(this::toResponse)
                .toList();

        return new FacilityAuditLogPageRes(content, totalElements, page, size);
    }

    // null 요청도 기본 검색 조건으로 처리
    private FacilityAuditLogSearchReq normalizeReq(FacilityAuditLogSearchReq req) {
        return req == null ? new FacilityAuditLogSearchReq() : req;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePage(FacilityAuditLogSearchReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 과도한 요청은 100개로 제한
    private int resolveSize(FacilityAuditLogSearchReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return req.getSize();
    }

    // 조회 시작일은 해당 날짜 00:00:00 포함
    private LocalDateTime toStartDateTime(FacilityAuditLogSearchReq req) {
        return req.getFrom() == null ? null : req.getFrom().atStartOfDay();
    }

    // 조회 종료일은 다음날 00:00:00 미만으로 계산해서 하루 전체를 포함
    private LocalDateTime toEndDateTime(FacilityAuditLogSearchReq req) {
        return req.getTo() == null ? null : req.getTo().plusDays(1).atStartOfDay();
    }

    // 시작일이 종료일보다 늦으면 잘못된 기간 조건
    private void validateDateRange(FacilityAuditLogSearchReq req) {
        if (req.getFrom() != null && req.getTo() != null && req.getFrom().isAfter(req.getTo())) {
            throw new BusinessException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    // DB 모델을 API 응답 DTO로 변환
    private FacilityAuditLogRes toResponse(FacilityAuditLog auditLog) {
        return FacilityAuditLogRes.from(
                auditLog,
                toJsonObject(auditLog.getBeforeData()),
                toJsonObject(auditLog.getAfterData())
        );
    }

    // 감사 로그 JSON 문자열을 프론트에서 바로 쓰기 쉬운 JSON 객체로 변환.
    // JsonNode로 반환하면 Spring Boot 4 기본 Jackson(3.x)이 이 값을(Jackson 2 타입이라) 트리로 인식 못 하고
    // is*() 게터를 그대로 직렬화해버려서(예: {"array":false,...}) 일반 Map/List/원시값 구조로 반환한다.
    private Object toJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("설비 감사 로그 역직렬화 실패", e);
        }
    }

    // SUPER_ADMIN만 설비 감사 이력 조회 가능
    private void requireSuperAdmin(UserPrincipal actor) {
        if (!UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
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
