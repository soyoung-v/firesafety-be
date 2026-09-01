package com.arcguard.firesafety.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcguard.firesafety.common.security.JwtUser;
import com.arcguard.firesafety.config.jwt.ConstJwt;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

// JWT 생성/검증
@Slf4j
@Component
public class JwtTokenProvider {

    private final ObjectMapper objectMapper;
    private final ConstJwt constJwt;
    private final SecretKey secretKey;

    // JWT 서명 키 초기화
    public JwtTokenProvider(ObjectMapper objectMapper, ConstJwt constJwt) {
        this.objectMapper = objectMapper;
        this.constJwt = constJwt;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(constJwt.getSecretKey()));
        log.info("JwtTokenProvider 초기화 완료");
    }

    // AT 생성
    public String generateAccessToken(JwtUser jwtUser) {
        return generateToken(jwtUser, constJwt.getAccessTokenValidityMilliseconds());
    }

    // RT 생성
    public String generateRefreshToken(JwtUser jwtUser) {
        return generateToken(jwtUser, constJwt.getRefreshTokenValidityMilliseconds());
    }

    // JWT 문자열 생성
    public String generateToken(JwtUser jwtUser, long tokenValidityMilliseconds) {
        Date now = new Date();
        return Jwts.builder()
                .header()
                .type(constJwt.getBearerFormat())
                .and()
                .issuer(constJwt.getIssuer())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + tokenValidityMilliseconds))
                .claim(constJwt.getClaimKey(), makeClaimByUserToJson(jwtUser))
                .signWith(secretKey)
                .compact();
    }

    // JWT 사용자 claim 파싱
    public JwtUser getJwtUserFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String claimJson = claims.get(constJwt.getClaimKey(), String.class);
            return objectMapper.readValue(claimJson, JwtUser.class);
        } catch (Exception e) {
            log.debug("JWT 검증 실패: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    // JWT 사용자 claim 직렬화
    public String makeClaimByUserToJson(JwtUser jwtUser) {
        try {
            return objectMapper.writeValueAsString(jwtUser);
        } catch (Exception e) {
            throw new IllegalStateException("JWT 사용자 claim 직렬화 실패", e);
        }
    }

    // JWT 만료 여부 확인
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
