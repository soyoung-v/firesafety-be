package com.arcguard.firesafety.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.dto.req.FcmTokenReq;
import com.arcguard.firesafety.auth.dto.req.UserBulkDeleteReq;
import com.arcguard.firesafety.auth.dto.req.UserCreateReq;
import com.arcguard.firesafety.auth.dto.req.UserUpdateReq;
import com.arcguard.firesafety.auth.dto.res.EmailCheckRes;
import com.arcguard.firesafety.auth.dto.res.UserAuditLogRes;
import com.arcguard.firesafety.auth.dto.res.UserBulkDeleteRes;
import com.arcguard.firesafety.auth.dto.res.UserCreateRes;
import com.arcguard.firesafety.auth.dto.res.UserListRes;
import com.arcguard.firesafety.auth.dto.res.UserUpdateRes;
import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserAuditAction;
import com.arcguard.firesafety.auth.model.UserAuditLog;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.auth.validation.CredentialPolicy;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    // user, refresh_token, user_audit_log 테이블 접근
    private final AuthMapper authMapper;

    // 신규 계정 비밀번호 BCrypt 암호화
    private final PasswordEncoder passwordEncoder;

    // 감사 로그 before/after JSON 변환
    private final ObjectMapper objectMapper;

    // 이메일/비밀번호 형식 정책
    private final CredentialPolicy credentialPolicy;

    // 계정 목록 조회
    // 1. 현재 사용자 확인 → 2. SUPER_ADMIN 권한 확인 → 3. 삭제되지 않은 사용자 조회
    @Transactional(readOnly = true)
    public List<UserListRes> getUsers() {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);

        return authMapper.findActiveUsers().stream()
                .map(UserListRes::from)
                .toList();
    }

    // 사용자 감사 이력 조회
    // 1. 현재 사용자 확인 → 2. SUPER_ADMIN 권한 확인 → 3. 감사 로그 최신순 조회
    @Transactional(readOnly = true)
    public List<UserAuditLogRes> getUserAuditLogs() {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);

        return authMapper.findUserAuditLogs().stream()
                .map(auditLog -> UserAuditLogRes.from(
                        auditLog,
                        toAuditJsonNode(auditLog.getBeforeData()),
                        toAuditJsonNode(auditLog.getAfterData())
                ))
                .toList();
    }

    // FCM 토큰 등록(기기별로 여러 개 보관)
    // 1. 현재 사용자 확인 → 2. 토큰값 확인 → 3. 활성 사용자 확인 → 4. 토큰 등록/소유자 갱신
    @Transactional
    public void updateFcmToken(FcmTokenReq req) {
        UserPrincipal actor = getCurrentUser();
        validateFcmTokenRequest(req);
        findActiveTargetUser(actor.getUserId());

        authMapper.upsertFcmToken(actor.getUserId(), req.getFcmToken());
    }

    // FCM 토큰 해제 — 소유자 확인 없이 토큰값 기준 삭제(unique 토큰이라 다른 사용자 토큰을 잘못 지울 수 없음, ADR-001과 동일 전제)
    @Transactional
    public void deleteFcmToken(FcmTokenReq req) {
        validateFcmTokenRequest(req);
        authMapper.deleteFcmTokens(List.of(req.getFcmToken()));
    }

    // 계정 등록
    // 1. 요청값 확인 → 2. 생성 권한 확인 → 3. 이메일 중복 확인 → 4. 사용자 저장 → 5. 감사 로그 저장
    @Transactional
    public UserCreateRes createUser(UserCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        validateCreateRequest(req);
        String email = credentialPolicy.normalizeEmail(req.getEmail());
        credentialPolicy.validatePassword(req.getPassword());
        validateCreatableRole(actor, req.getRole());

        if (authMapper.existsUserByEmail(email)) {
            throw new BusinessException(AuthErrorCode.DUPLICATED_EMAIL);
        }

        User user = buildUserForCreate(req, email, actor.getUserId());
        authMapper.insertUser(user);

        // 감사 로그에는 비밀번호 원문/해시를 저장하지 않음
        insertUserAuditLog(user, actor.getUserId(), UserAuditAction.CREATE, null, toAuditJson(user));

        return UserCreateRes.from(user);
    }

    // 이메일 중복확인 (계정 등록 폼에서 실시간 확인용). 계정 생성 권한이 있는 ADMIN 이상만 사용
    @Transactional(readOnly = true)
    public EmailCheckRes checkEmailDuplicate(String rawEmail) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        String email = credentialPolicy.normalizeEmail(rawEmail);
        return new EmailCheckRes(authMapper.existsUserByEmail(email));
    }

    // 계정 수정
    // 1. 요청값 확인 → 2. 활성 사용자 조회 → 3. 수정 권한 확인 → 4. 사용자 수정 → 5. 감사 로그 저장
    @Transactional
    public UserUpdateRes updateUser(Long userId, UserUpdateReq req) {
        UserPrincipal actor = getCurrentUser();
        validateUpdateRequest(userId, req);
        String email = credentialPolicy.normalizeEmail(req.getEmail());

        // 삭제된 사용자는 수정하지 않고 복구 API를 먼저 사용
        User targetUser = findActiveTargetUser(userId);
        validateUpdatableRole(actor, targetUser, req.getRole());
        validateEmailChangePermission(actor, targetUser, email);

        if (!targetUser.getEmail().equals(email)
                && authMapper.existsUserByEmail(email)) {
            throw new BusinessException(AuthErrorCode.DUPLICATED_EMAIL);
        }

        String beforeData = toAuditJson(targetUser);
        applyUpdate(targetUser, req, email, actor.getUserId());
        authMapper.updateUser(targetUser);

        // 실제로 값이 바뀐 경우에만 이력을 남긴다 — 수정 버튼만 누르고 아무것도 안 바꾼 경우까지
        // 이력에 남으면 진짜 변경 이력을 찾기 어려워진다.
        String afterData = toAuditJson(targetUser);
        if (!beforeData.equals(afterData)) {
            insertUserAuditLog(targetUser, actor.getUserId(), UserAuditAction.UPDATE, beforeData, afterData);
        }

        return UserUpdateRes.from(targetUser);
    }

    // 계정 단건 삭제
    // 1. 대상 조회 → 2. 삭제 권한 확인 → 3. 소프트 삭제 → 4. RT 폐기 → 5. 감사 로그 저장
    @Transactional
    public void deleteUser(Long userId) {
        UserPrincipal actor = getCurrentUser();
        validateDeleteRequest(userId);

        User targetUser = findDeletableTargetUser(userId);
        validateDeletableRole(actor, targetUser);
        deleteTargetUser(actor, targetUser);
    }

    // 계정 일괄 삭제
    // 1. 요청값 확인 → 2. 대상 전체 조회 → 3. 대상 전체 권한 확인 → 4. 대상별 삭제
    @Transactional
    public UserBulkDeleteRes deleteUsers(UserBulkDeleteReq req) {
        UserPrincipal actor = getCurrentUser();
        List<Long> userIds = validateBulkDeleteRequest(req);

        List<User> targetUsers = userIds.stream()
                .map(this::findDeletableTargetUser)
                .toList();

        // 하나라도 삭제 불가 대상이면 아무도 삭제하지 않음
        targetUsers.forEach(targetUser -> validateBulkDeletableRole(actor, targetUser));

        List<Long> deletedUserIds = new ArrayList<>();
        for (User targetUser : targetUsers) {
            deleteTargetUser(actor, targetUser);
            deletedUserIds.add(targetUser.getUserId());
        }

        return UserBulkDeleteRes.from(deletedUserIds);
    }

    // 계정 복구
    // 1. 삭제 사용자 조회 → 2. 복구 권한 확인 → 3. ACTIVE 전환 → 4. 감사 로그 저장
    @Transactional
    public UserUpdateRes restoreUser(Long userId) {
        UserPrincipal actor = getCurrentUser();
        validateDeleteRequest(userId);

        User targetUser = findRestorableTargetUser(userId);
        validateRestorableRole(actor, targetUser);

        String beforeData = toAuditJson(targetUser);
        int updatedRows = authMapper.restoreUser(targetUser.getUserId(), actor.getUserId());
        if (updatedRows == 0) {
            throw new BusinessException(AuthErrorCode.USER_NOT_DELETED);
        }

        markRestoredForAudit(targetUser, actor.getUserId());
        insertUserAuditLog(targetUser, actor.getUserId(), UserAuditAction.RESTORE, beforeData, toAuditJson(targetUser));

        return UserUpdateRes.from(targetUser);
    }

    // 계정 삭제 공통 처리
    private void deleteTargetUser(UserPrincipal actor, User targetUser) {
        String beforeData = toAuditJson(targetUser);
        int updatedRows = authMapper.softDeleteUser(targetUser.getUserId(), actor.getUserId());
        if (updatedRows == 0) {
            throw new BusinessException(AuthErrorCode.USER_ALREADY_DELETED);
        }

        // 삭제된 계정의 기존 RT는 전부 폐기
        authMapper.revokeAllRefreshTokensByUserId(targetUser.getUserId());

        markDeletedForAudit(targetUser, actor.getUserId());
        insertUserAuditLog(targetUser, actor.getUserId(), UserAuditAction.DELETE, beforeData, toAuditJson(targetUser));
    }

    // 등록용 User 객체 생성
    private User buildUserForCreate(UserCreateReq req, String email, Long actorUserId) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setRole(req.getRole());
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        user.setCreatedBy(actorUserId);
        return user;
    }

    // 계정 생성 권한 확인
    private void validateCreatableRole(UserPrincipal actor, UserRole targetRole) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN && targetRole != UserRole.SUPER_ADMIN) {
            return;
        }

        if (actorRole == UserRole.ADMIN && targetRole != UserRole.SUPER_ADMIN) {
            return;
        }

        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    // 계정 수정 권한 확인
    private void validateUpdatableRole(UserPrincipal actor, User targetUser, UserRole requestedRole) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN
                && targetUser.getRole() != UserRole.SUPER_ADMIN
                && requestedRole != UserRole.SUPER_ADMIN) {
            return;
        }

        if (actorRole == UserRole.ADMIN
                && targetUser.getRole() != UserRole.SUPER_ADMIN
                && requestedRole != UserRole.SUPER_ADMIN
                && hasActiveSharedSite(actor, targetUser)) {
            return;
        }

        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    // 이메일은 로그인 아이디를 겸해 실수로 바꾸면 본인도 모르게 로그인이 막힐 수 있어 SUPER_ADMIN만 변경 가능(프론트 입력칸 비활성화와 동일 기준)
    private void validateEmailChangePermission(UserPrincipal actor, User targetUser, String email) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());
        if (actorRole != UserRole.SUPER_ADMIN && !targetUser.getEmail().equals(email)) {
            throw new BusinessException(AuthErrorCode.EMAIL_UPDATE_FORBIDDEN);
        }
    }

    // 단건 삭제 권한 확인
    private void validateDeletableRole(UserPrincipal actor, User targetUser) {
        if (actor.getUserId() == targetUser.getUserId()) {
            throw new BusinessException(AuthErrorCode.SELF_DELETE_NOT_ALLOWED);
        }

        if (canManageActiveTarget(actor, targetUser)) {
            return;
        }

        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    // 일괄 삭제 권한 확인
    private void validateBulkDeletableRole(UserPrincipal actor, User targetUser) {
        if (actor.getUserId() == targetUser.getUserId()) {
            throw new BusinessException(AuthErrorCode.SELF_DELETE_NOT_ALLOWED);
        }

        if (canManageActiveTarget(actor, targetUser)) {
            return;
        }

        throw new BusinessException(AuthErrorCode.BULK_DELETE_FORBIDDEN_TARGET);
    }

    // 계정 복구 권한 확인
    private void validateRestorableRole(UserPrincipal actor, User targetUser) {
        if (!canRestoreTargetRole(actor, targetUser.getRole())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
        }
    }

    // ADMIN은 같은 활성 현장에 배정된 ADMIN/GENERAL, SUPER_ADMIN은 ADMIN/GENERAL만 관리 가능
    private boolean canManageActiveTarget(UserPrincipal actor, User targetUser) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN) {
            return targetUser.getRole() != UserRole.SUPER_ADMIN;
        }

        return actorRole == UserRole.ADMIN
                && targetUser.getRole() != UserRole.SUPER_ADMIN
                && hasActiveSharedSite(actor, targetUser);
    }

    // 복구는 기존 계약을 유지한다. ADMIN 복구 권한 확대는 별도 정책 확정 후 반영한다.
    private boolean canRestoreTargetRole(UserPrincipal actor, UserRole targetRole) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN) {
            return targetRole != UserRole.SUPER_ADMIN;
        }

        return actorRole == UserRole.ADMIN && targetRole == UserRole.GENERAL;
    }

    private boolean hasActiveSharedSite(UserPrincipal actor, User targetUser) {
        return authMapper.existsActiveSharedSiteAssignment(actor.getUserId(), targetUser.getUserId());
    }

    // 등록 요청값 확인
    private void validateCreateRequest(UserCreateReq req) {
        if (req == null || !StringUtils.hasText(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.EMAIL_REQUIRED);
        }
        if (!StringUtils.hasText(req.getPassword())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_REQUIRED);
        }
        if (!StringUtils.hasText(req.getName())) {
            throw new BusinessException(AuthErrorCode.NAME_REQUIRED);
        }
        if (req.getRole() == null) {
            throw new BusinessException(AuthErrorCode.ROLE_REQUIRED);
        }
    }

    // 수정 요청값 확인
    private void validateUpdateRequest(Long userId, UserUpdateReq req) {
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
        if (req == null || !StringUtils.hasText(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.EMAIL_REQUIRED);
        }
        if (!StringUtils.hasText(req.getName())) {
            throw new BusinessException(AuthErrorCode.NAME_REQUIRED);
        }
        if (req.getRole() == null) {
            throw new BusinessException(AuthErrorCode.ROLE_REQUIRED);
        }
    }

    // 삭제/복구 대상 ID 확인
    private void validateDeleteRequest(Long userId) {
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
    }

    // FCM 토큰 요청값 확인
    private void validateFcmTokenRequest(FcmTokenReq req) {
        if (req == null || !StringUtils.hasText(req.getFcmToken())) {
            throw new BusinessException(AuthErrorCode.FCM_TOKEN_REQUIRED);
        }
    }

    // 일괄 삭제 요청값 확인
    private List<Long> validateBulkDeleteRequest(UserBulkDeleteReq req) {
        if (req == null || req.getUserIds() == null || req.getUserIds().isEmpty()) {
            throw new BusinessException(AuthErrorCode.BULK_DELETE_EMPTY);
        }

        if (req.getUserIds().stream().anyMatch(userId -> userId == null)) {
            throw new BusinessException(AuthErrorCode.BULK_DELETE_INVALID_ID);
        }

        Set<Long> uniqueUserIds = new LinkedHashSet<>(req.getUserIds());
        if (uniqueUserIds.size() != req.getUserIds().size()) {
            throw new BusinessException(AuthErrorCode.BULK_DELETE_DUPLICATED);
        }

        return req.getUserIds();
    }

    // 수정 가능한 활성 사용자 조회
    private User findActiveTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null || targetUser.getDeletedAt() != null || targetUser.getAccountStatus() != UserAccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        return targetUser;
    }

    // 삭제 가능한 활성 사용자 조회
    private User findDeletableTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        if (targetUser.getDeletedAt() != null || targetUser.getAccountStatus() == UserAccountStatus.DELETED) {
            throw new BusinessException(AuthErrorCode.USER_ALREADY_DELETED);
        }
        return targetUser;
    }

    // 복구 가능한 삭제 사용자 조회
    private User findRestorableTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        if (targetUser.getDeletedAt() == null || targetUser.getAccountStatus() != UserAccountStatus.DELETED) {
            throw new BusinessException(AuthErrorCode.USER_NOT_DELETED);
        }
        return targetUser;
    }

    // 수정값을 User 객체에 반영
    private void applyUpdate(User targetUser, UserUpdateReq req, String email, Long actorUserId) {
        targetUser.setEmail(email);
        targetUser.setName(req.getName());
        targetUser.setPhone(req.getPhone());
        targetUser.setRole(req.getRole());
        targetUser.setUpdatedBy(actorUserId);
    }

    // 감사 로그 afterData용 삭제 상태 반영
    private void markDeletedForAudit(User targetUser, Long actorUserId) {
        targetUser.setAccountStatus(UserAccountStatus.DELETED);
        targetUser.setDeletedBy(actorUserId);
        targetUser.setDeletedAt(LocalDateTime.now());
    }

    // 감사 로그 afterData용 복구 상태 반영
    private void markRestoredForAudit(User targetUser, Long actorUserId) {
        targetUser.setAccountStatus(UserAccountStatus.ACTIVE);
        targetUser.setDeletedAt(null);
        targetUser.setRestoredBy(actorUserId);
        targetUser.setRestoredAt(LocalDateTime.now());
    }

    // SUPER_ADMIN 권한 확인
    private void requireSuperAdmin(UserPrincipal actor) {
        if (!UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
        }
    }

    // ADMIN 이상 권한 확인
    private void requireAdminOrSuperAdmin(UserPrincipal actor) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());
        if (actorRole != UserRole.ADMIN && actorRole != UserRole.SUPER_ADMIN) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
        }
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        //SecurityContextHolder = Spring Security가 쓰는 현재 로그인 사용자 보관함
        //Authentication = 그 보관함 안에 들어있는 인증 정보
        //UserPrincipal = 우리가 쓰기 좋게 만든 로그인 사용자 정보 객체
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(AuthErrorCode.EXPIRED_AUTH);
        }
        return userPrincipal;
    }

    // 사용자 감사 로그 저장
    private void insertUserAuditLog(User targetUser,
                                    Long actorUserId,
                                    UserAuditAction action,
                                    String beforeData,
                                    String afterData) {
        UserAuditLog auditLog = new UserAuditLog();
        auditLog.setTargetUserId(targetUser.getUserId());
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        authMapper.insertUserAuditLog(auditLog);
    }

    // 감사 로그 before/after JSON 생성
    private String toAuditJson(User user) {
        try {
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("userId", user.getUserId());
            auditData.put("email", user.getEmail());
            auditData.put("name", user.getName());
            auditData.put("phone", user.getPhone());
            auditData.put("role", user.getRole().name());
            auditData.put("accountStatus", user.getAccountStatus().name());
            auditData.put("createdBy", user.getCreatedBy());
            auditData.put("updatedBy", user.getUpdatedBy());
            auditData.put("deletedBy", user.getDeletedBy());
            auditData.put("deletedAt", user.getDeletedAt());
            auditData.put("restoredAt", user.getRestoredAt());
            auditData.put("restoredBy", user.getRestoredBy());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("사용자 감사 로그 직렬화 실패", e);
        }
    }

    // DB에 저장된 감사 로그 JSON 문자열을 응답용 JSON 객체로 변환.
    // JsonNode로 반환하면 Spring Boot 4 기본 Jackson(3.x)이 Jackson 2 타입인 이 값을 트리로 인식 못 하고
    // is*() 게터를 그대로 직렬화해버려서(예: {"array":false,...}) 일반 Map/List/원시값 구조로 반환한다.
    private Object toAuditJsonNode(String auditData) {
        if (!StringUtils.hasText(auditData)) {
            return null;
        }

        try {
            return objectMapper.readValue(auditData, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("사용자 감사 로그 역직렬화 실패", e);
        }
    }
}
