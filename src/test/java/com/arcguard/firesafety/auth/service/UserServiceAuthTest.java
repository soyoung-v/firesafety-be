package com.arcguard.firesafety.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.auth.dto.req.UserBulkDeleteReq;
import com.arcguard.firesafety.auth.dto.req.UserCreateReq;
import com.arcguard.firesafety.auth.dto.req.UserUpdateReq;
import com.arcguard.firesafety.auth.exception.AuthErrorCode;
import com.arcguard.firesafety.auth.mapper.AuthMapper;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAccountStatus;
import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.auth.validation.CredentialPolicy;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAuthTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(authMapper, passwordEncoder, new ObjectMapper().findAndRegisterModules(), new CredentialPolicy());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AUTH-004: SUPER_ADMIN은 ADMIN 계정을 등록할 수 있다")
    void superAdminCanCreateAdmin() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded-password");

        // when
        userService.createUser(new UserCreateReq(
                "admin@example.com",
                "password1",
                "관리자",
                "010-0000-0000",
                UserRole.ADMIN
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(userCaptor.getValue().getCreatedBy()).isEqualTo(1L);
        verify(authMapper).insertUserAuditLog(any());
    }

    @Test
    @DisplayName("AUTH-005: ADMIN은 ADMIN 계정을 등록할 수 있다")
    void adminCanCreateAdmin() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.existsUserByEmail("admin2@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded-password");

        // when
        userService.createUser(new UserCreateReq(
                "admin2@example.com",
                "password1",
                "관리자2",
                "010-0000-0001",
                UserRole.ADMIN
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(userCaptor.getValue().getCreatedBy()).isEqualTo(2L);
    }

    @Test
    @DisplayName("AUTH-006: ADMIN은 GENERAL 계정을 등록할 수 있다")
    void adminCanCreateGeneral() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.existsUserByEmail("general@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded-password");

        // when
        userService.createUser(new UserCreateReq(
                "general@example.com",
                "password1",
                "일반직원",
                "010-0000-0002",
                UserRole.GENERAL
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.GENERAL);
        assertThat(userCaptor.getValue().getCreatedBy()).isEqualTo(2L);
        verify(authMapper).insertUserAuditLog(any());
    }

    @Test
    @DisplayName("AUTH-007: ADMIN은 같은 활성 현장에 배정된 ADMIN 계정을 삭제할 수 있다")
    void adminCanDeleteSharedAdmin() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.ADMIN));
        when(authMapper.existsActiveSharedSiteAssignment(2L, 3L)).thenReturn(true);
        when(authMapper.softDeleteUser(3L, 2L)).thenReturn(1);

        // when
        userService.deleteUser(3L);

        // then
        verify(authMapper).softDeleteUser(3L, 2L);
        verify(authMapper).revokeAllRefreshTokensByUserId(3L);
        verify(authMapper).insertUserAuditLog(any());
    }

    @Test
    @DisplayName("ADMIN은 같은 활성 현장 배정이 없는 ADMIN 계정을 삭제할 수 없다")
    void adminCannotDeleteAdminWithoutSharedSite() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.ADMIN));
        when(authMapper.existsActiveSharedSiteAssignment(2L, 3L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(3L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.FORBIDDEN_ROLE));

        verify(authMapper, never()).softDeleteUser(any(), any());
        verify(authMapper, never()).revokeAllRefreshTokensByUserId(any());
    }

    @Test
    @DisplayName("ADMIN은 같은 활성 현장에 배정된 ADMIN 계정을 수정할 수 있다")
    void adminCanUpdateSharedAdmin() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.ADMIN));
        when(authMapper.existsActiveSharedSiteAssignment(2L, 3L)).thenReturn(true);

        // when
        userService.updateUser(3L, new UserUpdateReq(
                "target@example.com",
                "수정관리자",
                "010-1111-2222",
                UserRole.ADMIN
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).updateUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(userCaptor.getValue().getUpdatedBy()).isEqualTo(2L);
        verify(authMapper).insertUserAuditLog(any());
    }

    @Test
    @DisplayName("AUTH-024: SUPER_ADMIN은 계정 이메일을 변경할 수 있다")
    void superAdminCanUpdateEmail() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.ADMIN));
        when(authMapper.existsUserByEmail("changed@example.com")).thenReturn(false);

        // when
        userService.updateUser(3L, new UserUpdateReq(
                "changed@example.com",
                "수정관리자",
                "010-1111-2222",
                UserRole.ADMIN
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).updateUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("changed@example.com");
    }

    @Test
    @DisplayName("AUTH-024: ADMIN은 같은 활성 현장에 배정된 계정이라도 이메일을 변경할 수 없다")
    void adminCannotUpdateEmail() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.ADMIN));
        when(authMapper.existsActiveSharedSiteAssignment(2L, 3L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateUser(3L, new UserUpdateReq(
                "changed@example.com",
                "수정관리자",
                "010-1111-2222",
                UserRole.ADMIN
        )))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_UPDATE_FORBIDDEN));

        verify(authMapper, never()).updateUser(any());
    }

    @Test
    @DisplayName("ADMIN은 같은 활성 현장 배정이 없는 사용자를 수정할 수 없다")
    void adminCannotUpdateUserWithoutSharedSite() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.GENERAL));
        when(authMapper.existsActiveSharedSiteAssignment(2L, 3L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.updateUser(3L, new UserUpdateReq(
                "target@example.com",
                "수정직원",
                "010-1111-2222",
                UserRole.GENERAL
        )))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.FORBIDDEN_ROLE));

        verify(authMapper, never()).updateUser(any());
    }

    @Test
    @DisplayName("ADMIN은 사용자를 SUPER_ADMIN으로 수정할 수 없다")
    void adminCannotUpdateUserToSuperAdmin() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.GENERAL));

        // when & then
        assertThatThrownBy(() -> userService.updateUser(3L, new UserUpdateReq(
                "target@example.com",
                "수정직원",
                "010-1111-2222",
                UserRole.SUPER_ADMIN
        )))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.FORBIDDEN_ROLE));

        verify(authMapper, never()).updateUser(any());
    }

    @Test
    @DisplayName("ADMIN 일괄 삭제에 담당 범위 밖 대상이 포함되면 전체 삭제를 수행하지 않는다")
    void adminBulkDeleteRollsBackWhenOutOfScopeTargetIncluded() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.ADMIN));
        when(authMapper.findUserById(4L)).thenReturn(activeUser(4L, UserRole.GENERAL));
        when(authMapper.existsActiveSharedSiteAssignment(2L, 3L)).thenReturn(true);
        when(authMapper.existsActiveSharedSiteAssignment(2L, 4L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.deleteUsers(new UserBulkDeleteReq(List.of(3L, 4L))))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.BULK_DELETE_FORBIDDEN_TARGET));

        verify(authMapper, never()).softDeleteUser(any(), any());
    }

    @Test
    @DisplayName("관리자 계정 생성: 이메일 앞뒤 공백은 제거하고 소문자로 저장한다")
    void createUserTrimsAndLowercasesEmail() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded-password");

        // when
        userService.createUser(new UserCreateReq(
                "  ADMIN@Example.COM  ",
                "password1",
                "관리자",
                "010-0000-0000",
                UserRole.ADMIN
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("관리자 계정 생성: 중복 이메일이면 409를 반환한다")
    void createUserWithDuplicatedEmailFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("admin@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(new UserCreateReq(
                "admin@example.com",
                "password1",
                "관리자",
                "010-0000-0000",
                UserRole.ADMIN
        )))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.DUPLICATED_EMAIL));

        verify(authMapper, never()).insertUser(any());
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User activeUser(Long userId, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("target@example.com");
        user.setName("대상사용자");
        user.setRole(role);
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        return user;
    }
}
