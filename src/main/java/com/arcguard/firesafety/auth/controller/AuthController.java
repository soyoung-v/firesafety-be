package com.arcguard.firesafety.auth.controller;

import com.arcguard.firesafety.auth.dto.req.LoginReq;
import com.arcguard.firesafety.auth.dto.req.PasswordResetConfirmReq;
import com.arcguard.firesafety.auth.dto.req.PasswordResetRequestReq;
import com.arcguard.firesafety.auth.dto.res.LoginRes;
import com.arcguard.firesafety.auth.service.AuthService;
import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "로그인, 로그아웃, 토큰 재발급, 비밀번호 재설정")
public class AuthController {

    private final AuthService authService;

    // 로그인 (POST /api/auth/login)
    // 성공 시 AT/RT를 HttpOnly Cookie로 발급, body에는 사용자 정보만 반환
    @Operation(summary = "로그인", description = "성공 시 at/rt HttpOnly Cookie를 Set-Cookie로 발급하고, 응답 body에는 사용자 정보만 반환한다.")
    @PostMapping("/login")
    public ResultResponse<LoginRes> login(@Valid @RequestBody LoginReq req, HttpServletResponse response) {
        LoginRes loginRes = authService.login(req, response);
        return ResultResponse.success("로그인 성공", loginRes);
    }

    // 로그아웃 (POST /api/auth/logout)
    // DB의 현재 RT를 폐기하고 AT/RT 쿠키를 만료 처리
    @Operation(summary = "로그아웃", description = "현재 Refresh Token을 폐기하고 at/rt 쿠키를 만료 처리한다.")
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
    @PostMapping("/logout")
    public ResultResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResultResponse.success("로그아웃 성공", null);
    }

    // 토큰 재발급 (POST /api/auth/reissue)
    // 쿠키의 RT를 검증해 새 AT 쿠키 발급
    @Operation(summary = "토큰 재발급", description = "rt HttpOnly Cookie를 검증하고 새 at HttpOnly Cookie를 발급한다.")
    @SecurityRequirement(name = OpenApiConfig.REFRESH_TOKEN_COOKIE)
    @PostMapping("/reissue")
    public ResultResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        authService.reissue(request, response);
        return ResultResponse.success("토큰 재발급 성공", null);
    }

    // 비밀번호 재설정 요청 (POST /api/auth/password-reset/request)
    // 계정 존재 여부와 관계없이 같은 응답 반환
    @Operation(summary = "비밀번호 재설정 요청", description = "이메일 존재 여부와 관계없이 같은 성공 응답을 반환한다.")
    @PostMapping("/password-reset/request")
    public ResultResponse<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestReq req,
                                                     HttpServletRequest request) {
        authService.requestPasswordReset(req, request);
        return ResultResponse.success("비밀번호 재설정 안내 메일을 발송했습니다", null);
    }

    // 비밀번호 재설정 확정 (POST /api/auth/password-reset/confirm)
    // 메일 링크의 일회용 토큰 검증 후 새 비밀번호 저장
    @Operation(summary = "비밀번호 재설정 확정", description = "일회용 토큰 검증 후 새 비밀번호를 BCrypt로 저장하고 기존 Refresh Token을 폐기한다.")
    @PostMapping("/password-reset/confirm")
    public ResultResponse<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmReq req) {
        authService.confirmPasswordReset(req);
        return ResultResponse.success("비밀번호가 변경되었습니다", null);
    }
}
