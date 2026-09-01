package com.arcguard.firesafety.auth.mapper;

import com.arcguard.firesafety.auth.model.PasswordResetToken;
import com.arcguard.firesafety.auth.model.RefreshToken;
import com.arcguard.firesafety.auth.model.User;
import com.arcguard.firesafety.auth.model.UserAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 로그인, 토큰, 계정관리에서 사용하는 auth 도메인 DB 접근 지점
@Mapper
public interface AuthMapper {

    // 로그인 시 이메일로 사용자 조회
    User findUserByEmail(@Param("email") String email);

    // 토큰 재발급과 보호 흐름에서 사용자 존재 여부 확인
    User findUserById(@Param("userId") Long userId);

    // 로그인 시 Refresh Token 원문 대신 SHA-256 해시 저장
    void insertRefreshToken(RefreshToken refreshToken);

    // 재발급/로그아웃 시 rt 쿠키 해시값으로 Refresh Token 조회
    RefreshToken findRefreshTokenByTokenHash(@Param("tokenHash") String tokenHash);

    // 로그아웃 시 현재 Refresh Token 폐기 처리
    void revokeRefreshToken(@Param("tokenHash") String tokenHash);

    // 계정 삭제 시 대상 사용자의 모든 Refresh Token 폐기 처리
    void revokeAllRefreshTokensByUserId(@Param("userId") Long userId);

    // 계정 등록 시 이메일 중복 여부 확인
    boolean existsUserByEmail(@Param("email") String email);

    // SUPER_ADMIN 계정관리 화면에서 삭제되지 않은 사용자 목록 조회
    List<User> findActiveUsers();

    // 현장 담당 직원 목록/연락망 조회(필터/페이징 없음). 담당 현장 범위 판단은 호출한 facility 서비스가 먼저 처리
    List<User> findActiveSiteUsersByRoles(@Param("siteId") Long siteId, @Param("roles") List<String> roles);

    // 현장 담당 직원 목록 화면(SCR-404)용 — keyword(이름/이메일/전화번호 부분일치)/페이징 지원
    List<User> findActiveSiteUsersByRolesPaged(@Param("siteId") Long siteId,
                                                @Param("roles") List<String> roles,
                                                @Param("keyword") String keyword,
                                                @Param("size") int size,
                                                @Param("offset") int offset);

    // 현장 담당 직원 목록 전체 개수
    long countActiveSiteUsersByRoles(@Param("siteId") Long siteId,
                                      @Param("roles") List<String> roles,
                                      @Param("keyword") String keyword);

    // ADMIN이 대상 사용자와 같은 활성 현장에 배정되어 있는지 확인
    boolean existsActiveSharedSiteAssignment(@Param("actorUserId") Long actorUserId,
                                             @Param("targetUserId") Long targetUserId);

    // SUPER_ADMIN 계정관리 이력 화면에서 사용자 변경 감사 로그 조회
    List<UserAuditLog> findUserAuditLogs();

    // 관리자 계정관리 화면에서 신규 사용자 등록
    void insertUser(User user);

    // 관리자 계정관리 화면에서 사용자 기본 정보 수정
    void updateUser(User user);

    // 비밀번호 재설정 확정 시 새 비밀번호 해시 저장
    int updatePassword(@Param("userId") Long userId, @Param("password") String password);

    // 현재 로그인 사용자의 FCM 토큰 등록(기기별로 여러 개 보관, 같은 토큰이면 소유자만 갱신)
    void upsertFcmToken(@Param("userId") Long userId, @Param("fcmToken") String fcmToken);

    // 경보 발생 현장의 담당자(ADMIN/GENERAL) FCM 토큰 조회(기기별 다건)
    List<String> findFcmTokensForAlertSite(@Param("siteId") Long siteId);

    // FCM 발송 결과 UNREGISTERED 등으로 무효 판정된 토큰 정리
    void deleteFcmTokens(@Param("fcmTokens") List<String> fcmTokens);

    // 비밀번호 재설정 요청 전 기존 미사용 토큰 만료 처리
    void expireUnusedPasswordResetTokensByUserId(@Param("userId") Long userId);

    // 비밀번호 재설정 메일 발송 전 토큰 해시 저장
    void insertPasswordResetToken(PasswordResetToken passwordResetToken);

    // 비밀번호 재설정 확정 시 토큰 해시로 토큰 조회
    PasswordResetToken findPasswordResetTokenByTokenHash(@Param("tokenHash") String tokenHash);

    // 비밀번호 재설정 완료 후 토큰 재사용 방지
    int markPasswordResetTokenUsed(@Param("tokenId") Long tokenId);

    // 계정 삭제 시 상태값과 삭제 감사 필드만 변경
    int softDeleteUser(@Param("userId") Long userId, @Param("deletedBy") Long deletedBy);

    // 계정 복구 시 삭제 기준값을 해제하고 복구 감사 필드 기록
    int restoreUser(@Param("userId") Long userId, @Param("restoredBy") Long restoredBy);

    // 사용자 생성/수정/삭제/복구/비밀번호 변경 이력 기록
    void insertUserAuditLog(UserAuditLog auditLog);

    // 서버 최초 기동 시 플랫폼관리자 계정 생성
    void insertBootstrapSuperAdmin(User user);
}
