package com.arcguard.firesafety.auth.service;

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
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.auth.validation.CredentialPolicy;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.config.jwt.ConstJwt;
import com.arcguard.firesafety.config.security.JwtTokenManager;
import com.arcguard.firesafety.config.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtTokenManager jwtTokenManager;

    @Mock
    private PasswordResetMailService passwordResetMailService;

    @Mock
    private PasswordResetRateLimiter passwordResetRateLimiter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AuthService authService;
    private PasswordResetProperties passwordResetProperties;

    @BeforeEach
    void setUp() {
        ConstJwt constJwt = new ConstJwt(
                "firesafety",
                "JWT",
                "user",
                "test-secret",
                "at",
                "/",
                3600,
                3600000,
                "rt",
                "/api/auth",
                604800,
                604800000,
                false,
                "Lax"
        );

        passwordResetProperties = new PasswordResetProperties();
        passwordResetProperties.setBaseUrl("http://localhost:5173/reset-password");
        passwordResetProperties.setTokenExpirationMinutes(30);
        passwordResetProperties.setMailFromAddress("noreply@example.com");
        passwordResetProperties.setMailFromName("아크가드 ArcGuard");
        passwordResetProperties.setRateLimitWindowMinutes(10);
        passwordResetProperties.setRateLimitMaxCount(5);

        authService = new AuthService(
                authMapper,
                passwordEncoder,
                jwtTokenProvider,
                jwtTokenManager,
                constJwt,
                passwordResetMailService,
                passwordResetProperties,
                passwordResetRateLimiter,
                new CredentialPolicy(),
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("AUTH-001: 정상 로그인 성공 시 사용자 정보만 반환하고 RT는 해시로 저장한다")
    void loginSuccess() {
        // given
        User user = activeUser(3L, "admin@example.com", UserRole.ADMIN);
        when(authMapper.findUserByEmail("admin@example.com")).thenReturn(user);
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(JwtUser.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(JwtUser.class))).thenReturn("refresh-token");

        // when
        LoginRes result = authService.login(new LoginReq("admin@example.com", "password"), response);

        // then
        assertThat(result.getUserId()).isEqualTo(3L);
        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(authMapper).insertRefreshToken(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);
        assertThat(tokenCaptor.getValue().getTokenHash()).isNotEqualTo("refresh-token");

        verify(jwtTokenManager).setAccessTokenInCookie(response, "access-token");
        verify(jwtTokenManager).setRefreshTokenInCookie(response, "refresh-token");
    }

    @Test
    @DisplayName("AUTH-002: 잘못된 비밀번호 로그인 시 401 통일 에러를 반환한다")
    void loginFailWithWrongPassword() {
        // given
        User user = activeUser(3L, "admin@example.com", UserRole.ADMIN);
        when(authMapper.findUserByEmail("admin@example.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginReq("admin@example.com", "wrong-password"), response))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_LOGIN));

        verify(authMapper, never()).insertRefreshToken(any());
    }

    @Test
    @DisplayName("AUTH-003: RT 쿠키가 없으면 재발급 실패 처리하고 쿠키를 만료한다")
    void reissueFailWithoutRefreshToken() {
        // given
        when(jwtTokenManager.getRefreshTokenFromCookie(request)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.EXPIRED_AUTH));

        verify(jwtTokenManager).expireCookies(response);
    }

    @Test
    @DisplayName("ACC-008: 존재하지 않는 이메일의 비밀번호 재설정 요청은 메일을 보내지 않고 조용히 종료한다")
    void passwordResetRequestWithUnknownEmailReturnsSameFlow() {
        // given
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authMapper.findUserByEmail("unknown@example.com")).thenReturn(null);

        // when
        authService.requestPasswordReset(new PasswordResetRequestReq("unknown@example.com"), request);

        // then
        verify(authMapper, never()).insertPasswordResetToken(any());
        verify(passwordResetMailService, never()).sendPasswordResetMail(any(), anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 요청: ACTIVE 계정이면 기존 토큰 만료 후 새 토큰 해시를 저장하고 메일을 발송한다")
    void passwordResetRequestWithActiveUserSavesTokenHashAndSendsMail() {
        // given
        User user = activeUser(7L, "user@example.com", UserRole.GENERAL);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader(anyString())).thenAnswer(invocation ->
                "User-Agent".equals(invocation.getArgument(0)) ? "JUnit" : null);
        when(authMapper.findUserByEmail("user@example.com")).thenReturn(user);

        // when
        authService.requestPasswordReset(new PasswordResetRequestReq("user@example.com"), request);

        // then
        verify(authMapper).expireUnusedPasswordResetTokensByUserId(7L);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(authMapper).insertPasswordResetToken(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);
        assertThat(tokenCaptor.getValue().getRequestIp()).isEqualTo("127.0.0.1");
        assertThat(tokenCaptor.getValue().getUserAgent()).isEqualTo("JUnit");

        verify(passwordResetMailService).sendPasswordResetMail(any(User.class), anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 확정: 정상 토큰이면 비밀번호 변경, 토큰 사용 처리, RT 폐기, 감사 로그를 수행한다")
    void passwordResetConfirmWithValidTokenChangesPassword() {
        // given
        PasswordResetToken token = passwordResetToken(10L, 7L, LocalDateTime.now().plusMinutes(10), null);
        User user = activeUser(7L, "user@example.com", UserRole.GENERAL);

        when(authMapper.findPasswordResetTokenByTokenHash(anyString())).thenReturn(token);
        when(authMapper.findUserById(7L)).thenReturn(user);
        when(passwordEncoder.encode("new-password1")).thenReturn("encoded-new-password");
        when(authMapper.updatePassword(7L, "encoded-new-password")).thenReturn(1);
        when(authMapper.markPasswordResetTokenUsed(10L)).thenReturn(1);

        // when
        authService.confirmPasswordReset(new PasswordResetConfirmReq("original-token", "new-password1"));

        // then
        verify(authMapper).updatePassword(7L, "encoded-new-password");
        verify(authMapper).markPasswordResetTokenUsed(10L);
        verify(authMapper).revokeAllRefreshTokensByUserId(7L);
        verify(authMapper).insertUserAuditLog(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 확정: 만료 토큰이면 401 에러를 반환한다")
    void passwordResetConfirmWithExpiredTokenFails() {
        // given
        PasswordResetToken token = passwordResetToken(10L, 7L, LocalDateTime.now().minusMinutes(1), null);
        when(authMapper.findPasswordResetTokenByTokenHash(anyString())).thenReturn(token);

        // when & then
        assertThatThrownBy(() -> authService.confirmPasswordReset(new PasswordResetConfirmReq("original-token", "new-password1")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED));

        verify(authMapper, never()).updatePassword(any(), any());
    }

    private User activeUser(Long userId, String email, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setName("테스트사용자");
        user.setRole(role);
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        return user;
    }

    private PasswordResetToken passwordResetToken(Long tokenId,
                                                  Long userId,
                                                  LocalDateTime expiresAt,
                                                  LocalDateTime usedAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setTokenId(tokenId);
        token.setUserId(userId);
        token.setTokenHash("token-hash");
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        return token;
    }
}
