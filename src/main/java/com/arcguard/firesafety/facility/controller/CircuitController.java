package com.arcguard.firesafety.facility.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.facility.dto.req.CircuitCreateReq;
import com.arcguard.firesafety.facility.dto.req.CircuitUpdateReq;
import com.arcguard.firesafety.facility.dto.res.CircuitCreateRes;
import com.arcguard.firesafety.facility.dto.res.CircuitDetailRes;
import com.arcguard.firesafety.facility.dto.res.CircuitListRes;
import com.arcguard.firesafety.facility.service.CircuitService;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비관리-회로", description = "회로 조회, 등록, 소프트 삭제")
public class CircuitController {

    private final CircuitService circuitService;

    // 회로 등록 (POST /api/panels/{panelId}/circuits)
    // ADMIN 이상 가능, 채널은 분전반 회로 개수 범위 안에서만 등록
    @Operation(summary = "회로 등록", description = "ADMIN 이상 가능. 채널 번호는 분전반 circuitCount 범위 안에서만 등록할 수 있다.")
    @PostMapping("/panels/{panelId}/circuits")
    public ResultResponse<CircuitCreateRes> createCircuit(@PathVariable Long panelId, @RequestBody CircuitCreateReq req) {
        CircuitCreateRes circuit = circuitService.createCircuit(panelId, req);
        return ResultResponse.success("회로 등록 성공", circuit);
    }

    // 회로 목록 조회 (GET /api/panels/{panelId}/circuits)
    // 삭제되지 않은 분전반 아래의 활성 회로만 반환
    @Operation(summary = "회로 목록 조회", description = "삭제되지 않은 분전반 아래의 활성 회로만 조회한다.")
    @GetMapping("/panels/{panelId}/circuits")
    public ResultResponse<List<CircuitListRes>> getCircuits(@PathVariable Long panelId) {
        List<CircuitListRes> circuits = circuitService.getCircuits(panelId);
        return ResultResponse.success(String.format("%d rows", circuits.size()), circuits);
    }

    // 회로 상세 조회 (GET /api/circuits/{circuitId})
    // 삭제된 회로 또는 삭제된 상위 설비에 속한 회로는 조회하지 않음
    @Operation(summary = "회로 상세 조회", description = "삭제된 회로 또는 삭제된 상위 설비에 속한 회로는 조회하지 않는다.")
    @GetMapping("/circuits/{circuitId}")
    public ResultResponse<CircuitDetailRes> getCircuit(@PathVariable Long circuitId) {
        CircuitDetailRes circuit = circuitService.getCircuit(circuitId);
        return ResultResponse.success("회로 상세 조회 성공", circuit);
    }

    // 회로 부하종류 수정 (PUT /api/circuits/{circuitId})
    // ADMIN 이상 가능, channelNo/panelId는 변경 불가 — loadType만 수정
    @Operation(summary = "회로 부하종류 수정", description = "ADMIN 이상 가능. loadType만 수정할 수 있다(채널번호/분전반은 변경 불가).")
    @PutMapping("/circuits/{circuitId}")
    public ResultResponse<CircuitDetailRes> updateCircuit(@PathVariable Long circuitId, @RequestBody CircuitUpdateReq req) {
        CircuitDetailRes circuit = circuitService.updateCircuit(circuitId, req);
        return ResultResponse.success("회로 수정 성공", circuit);
    }

    // 회로 삭제 (DELETE /api/circuits/{circuitId})
    // 물리 삭제하지 않고 deleted_at만 기록
    @Operation(summary = "회로 삭제", description = "물리 삭제하지 않고 deleted_at을 기록한다.")
    @DeleteMapping("/circuits/{circuitId}")
    public ResultResponse<Void> deleteCircuit(@PathVariable Long circuitId) {
        circuitService.deleteCircuit(circuitId);
        return ResultResponse.success("회로 삭제 성공", null);
    }
}
