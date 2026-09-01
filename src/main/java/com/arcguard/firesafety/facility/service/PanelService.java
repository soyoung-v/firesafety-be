package com.arcguard.firesafety.facility.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.dto.req.PanelCreateReq;
import com.arcguard.firesafety.facility.dto.req.PanelListReq;
import com.arcguard.firesafety.facility.dto.req.PanelUpdateReq;
import com.arcguard.firesafety.facility.dto.res.PanelDetailRes;
import com.arcguard.firesafety.facility.dto.res.PanelCircuitStatusRes;
import com.arcguard.firesafety.facility.dto.res.PanelRecentAlertRes;
import com.arcguard.firesafety.facility.dto.res.PanelCreateRes;
import com.arcguard.firesafety.facility.dto.res.PanelListPageRes;
import com.arcguard.firesafety.facility.dto.res.PanelListRes;
import com.arcguard.firesafety.facility.dto.res.PanelUpdateRes;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import com.arcguard.firesafety.facility.mapper.PanelMapper;
import com.arcguard.firesafety.facility.mapper.SiteMapper;
import com.arcguard.firesafety.facility.model.CircuitStatusRow;
import com.arcguard.firesafety.facility.model.FacilityAuditAction;
import com.arcguard.firesafety.facility.model.FacilityAuditLog;
import com.arcguard.firesafety.facility.model.FacilityAuditTargetType;
import com.arcguard.firesafety.facility.model.Panel;
import com.arcguard.firesafety.facility.model.PanelStatus;
import com.arcguard.firesafety.facility.model.Site;
import com.arcguard.firesafety.diagnosis.model.Verdict;
import com.arcguard.firesafety.sensor.model.SensorFrame;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PanelService {

    private static final BigDecimal DEFAULT_LEAK_MA_THRESHOLD = BigDecimal.valueOf(20.0);
    private static final BigDecimal DEFAULT_TEMP_THRESHOLD = BigDecimal.valueOf(80.0);
    private static final BigDecimal DEFAULT_HUMIDITY_THRESHOLD = BigDecimal.valueOf(80.0);
    private static final BigDecimal DEFAULT_OVERCURRENT_THRESHOLD = BigDecimal.valueOf(30.0);
    // 가스/불꽃 원시값 기준 방향(>=)만 확정, 정확한 수치는 하드웨어 확정 전까지 잠정값
    private static final Integer DEFAULT_GAS_THRESHOLD = 5000;
    private static final Integer DEFAULT_FIRE_THRESHOLD = 5000;
    private static final int RECENT_ALERT_LIMIT = 5;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    // panel 테이블 접근
    private final PanelMapper panelMapper;

    // site, user_site, facility_audit_log 테이블 접근
    private final SiteMapper siteMapper;

    // 분전반 등록 시 회로 자동 생성 위임
    private final CircuitService circuitService;

    // 감사 로그 after JSON 변환
    private final ObjectMapper objectMapper;

    // 분전반 등록
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 현장 권한 확인 → 4. 분전반 저장 → 5. 감사 로그 저장
    @Transactional
    public PanelCreateRes createPanel(Long siteId, PanelCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validateCreateRequest(siteId, req);
        validateSiteAccess(actor, siteId);

        if (panelMapper.existsPanelByDeviceSerial(req.getDeviceSerial())) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_DEVICE_SERIAL);
        }
        if (panelMapper.existsPanelByMNo(req.getMNo())) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_M_NO);
        }

        Panel panel = buildPanelForCreate(siteId, req);
        panelMapper.insertPanel(panel);

        Panel savedPanel = findActivePanel(panel.getPanelId());
        insertFacilityAuditLog(savedPanel, actor.getUserId(), FacilityAuditAction.CREATE, null, toAuditJson(savedPanel));

        // 회로는 loadType 없이 채널 1~circuitCount까지 자동 생성, 이후 프론트에서 채널별로 loadType만 수정
        circuitService.createCircuitsForPanel(savedPanel.getPanelId(), savedPanel.getCircuitCount(), actor.getUserId());

        return PanelCreateRes.from(savedPanel);
    }

    // 분전반 목록 조회
    // 1. 현재 사용자 확인 → 2. 현장/상태/검색어 필터 확인 → 3. 페이징 계산 → 4. 역할별 조회 범위 적용
    @Transactional(readOnly = true)
    public PanelListPageRes getPanels(PanelListReq req) {
        UserPrincipal actor = getCurrentUser();
        PanelListReq searchReq = req == null ? new PanelListReq() : req;
        Long siteId = searchReq.getSiteId();
        String status = searchReq.getStatus() == null ? null : searchReq.getStatus().name();
        String keyword = normalizeKeyword(searchReq.getKeyword());
        int page = resolvePage(searchReq);
        int size = resolveSize(searchReq);
        int offset = page * size;

        UserRole actorRole = UserRole.valueOf(actor.getRole());
        List<Panel> panels;
        long totalElements;
        if (actorRole == UserRole.SUPER_ADMIN) {
            panels = panelMapper.findActivePanels(siteId, status, keyword, size, offset);
            totalElements = panelMapper.countActivePanels(siteId, status, keyword);
        } else {
            // ADMIN/GENERAL은 배정된 현장 안에서만 조회
            panels = panelMapper.findActivePanelsByUserId(actor.getUserId(), siteId, status, keyword, size, offset);
            totalElements = panelMapper.countActivePanelsByUserId(actor.getUserId(), siteId, status, keyword);
        }

        List<PanelListRes> content = panels.stream()
                .map(PanelListRes::from)
                .toList();
        return new PanelListPageRes(content, totalElements);
    }

    // 공백만 입력한 검색어는 필터 없음으로 처리
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePage(PanelListReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolveSize(PanelListReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return req.getSize();
    }

    // 분전반 상세 조회
    // 1. 현재 사용자 확인 → 2. 활성 분전반 조회 → 3. 현장 접근 권한 확인 → 4. 최신 센서값/회로상태/최근경보 조립
    @Transactional(readOnly = true)
    public PanelDetailRes getPanel(Long panelId) {
        UserPrincipal actor = getCurrentUser();
        validatePanelId(panelId);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        SensorFrame latestFrame = panelMapper.findLatestSensorFrameByPanelId(panelId);
        List<PanelCircuitStatusRes> circuits = resolveCircuitStatuses(panel);
        List<PanelRecentAlertRes> recentAlerts = panelMapper.findRecentAlertsByPanelId(panelId, RECENT_ALERT_LIMIT);
        boolean[] alarmBits = resolveAlarmBits(latestFrame == null ? null : latestFrame.getErrorBits());

        return PanelDetailRes.from(
                panel,
                latestFrame == null ? null : latestFrame.getTotalCurrent(),
                latestFrame == null ? null : latestFrame.getLeakMa(),
                latestFrame == null ? null : latestFrame.getVoltV(),
                latestFrame == null ? null : latestFrame.getTotalPower(),
                latestFrame == null ? null : latestFrame.getDoorStatus(),
                alarmBits[5],
                latestFrame == null ? null : latestFrame.getTemperature(),
                latestFrame == null ? null : latestFrame.getHumidity(),
                latestFrame == null ? null : latestFrame.getFireRaw(),
                latestFrame == null ? null : latestFrame.getGasRaw(),
                alarmBits[0],
                alarmBits[1],
                alarmBits[2],
                alarmBits[3],
                alarmBits[4],
                alarmBits[6],
                circuits,
                recentAlerts
        );
    }

    // aerror ALARM byte(byte3)를 항목별 비트로 분해. 순서는 DeviceAlertService와 동일
    // (0=누전, 1=과열, 2=습도, 3=가스, 4=불꽃, 5=문열림, 6=과전류)
    // 화면 카드가 "서버 임계값 초과(노랑)"와 "하드웨어 알람(빨강)"을 구분해서 보여주기 위한 값
    private boolean[] resolveAlarmBits(String errorBits) {
        boolean[] bits = new boolean[7];
        if (errorBits == null || !errorBits.matches("^[0-9A-Fa-f]{8}$")) {
            return bits;
        }
        int byteValue = Integer.parseInt(errorBits.substring(6, 8), 16);
        for (int i = 0; i < 7; i++) {
            bits[i] = (byteValue & (1 << i)) != 0;
        }
        return bits;
    }

    // 회로별 상태 계산 (FR-03-03). 분전반이 OFFLINE이면 소속 회로 전부 OFFLINE으로 본다.
    private List<PanelCircuitStatusRes> resolveCircuitStatuses(Panel panel) {
        List<CircuitStatusRow> rows = panelMapper.findCircuitStatusRowsByPanelId(panel.getPanelId());
        return rows.stream()
                .map(row -> new PanelCircuitStatusRes(
                        row.getCircuitId(),
                        row.getChannelNo(),
                        row.getLoadType(),
                        row.getCurrentA(),
                        row.getArcCounter(),
                        resolveCircuitStatus(panel, row)
                ))
                .toList();
    }

    // 하드웨어 ARC가 위험, 하드웨어 정상+AI ARC는 주의, 둘 다 정상이면 정상
    private PanelStatus resolveCircuitStatus(Panel panel, CircuitStatusRow row) {
        if (panel.getStatus() == PanelStatus.OFFLINE) {
            return PanelStatus.OFFLINE;
        }
        if (Boolean.TRUE.equals(row.getDeviceArcFlag())) {
            return PanelStatus.RISK;
        }
        if (row.getLatestAiVerdict() == Verdict.ARC) {
            return PanelStatus.CAUTION;
        }
        return PanelStatus.NORMAL;
    }

    // 분전반 수정
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 분전반/현장 권한 확인 → 4. 수정 → 5. 감사 로그 저장
    @Transactional
    public PanelUpdateRes updatePanel(Long panelId, PanelUpdateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validateUpdateRequest(panelId, req);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        if (!panel.getDeviceSerial().equals(req.getDeviceSerial())
                && panelMapper.existsPanelByDeviceSerialExceptSelf(panelId, req.getDeviceSerial())) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_DEVICE_SERIAL);
        }
        if (!panel.getMNo().equals(req.getMNo())
                && panelMapper.existsPanelByMNoExceptSelf(panelId, req.getMNo())) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_M_NO);
        }

        String beforeData = toAuditJson(panel);
        applyUpdate(panel, req);

        int updatedRows = panelMapper.updatePanel(panel);
        if (updatedRows == 0) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }

        Panel updatedPanel = findActivePanel(panelId);
        String afterData = toAuditJson(updatedPanel);
        if (!beforeData.equals(afterData)) {
            insertFacilityAuditLog(updatedPanel, actor.getUserId(), FacilityAuditAction.UPDATE, beforeData, afterData);
        }

        return PanelUpdateRes.from(updatedPanel);
    }

    // 분전반 소프트 삭제
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 분전반/현장 권한 확인 → 4. deleted_at 기록 → 5. 감사 로그 저장
    @Transactional
    public void deletePanel(Long panelId) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validatePanelId(panelId);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());
        String beforeData = toAuditJson(panel);

        int updatedRows = panelMapper.softDeletePanel(panelId);
        if (updatedRows == 0) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }

        // 삭제 후 일반 조회에서 빠지므로 감사 로그용 상태는 메모리에서 반영
        panel.setDeletedAt(LocalDateTime.now());
        panel.setUpdatedAt(LocalDateTime.now());
        insertFacilityAuditLog(panel, actor.getUserId(), FacilityAuditAction.DELETE, beforeData, toAuditJson(panel));

        // 분전반이 사라지므로 소속 회로도 함께 소프트 삭제 (물리 삭제 아님 — 과거 이력 보존)
        circuitService.deleteCircuitsForPanel(panelId, actor.getUserId());
    }

    // 등록 요청값 확인
    private void validateCreateRequest(Long siteId, PanelCreateReq req) {
        if (siteId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
        if (req == null) {
            throw new BusinessException(FacilityErrorCode.PANEL_NAME_REQUIRED);
        }
        validatePanelFields(req.getName(), req.getDeviceSerial(), req.getMNo(), req.getCircuitCount());
    }

    // 수정 요청값 확인
    private void validateUpdateRequest(Long panelId, PanelUpdateReq req) {
        validatePanelId(panelId);
        if (req == null) {
            throw new BusinessException(FacilityErrorCode.PANEL_NAME_REQUIRED);
        }
        validatePanelFields(req.getName(), req.getDeviceSerial(), req.getMNo(), req.getCircuitCount());
    }

    // 분전반 등록/수정 공통 필드 확인
    private void validatePanelFields(String name, String deviceSerial, String mNo, Integer circuitCountReq) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(FacilityErrorCode.PANEL_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(deviceSerial)) {
            throw new BusinessException(FacilityErrorCode.DEVICE_SERIAL_REQUIRED);
        }
        if (!StringUtils.hasText(mNo) || mNo.length() != 5) {
            throw new BusinessException(FacilityErrorCode.INVALID_M_NO);
        }

        int circuitCount = circuitCountReq == null ? 10 : circuitCountReq;
        if (circuitCount < 1 || circuitCount > 10) {
            throw new BusinessException(FacilityErrorCode.INVALID_CIRCUIT_COUNT);
        }
    }

    // 분전반 ID 확인
    private void validatePanelId(Long panelId) {
        if (panelId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
    }

    // 등록용 Panel 객체 생성
    private Panel buildPanelForCreate(Long siteId, PanelCreateReq req) {
        Panel panel = new Panel();
        panel.setSiteId(siteId);
        panel.setName(req.getName());
        panel.setDeviceSerial(req.getDeviceSerial());
        panel.setMNo(req.getMNo());
        panel.setInstalledAt(req.getInstalledAt());
        // 등록 직후는 아직 통신 이력이 없어 "정상 확인됨"이 아니라 OFFLINE이 정확한 초기 상태다.
        // 첫 센서 프레임이 들어오면 PanelStatusAggregationService가 그때 실제 상태로 재계산한다.
        panel.setStatus(PanelStatus.OFFLINE);
        panel.setIsOnline(false);
        panel.setCircuitCount(req.getCircuitCount() == null ? 10 : req.getCircuitCount());
        panel.setLeakMaThreshold(defaultIfNull(req.getLeakMaThreshold(), DEFAULT_LEAK_MA_THRESHOLD));
        panel.setTempThreshold(defaultIfNull(req.getTempThreshold(), DEFAULT_TEMP_THRESHOLD));
        panel.setHumidityThreshold(defaultIfNull(req.getHumidityThreshold(), DEFAULT_HUMIDITY_THRESHOLD));
        panel.setOvercurrentThreshold(defaultIfNull(req.getOvercurrentThreshold(), DEFAULT_OVERCURRENT_THRESHOLD));
        panel.setGasThreshold(defaultIfNull(req.getGasThreshold(), DEFAULT_GAS_THRESHOLD));
        panel.setFireThreshold(defaultIfNull(req.getFireThreshold(), DEFAULT_FIRE_THRESHOLD));
        return panel;
    }

    // 수정값을 Panel 객체에 반영
    private void applyUpdate(Panel panel, PanelUpdateReq req) {
        panel.setName(req.getName());
        panel.setDeviceSerial(req.getDeviceSerial());
        panel.setMNo(req.getMNo());
        panel.setInstalledAt(req.getInstalledAt());
        panel.setCircuitCount(req.getCircuitCount() == null ? 10 : req.getCircuitCount());
        panel.setLeakMaThreshold(defaultIfNull(req.getLeakMaThreshold(), DEFAULT_LEAK_MA_THRESHOLD));
        panel.setTempThreshold(defaultIfNull(req.getTempThreshold(), DEFAULT_TEMP_THRESHOLD));
        panel.setHumidityThreshold(defaultIfNull(req.getHumidityThreshold(), DEFAULT_HUMIDITY_THRESHOLD));
        panel.setOvercurrentThreshold(defaultIfNull(req.getOvercurrentThreshold(), DEFAULT_OVERCURRENT_THRESHOLD));
        panel.setGasThreshold(defaultIfNull(req.getGasThreshold(), DEFAULT_GAS_THRESHOLD));
        panel.setFireThreshold(defaultIfNull(req.getFireThreshold(), DEFAULT_FIRE_THRESHOLD));
    }

    // 임계치 미입력 시 기본값 적용
    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    // 임계치 미입력 시 기본값 적용
    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    // 등록 직후 활성 분전반 재조회
    private Panel findActivePanel(Long panelId) {
        Panel panel = panelMapper.findActivePanelById(panelId);
        if (panel == null) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }
        return panel;
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

        // ADMIN은 본인에게 배정된 활성 현장에만 분전반 등록 가능
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
    private void insertFacilityAuditLog(Panel panel,
                                        Long actorUserId,
                                        FacilityAuditAction action,
                                        String beforeData,
                                        String afterData) {
        FacilityAuditLog auditLog = new FacilityAuditLog();
        auditLog.setTargetType(FacilityAuditTargetType.PANEL);
        auditLog.setTargetId(panel.getPanelId());
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        siteMapper.insertFacilityAuditLog(auditLog);
    }

    // 감사 로그 before/after JSON 생성
    private String toAuditJson(Panel panel) {
        try {
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("panelId", panel.getPanelId());
            auditData.put("siteId", panel.getSiteId());
            auditData.put("name", panel.getName());
            auditData.put("deviceSerial", panel.getDeviceSerial());
            auditData.put("mNo", panel.getMNo());
            auditData.put("installedAt", panel.getInstalledAt());
            auditData.put("status", panel.getStatus().name());
            auditData.put("isOnline", panel.getIsOnline());
            auditData.put("lastCommunicatedAt", panel.getLastCommunicatedAt());
            auditData.put("circuitCount", panel.getCircuitCount());
            auditData.put("leakMaThreshold", panel.getLeakMaThreshold());
            auditData.put("tempThreshold", panel.getTempThreshold());
            auditData.put("humidityThreshold", panel.getHumidityThreshold());
            auditData.put("overcurrentThreshold", panel.getOvercurrentThreshold());
            auditData.put("gasThreshold", panel.getGasThreshold());
            auditData.put("fireThreshold", panel.getFireThreshold());
            auditData.put("createdAt", panel.getCreatedAt());
            // updatedAt은 매 UPDATE 쿼리마다 CURRENT_TIMESTAMP로 무조건 갱신되는 컬럼이라
            // 감사 로그 diff 비교에 넣으면 실제 값 변경이 없어도 항상 다르게 나온다 — 비교 대상에서 제외
            auditData.put("deletedAt", panel.getDeletedAt());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("설비 감사 로그 직렬화 실패", e);
        }
    }
}
