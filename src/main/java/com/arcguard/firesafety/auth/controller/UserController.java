package com.arcguard.firesafety.auth.controller;

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
import com.arcguard.firesafety.auth.service.UserService;
import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "회원관리", description = "관리자 계정 생성, 수정, 소프트 삭제, 복구, 감사 이력")
public class UserController {

    private final UserService userService;

    // 계정 목록 조회 (GET /api/users)
    // SUPER_ADMIN 전용, 삭제된 사용자는 제외
    @Operation(summary = "계정 목록 조회", description = "SUPER_ADMIN 전용. 삭제된 사용자는 일반 목록에서 제외한다.")
    @GetMapping
    public ResultResponse<List<UserListRes>> getUsers() {
        List<UserListRes> users = userService.getUsers();
        return ResultResponse.success(String.format("%d rows", users.size()), users);
    }

    // 계정 변경 이력 조회 (GET /api/users/audit-logs)
    // SUPER_ADMIN 전용, user_audit_log 최신순 조회
    @Operation(summary = "계정 변경 이력 조회", description = "SUPER_ADMIN 전용. 사용자 생성/수정/삭제/복구/비밀번호 변경 이력을 조회한다.")
    @GetMapping("/audit-logs")
    public ResultResponse<List<UserAuditLogRes>> getUserAuditLogs() {
        List<UserAuditLogRes> logs = userService.getUserAuditLogs();
        return ResultResponse.success(String.format("%d rows", logs.size()), logs);
    }

    // FCM 토큰 등록 (PATCH /api/users/me/fcm-token)
    // 프론트에서 발급받은 현재 기기 토큰을 로그인 사용자에게 저장
    @Operation(summary = "FCM 토큰 등록", description = "로그인 사용자의 현재 기기 FCM 토큰을 저장한다.")
    @PatchMapping("/me/fcm-token")
    public ResultResponse<Void> updateFcmToken(@RequestBody FcmTokenReq req) {
        userService.updateFcmToken(req);
        return ResultResponse.success("FCM 토큰 등록 성공", null);
    }

    // FCM 토큰 해제 (DELETE /api/users/me/fcm-token)
    // 사용자가 이 기기에서 푸시 알림을 끌 때 호출 — 등록과 동일하게 요청 본문의 토큰값 기준으로 삭제
    @Operation(summary = "FCM 토큰 해제", description = "로그인 사용자의 현재 기기 FCM 토큰을 삭제해 더 이상 푸시가 가지 않게 한다.")
    @DeleteMapping("/me/fcm-token")
    public ResultResponse<Void> deleteFcmToken(@RequestBody FcmTokenReq req) {
        userService.deleteFcmToken(req);
        return ResultResponse.success("FCM 토큰 해제 성공", null);
    }

    // 이메일 중복확인 (GET /api/users/check-email)
    // 계정 등록 폼에서 실시간으로 사용, ADMIN 이상만 호출 가능
    @Operation(summary = "이메일 중복확인", description = "계정 등록 폼에서 실시간으로 이메일 중복 여부를 확인한다. ADMIN 이상만 호출 가능하다.")
    @GetMapping("/check-email")
    public ResultResponse<EmailCheckRes> checkEmail(@RequestParam String email) {
        EmailCheckRes result = userService.checkEmailDuplicate(email);
        return ResultResponse.success("이메일 중복확인 성공", result);
    }

    // 계정 등록 (POST /api/users)
    // 공개 회원가입이 아니라 관리자가 하위 계정을 생성
    @Operation(summary = "계정 등록",
            description = "공개 회원가입이 아니라 관리자가 ADMIN/GENERAL 계정을 생성한다. SUPER_ADMIN 계정은 생성할 수 없다. 아이디는 이메일 형식이다.")
    @PostMapping
    public ResultResponse<UserCreateRes> createUser(@Valid @RequestBody UserCreateReq req) {
        UserCreateRes user = userService.createUser(req);
        return ResultResponse.success("계정 등록 성공", user);
    }

    // 계정 수정 (PUT /api/users/{userId})
    // 대상 등급과 수정 후 등급을 함께 권한 검증
    @Operation(summary = "계정 수정",
            description = "대상 등급과 수정 후 등급을 함께 권한 검증한다. ADMIN은 같은 활성 현장에 배정된 ADMIN/GENERAL만 수정할 수 있다.")
    @PutMapping("/{userId}")
    public ResultResponse<UserUpdateRes> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateReq req) {
        UserUpdateRes user = userService.updateUser(userId, req);
        return ResultResponse.success("계정 수정 성공", user);
    }

    // 계정 삭제 (DELETE /api/users/{userId})
    // 소프트 삭제 처리 후 대상 RT 전체 폐기
    @Operation(summary = "계정 삭제",
            description = "물리 삭제하지 않고 account_status/deleted_at을 기록하며 대상 사용자의 Refresh Token을 모두 폐기한다. ADMIN은 같은 활성 현장에 배정된 ADMIN/GENERAL만 삭제할 수 있다.")
    @DeleteMapping("/{userId}")
    public ResultResponse<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResultResponse.success("계정 삭제 성공", null);
    }

    // 계정 일괄 삭제 (PATCH /api/users/bulk-delete)
    // 대상 중 하나라도 삭제 불가하면 전체 롤백
    @Operation(summary = "계정 일괄 삭제",
            description = "대상 중 자기 자신, 권한 밖, 이미 삭제된 사용자가 포함되면 전체 롤백한다. ADMIN은 같은 활성 현장에 배정된 ADMIN/GENERAL만 삭제할 수 있다.")
    @PatchMapping("/bulk-delete")
    public ResultResponse<UserBulkDeleteRes> deleteUsers(@RequestBody UserBulkDeleteReq req) {
        UserBulkDeleteRes result = userService.deleteUsers(req);
        return ResultResponse.success("계정 일괄 삭제 성공", result);
    }

    // 계정 복구 (PATCH /api/users/{userId}/restore)
    // 삭제된 계정을 ACTIVE 상태로 전환
    @Operation(summary = "계정 복구", description = "삭제된 계정을 ACTIVE 상태로 전환한다. ADMIN은 GENERAL만, SUPER_ADMIN은 ADMIN/GENERAL을 복구할 수 있다.")
    @PatchMapping("/{userId}/restore")
    public ResultResponse<UserUpdateRes> restoreUser(@PathVariable Long userId) {
        UserUpdateRes user = userService.restoreUser(userId);
        return ResultResponse.success("계정 복구 성공", user);
    }
}
