package com.arcguard.firesafety.system.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import com.arcguard.firesafety.system.dto.req.SystemReleaseCreateReq;
import com.arcguard.firesafety.system.dto.res.SystemReleasePageRes;
import com.arcguard.firesafety.system.dto.res.SystemVersionRes;
import com.arcguard.firesafety.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "시스템관리", description = "SW 버전 정보 등 시스템 관리용 API")
public class SystemController {

    private final SystemService systemService;

    // SW 버전 정보 요약 조회 (GET /api/system/version, REQ-702) — 로그인한 사용자면 조회 가능
    @Operation(summary = "SW 버전 정보 요약 조회", description = "등록된 이력 중 최신 소프트웨어/AI 모델 버전을 반환한다.")
    @GetMapping("/version")
    public ResultResponse<SystemVersionRes> getVersion() {
        return ResultResponse.success("조회 성공", systemService.getVersion());
    }

    // 업데이트 이력 목록 조회 (GET /api/system/releases)
    @Operation(summary = "업데이트 이력 목록 조회", description = "소프트웨어/AI 모델 업데이트 이력을 최신순으로 페이지 조회한다.")
    @GetMapping("/releases")
    public ResultResponse<SystemReleasePageRes> getReleases(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        SystemReleasePageRes releases = systemService.getReleases(page, size);
        return ResultResponse.success(String.format("%d rows", releases.getContent().size()), releases);
    }

    // 업데이트 이력 등록 (POST /api/system/releases) — SUPER_ADMIN 전용
    @Operation(summary = "업데이트 이력 등록", description = "소프트웨어 또는 AI 모델 릴리즈 이력을 등록한다. SUPER_ADMIN 전용.")
    @PostMapping("/releases")
    public ResultResponse<Void> createRelease(@Valid @RequestBody SystemReleaseCreateReq req) {
        systemService.createRelease(req);
        return ResultResponse.success("업데이트 이력 등록 성공", null);
    }
}
