package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.CircuitCreateReq;
import com.arcguard.firesafety.facility.dto.req.CircuitUpdateReq;
import com.arcguard.firesafety.facility.dto.res.CircuitCreateRes;
import com.arcguard.firesafety.facility.dto.res.CircuitDetailRes;
import com.arcguard.firesafety.facility.dto.res.CircuitListRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.CircuitMapper;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.Circuit;
import com.arcguard.firesafety.facility.model.FacilityAuditAction;
import com.arcguard.firesafety.facility.model.FacilityAuditLog;
import com.arcguard.firesafety.facility.model.FacilityAuditTargetType;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.Site;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CircuitService {

    // circuit 테이블 접근
    private final CircuitMapper circuitMapper;

    // panel 테이블 접근
    private final PanelMapper panelMapper;

    // site, user_site, facility_audit_log 테이블 접근
    private final SiteMapper siteMapper;

    // 감사 로그 after JSON 변환
    private final ObjectMapper objectMapper;

    // 회로 등록
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 분전반/현장 권한 확인 → 4. 채널 검증 → 5. 회로 저장 → 6. 감사 로그 저장
    @Transactional
    public CircuitCreateRes createCircuit(Long panelId, CircuitCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validateCreateRequest(panelId, req);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());
        validateChannelNo(panel, req.getChannelNo());

        // 삭제 이력까지 포함해서 조회 — 같은 채널번호로 삭제된 회로가 있으면 새로 만들지 않고 재활성화한다.
        // DB에 panel_id+channel_no UNIQUE 제약이 있어서, 삭제된 row가 남아있는 채로 새 row를 INSERT하면
        // 항상 제약 위반이 난다(소프트 삭제와 DB UNIQUE는 같이 못 씀).
        Circuit existing = circuitMapper.findCircuitByPanelIdAndChannelNo(panelId, req.getChannelNo());
        Circuit savedCircuit;
        if (existing != null && existing.getDeletedAt() == null) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_CHANNEL_NO);
        } else if (existing != null) {
            circuitMapper.reactivateCircuit(existing.getCircuitId(), req.getLoadType());
            savedCircuit = findActiveCircuit(existing.getCircuitId());
        } else {
            Circuit circuit = buildCircuitForCreate(panelId, req);
            circuitMapper.insertCircuit(circuit);
            savedCircuit = findActiveCircuit(circuit.getCircuitId());
        }

        insertFacilityAuditLog(savedCircuit, actor.getUserId(), FacilityAuditAction.CREATE, null, toAuditJson(savedCircuit));

        return CircuitCreateRes.from(savedCircuit);
    }

    // 분전반 등록 시 채널 1~circuitCount까지 loadType 없이 회로 일괄 생성 (PanelService에서 호출)
    // 이미 권한/현장 검증이 끝난 신규 분전반 대상이라 별도 검증 없이 바로 생성한다.
    @Transactional
    public void createCircuitsForPanel(Long panelId, int circuitCount, Long actorUserId) {
        for (int channelNo = 1; channelNo <= circuitCount; channelNo++) {
            Circuit circuit = new Circuit();
            circuit.setPanelId(panelId);
            circuit.setChannelNo(channelNo);
            circuitMapper.insertCircuit(circuit);

            Circuit savedCircuit = findActiveCircuit(circuit.getCircuitId());
            insertFacilityAuditLog(savedCircuit, actorUserId, FacilityAuditAction.CREATE, null, toAuditJson(savedCircuit));
        }
    }

    // 분전반 삭제 시 소속 회로도 함께 소프트 삭제 (PanelService.deletePanel()에서 호출)
    // 물리 삭제는 하지 않는다 — sensor_frame_circuit/ai_diagnosis_result/alert가 circuit을
    // FK ON DELETE RESTRICT로 참조하고 있어 이력 보존을 위해서도 소프트 삭제가 맞다.
    @Transactional
    public void deleteCircuitsForPanel(Long panelId, Long actorUserId) {
        List<Circuit> circuits = circuitMapper.findActiveCircuitsByPanelId(panelId);
        for (Circuit circuit : circuits) {
            String beforeData = toAuditJson(circuit);
            circuitMapper.softDeleteCircuit(circuit.getCircuitId());

            circuit.setDeletedAt(LocalDateTime.now());
            circuit.setUpdatedAt(LocalDateTime.now());
            insertFacilityAuditLog(circuit, actorUserId, FacilityAuditAction.DELETE, beforeData, toAuditJson(circuit));
        }
    }

    // 회로 부하종류 수정
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 회로/분전반/현장 권한 확인 → 4. loadType 수정 → 5. 감사 로그 저장
    @Transactional
    public CircuitDetailRes updateCircuit(Long circuitId, CircuitUpdateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validateCircuitId(circuitId);
        if (StringUtils.hasText(req == null ? null : req.getLoadType()) && req.getLoadType().length() > 50) {
            throw new BusinessException(FacilityErrorCode.LOAD_TYPE_TOO_LONG);
        }

        Circuit circuit = findActiveCircuit(circuitId);
        Panel panel = findActivePanel(circuit.getPanelId());
        validateSiteAccess(actor, panel.getSiteId());

        String beforeData = toAuditJson(circuit);
        circuitMapper.updateCircuitLoadType(circuitId, req == null ? null : req.getLoadType());

        Circuit updatedCircuit = findActiveCircuit(circuitId);
        String afterData = toAuditJson(updatedCircuit);
        if (!beforeData.equals(afterData)) {
            insertFacilityAuditLog(updatedCircuit, actor.getUserId(), FacilityAuditAction.UPDATE, beforeData, afterData);
        }

        return CircuitDetailRes.from(updatedCircuit);
    }

    // 회로 목록 조회
    // 1. 현재 사용자 확인 → 2. 분전반/현장 권한 확인 → 3. 삭제되지 않은 회로만 조회
    @Transactional(readOnly = true)
    public List<CircuitListRes> getCircuits(Long panelId) {
        UserPrincipal actor = getCurrentUser();
        validatePanelId(panelId);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        return circuitMapper.findActiveCircuitsByPanelId(panelId)
                .stream()
                .map(CircuitListRes::from)
                .toList();
    }

    // 회로 상세 조회
    // 회로 기준으로 상위 분전반과 현장을 다시 확인해서 삭제된 상위 설비 접근을 차단
    @Transactional(readOnly = true)
    public CircuitDetailRes getCircuit(Long circuitId) {
        UserPrincipal actor = getCurrentUser();
        validateCircuitId(circuitId);

        Circuit circuit = findActiveCircuit(circuitId);
        Panel panel = findActivePanel(circuit.getPanelId());
        validateSiteAccess(actor, panel.getSiteId());

        return CircuitDetailRes.from(circuit);
    }

    // 회로 소프트 삭제
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 회로/분전반/현장 권한 확인 → 4. deleted_at 기록 → 5. 감사 로그 저장
    @Transactional
    public void deleteCircuit(Long circuitId) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validateCircuitId(circuitId);

        Circuit circuit = findActiveCircuit(circuitId);
        Panel panel = findActivePanel(circuit.getPanelId());
        validateSiteAccess(actor, panel.getSiteId());
        String beforeData = toAuditJson(circuit);

        int updatedRows = circuitMapper.softDeleteCircuit(circuitId);
        if (updatedRows == 0) {
            throw new BusinessException(FacilityErrorCode.CIRCUIT_NOT_FOUND);
        }

        // 삭제 후 일반 조회에서 빠지므로 감사 로그용 상태는 메모리에서 반영
        circuit.setDeletedAt(LocalDateTime.now());
        circuit.setUpdatedAt(LocalDateTime.now());
        insertFacilityAuditLog(circuit, actor.getUserId(), FacilityAuditAction.DELETE, beforeData, toAuditJson(circuit));
    }

    // 등록 요청값 확인
    private void validateCreateRequest(Long panelId, CircuitCreateReq req) {
        if (panelId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
        if (req == null || req.getChannelNo() == null) {
            throw new BusinessException(FacilityErrorCode.CHANNEL_NO_REQUIRED);
        }

        if (StringUtils.hasText(req.getLoadType()) && req.getLoadType().length() > 50) {
            throw new BusinessException(FacilityErrorCode.LOAD_TYPE_TOO_LONG);
        }
    }

    // 목록 조회용 분전반 ID 확인
    private void validatePanelId(Long panelId) {
        if (panelId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
    }

    // 상세 조회용 회로 ID 확인
    private void validateCircuitId(Long circuitId) {
        if (circuitId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
    }

    // 분전반 회로 개수와 물리 최대값 기준 검증
    private void validateChannelNo(Panel panel, Integer channelNo) {
        if (channelNo < 1 || channelNo > 10 || channelNo > panel.getCircuitCount()) {
            throw new BusinessException(FacilityErrorCode.INVALID_CHANNEL_NO);
        }
    }

    // 등록용 Circuit 객체 생성
    private Circuit buildCircuitForCreate(Long panelId, CircuitCreateReq req) {
        Circuit circuit = new Circuit();
        circuit.setPanelId(panelId);
        circuit.setChannelNo(req.getChannelNo());
        circuit.setLoadType(req.getLoadType());
        return circuit;
    }

    // 활성 분전반 조회
    private Panel findActivePanel(Long panelId) {
        Panel panel = panelMapper.findActivePanelById(panelId);
        if (panel == null) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }
        return panel;
    }

    // 등록 직후 활성 회로 재조회
    private Circuit findActiveCircuit(Long circuitId) {
        Circuit circuit = circuitMapper.findActiveCircuitById(circuitId);
        if (circuit == null) {
            throw new BusinessException(FacilityErrorCode.CIRCUIT_NOT_FOUND);
        }
        return circuit;
    }

    // 현장 접근 권한 확인
    private void validateSiteAccess(UserPrincipal actor, Long siteId) {
        Site site = siteMapper.findActiveSiteById(siteId);
        if (site == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }

        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return;
        }

        // ADMIN은 본인에게 배정된 활성 현장 소속 분전반에만 회로 등록 가능
        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // ADMIN 이상 권한 확인
    private void requireAdminOrSuperAdmin(UserPrincipal actor) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole()) || UserRole.ADMIN.name().equals(actor.getRole())) {
            return;
        }
        throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }

    // 설비 감사 로그 저장
    private void insertFacilityAuditLog(Circuit circuit,
                                        Long actorUserId,
                                        FacilityAuditAction action,
                                        String beforeData,
                                        String afterData) {
        FacilityAuditLog auditLog = new FacilityAuditLog();
        auditLog.setTargetType(FacilityAuditTargetType.CIRCUIT);
        auditLog.setTargetId(circuit.getCircuitId());
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        siteMapper.insertFacilityAuditLog(auditLog);
    }

    // 감사 로그 before/after JSON 생성
    private String toAuditJson(Circuit circuit) {
        try {
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("circuitId", circuit.getCircuitId());
            auditData.put("panelId", circuit.getPanelId());
            auditData.put("channelNo", circuit.getChannelNo());
            auditData.put("loadType", circuit.getLoadType());
            auditData.put("createdAt", circuit.getCreatedAt());
            auditData.put("updatedAt", circuit.getUpdatedAt());
            auditData.put("deletedAt", circuit.getDeletedAt());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("설비 감사 로그 직렬화 실패", e);
        }
    }
}
