package com.arcguard.firesafety.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// SecurityContext 인증 사용자 정보. 권한 판단의 기준 컨텍스트
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final JwtUser jwtUser;

    // 인증 사용자 ID 조회
    public long getUserId() {
        return jwtUser.getUserId();
    }

    // 인증 사용자 권한 조회
    public String getRole() {
        return jwtUser.getRole();
    }

    // Spring Security 권한 목록 변환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + jwtUser.getRole()));
    }

    // JWT 인증에서는 비밀번호 미사용
    @Override
    public String getPassword() {
        return "";
    }

    // SecurityContext 사용자 식별값 변환
    @Override
    public String getUsername() {
        return String.valueOf(jwtUser.getUserId());
    }
}
