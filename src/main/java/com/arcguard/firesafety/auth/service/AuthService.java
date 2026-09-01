package com.arcguard.firesafety.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.config.PasswordResetProperties;
import com.arcguard.firesafety.auth.dto.req.LoginReq;
import com.arcguard.firesafety.auth.dto.req.PasswordResetConfirmReq;
import com.arcguard.firesafety.auth.dto.req.PasswordResetRequestReq;
import com.arcguard.firesafety.auth.dto.res.LoginRes;
import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.auth.model.PasswordResetToken;
import com.arcguard.firesafety.auth.model.RefreshToken;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserAuditAction;
import com.arcguard.firesafety.auth.model.UserAuditLog;
import com.arcguard.firesafety.auth.util.TokenHashUtil;
import com.arcguard.firesafety.auth.validation.CredentialPolicy;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.config.jwt.ConstJwt;
import com.arcguard.firesafety.config.security.JwtTokenManager;
import com.arcguard.firesafety.config.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // user, refresh_token 테이블 접근
    private final AuthMapper authMapper;

    // BCrypt 비밀번호 비교
    private final PasswordEncoder passwordEncoder;

    // JWT 생성/파싱 담당
    private final JwtTokenProvider jwtTokenProvider;

    // JWT 쿠키 저장/조회 담당
    private final JwtTokenManager jwtTokenManager;

    // JWT 만료시간 설정값
    private final ConstJwt constJwt;

    // 비밀번호 재설정 메일 발송
    private final PasswordResetMailService passwordResetMailService;

    // 비밀번호 재설정 링크/만료/발신자 설정
    private final PasswordResetProperties passwordResetProperties;

    // 비밀번호 재설정 반복 요청 제한
    private final PasswordResetRateLimiter passwordResetRateLimiter;

    // 이메일/비밀번호 형식 정책
    private final CredentialPolicy credentialPolicy;

    // 감사 로그 JSON 변환
    private final ObjectMapper objectMapper;

    // 로그인 처리
    // 1. 요청값 확인 → 2. 이메일 조회/비밀번호 검증 → 3. AT/RT 발급 → 4. RT 해시 저장
    @Transactional
    public LoginRes login(LoginReq req, HttpServletResponse response) {
        validateLoginRequest(req);
        String email = credentialPolicy.normalizeEmail(req.getEmail());

        // 계정 존재/삭제/비밀번호 오류는 모두 같은 로그인 실패로 처리
        User user = authMapper.findUserByEmail(email);
        if (user == null || isDeleted(user) || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }

        // JWT에는 인증에 필요한 userId, role만 저장
        JwtUser jwtUser = new JwtUser(user.getUserId(), user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);

        // RT 원문은 쿠키로만 전달하고 DB에는 해시만 저장
        saveRefreshToken(user.getUserId(), refreshToken);
        jwtTokenManager.setAccessTokenInCookie(response, accessToken);
        jwtTokenManager.setRefreshTokenInCookie(response, refreshToken);

        return LoginRes.from(user);
    }

    // 로그아웃 처리
    // 1. 쿠키에서 RT 추출 → 2. DB RT 폐기 → 3. AT/RT 쿠키 만료
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtTokenManager.getRefreshTokenFromCookie(request);

        // rt 쿠키가 있을 때만 DB 토큰 폐기
        if (StringUtils.hasText(refreshToken)) {
            authMapper.revokeRefreshToken(TokenHashUtil.sha256Hex(refreshToken));
        }

        // HttpOnly 쿠키는 서버가 만료 쿠키로 삭제
        jwtTokenManager.expireCookies(response);
    }

    // 토큰 재발급 처리
    // 1. 쿠키 RT 추출 → 2. JWT 검증 → 3. DB 해시 검증 → 4. 새 AT 쿠키 발급
    @Transactional(readOnly = true)
    public void reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtTokenManager.getRefreshTokenFromCookie(request);
        if (!StringUtils.hasText(refreshToken)) {
            failReissue(response);
        }

        // RT 서명/만료 검증
        JwtUser jwtUser = jwtTokenProvider.getJwtUserFromToken(refreshToken);
        if (jwtUser == null) {
            failReissue(response);
        }

        // DB에는 RT 원문이 아니라 SHA-256 해시로 저장되어 있음
        RefreshToken savedToken = authMapper.findRefreshTokenByTokenHash(TokenHashUtil.sha256Hex(refreshToken));
        if (!isUsableRefreshToken(savedToken, jwtUser)) {
            failReissue(response);
        }

        // 삭제된 사용자는 RT가 남아 있어도 재발급 불가
        User user = authMapper.findUserById(savedToken.getUserId());
        if (user == null || isDeleted(user)) {
            failReissue(response);
        }

        // 최신 role 기준으로 새 AT 생성
        JwtUser refreshedJwtUser = new JwtUser(user.getUserId(), user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(refreshedJwtUser);
        jwtTokenManager.setAccessTokenInCookie(response, accessToken);
    }

    // 비밀번호 재설정 요청
    // 1. 요청값 확인 → 2. 요청 횟수 제한 → 3. ACTIVE 계정이면 토큰 저장 → 4. 메일 발송
    public void requestPasswordReset(PasswordResetRequestReq req, HttpServletRequest request) {
        validatePasswordResetRequest(req);
        String email = credentialPolicy.normalizeEmail(req.getEmail());
        passwordResetRateLimiter.check(email, getClientIp(request));

        // 계정 존재 여부는 응답에 노출하지 않음
        User user = authMapper.findUserByEmail(email);
        if (user == null || isDeleted(user)) {
            return;
        }

        // 새 토큰을 만들기 전에 이전 미사용 토큰은 만료 처리
        authMapper.expireUnusedPasswordResetTokensByUserId(user.getUserId());

        String originalToken = generatePasswordResetToken();
        PasswordResetToken passwordResetToken = buildPasswordResetToken(user, originalToken, request);
        authMapper.insertPasswordResetToken(passwordResetToken);

        passwordResetMailService.sendPasswordResetMail(user, originalToken);
    }

    // 비밀번호 재설정 확정
    // 1. 토큰 검증 → 2. 새 비밀번호 저장 → 3. 토큰 사용 처리 → 4. RT 폐기 → 5. 감사 로그 저장
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmReq req) {
        validatePasswordResetConfirmRequest(req);
        credentialPolicy.validatePassword(req.getNewPassword());

        PasswordResetToken token = authMapper.findPasswordResetTokenByTokenHash(TokenHashUtil.sha256Hex(req.getToken()));
        validatePasswordResetToken(token);

        User user = authMapper.findUserById(token.getUserId());
        if (user == null || isDeleted(user)) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        String beforeData = toPasswordResetAuditJson(user);
        String encodedPassword = passwordEncoder.encode(req.getNewPassword());
        int updatedRows = authMapper.updatePassword(user.getUserId(), encodedPassword);
        if (updatedRows == 0) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        int usedRows = authMapper.markPasswordResetTokenUsed(token.getTokenId());
        if (usedRows == 0) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        // 비밀번호 변경 후 기존 로그인 세션은 모두 무효화
        authMapper.revokeAllRefreshTokensByUserId(user.getUserId());
        insertPasswordResetAuditLog(user, beforeData);
    }

    // 로그인 요청값 확인
    private void validateLoginRequest(LoginReq req) {
        if (req == null || !StringUtils.hasText(req.getEmail()) || !StringUtils.hasText(req.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }
    }

    // 비밀번호 재설정 요청값 확인
    private void validatePasswordResetRequest(PasswordResetRequestReq req) {
        if (req == null || !StringUtils.hasText(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    // 비밀번호 재설정 확정 요청값 확인
    private void validatePasswordResetConfirmRequest(PasswordResetConfirmReq req) {
        if (req == null || !StringUtils.hasText(req.getToken()) || !StringUtils.hasText(req.getNewPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    // 비밀번호 재설정 토큰 상태 확인
    private void validatePasswordResetToken(PasswordResetToken token) {
        if (token == null) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }
    }

    // 소프트 삭제된 계정인지 확인
    private boolean isDeleted(User user) {
        return user.getAccountStatus() == UserAccountStatus.DELETED || user.getDeletedAt() != null;
    }

    // DB에 저장된 RT 해시가 재발급 가능한 상태인지 확인
    private boolean isUsableRefreshToken(RefreshToken savedToken, JwtUser jwtUser) {
        return savedToken != null
                && savedToken.getRevokedAt() == null
                && savedToken.getExpiresAt().isAfter(LocalDateTime.now())
                && savedToken.getUserId().equals(jwtUser.getUserId());
    }

    // 재발급 실패 처리
    private void failReissue(HttpServletResponse response) {
        jwtTokenManager.expireCookies(response);
        throw new BusinessException(AuthErrorCode.EXPIRED_AUTH);
    }

    // RT 저장
    // 원문 저장 금지: SHA-256 해시와 만료시간만 저장
    private void saveRefreshToken(Long userId, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHashUtil.sha256Hex(refreshToken));
        token.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(constJwt.getRefreshTokenValidityMilliseconds())));

        authMapper.insertRefreshToken(token);
    }

    // 비밀번호 재설정 원본 토큰 생성
    private String generatePasswordResetToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // DB에는 원본 토큰 대신 SHA-256 해시만 저장
    private PasswordResetToken buildPasswordResetToken(User user, String originalToken, HttpServletRequest request) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getUserId());
        token.setTokenHash(TokenHashUtil.sha256Hex(originalToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetProperties.getTokenExpirationMinutes()));
        token.setRequestIp(getClientIp(request));
        token.setUserAgent(getUserAgent(request));
        return token;
    }

    // 프록시 환경이면 X-Forwarded-For 첫 번째 IP를 우선 사용
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // DB 컬럼 길이에 맞춰 User-Agent는 최대 255자로 저장
    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }

    // 비밀번호 변경 감사 로그 저장
    private void insertPasswordResetAuditLog(User user, String beforeData) {
        UserAuditLog auditLog = new UserAuditLog();
        auditLog.setTargetUserId(user.getUserId());
        auditLog.setActorUserId(null);
        auditLog.setAction(UserAuditAction.PASSWORD_RESET);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(toPasswordResetAuditJson(user));
        authMapper.insertUserAuditLog(auditLog);
    }

    // 감사 로그에는 비밀번호 해시와 토큰을 저장하지 않음
    private String toPasswordResetAuditJson(User user) {
        try {
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("userId", user.getUserId());
            auditData.put("email", user.getEmail());
            auditData.put("action", UserAuditAction.PASSWORD_RESET.name());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("비밀번호 재설정 감사 로그 직렬화 실패", e);
        }
    }
}
