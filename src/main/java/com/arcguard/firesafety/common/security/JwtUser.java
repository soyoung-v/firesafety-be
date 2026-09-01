package com.arcguard.firesafety.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// JWT claim에 담는 최소 사용자 컨텍스트
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JwtUser {

    private long userId;

    // 권한 문자열: SUPER_ADMIN, ADMIN, GENERAL
    private String role;
}
