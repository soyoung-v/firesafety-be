package com.arcguard.firesafety.facility.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.facility.dto.req.PanelCreateReq;
import com.arcguard.firesafety.facility.dto.req.PanelListReq;
import com.arcguard.firesafety.facility.dto.req.PanelUpdateReq;
import com.arcguard.firesafety.facility.dto.res.PanelDetailRes;
import com.arcguard.firesafety.facility.dto.res.PanelCreateRes;
import com.arcguard.firesafety.facility.dto.res.PanelListPageRes;
import com.arcguard.firesafety.facility.dto.res.PanelUpdateRes;
import com.arcguard.firesafety.facility.service.PanelService;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비관리-분전반", description = "분전반 조회, 등록, 수정, 소프트 삭제")
public class PanelController {

    private final PanelService panelService;

    // 분전반 목록 조회 (GET /api/panels)
    // siteId/status/keyword 필터 + page/size 페이징 지원
    @Operation(summary = "분전반 목록 조회", description = "siteId/status/keyword(분전반명 부분일치) 필터와 page/size 페이징을 지원한다. 삭제된 분전반과 삭제된 현장 소속 분전반은 제외한다.")
    @GetMapping("/panels")
    public ResultResponse<PanelListPageRes> getPanels(@ModelAttribute PanelListReq req) {
        PanelListPageRes panels = panelService.getPanels(req);
        return ResultResponse.success(String.format("%d rows", panels.getContent().size()), panels);
    }

    // 분전반 상세 조회 (GET /api/panels/{panelId})
    // 삭제된 분전반과 삭제된 현장 소속 분전반은 제외
    @Operation(summary = "분전반 상세 조회", description = "삭제된 분전반과 삭제된 현장 소속 분전반은 조회하지 않는다.")
    @GetMapping("/panels/{panelId}")
    public ResultResponse<PanelDetailRes> getPanel(@PathVariable Long panelId) {
        PanelDetailRes panel = panelService.getPanel(panelId);
        return ResultResponse.success("분전반 상세 조회 성공", panel);
    }

    // 분전반 등록 (POST /api/sites/{siteId}/panels)
    // ADMIN 이상 가능, ADMIN은 배정된 현장에만 등록
    @Operation(summary = "분전반 등록", description = "ADMIN 이상 가능. ADMIN은 본인 담당 현장에만 등록할 수 있다.")
    @PostMapping("/sites/{siteId}/panels")
    public ResultResponse<PanelCreateRes> createPanel(@PathVariable Long siteId, @RequestBody PanelCreateReq req) {
        PanelCreateRes panel = panelService.createPanel(siteId, req);
        return ResultResponse.success("분전반 등록 성공", panel);
    }

    // 분전반 수정 (PUT /api/panels/{panelId})
    // ADMIN 이상 가능, 기본 정보와 임계치만 수정
    @Operation(summary = "분전반 수정", description = "ADMIN 이상 가능. 기본 정보와 서버 관제용 주의 기준값을 수정한다.")
    @PutMapping("/panels/{panelId}")
    public ResultResponse<PanelUpdateRes> updatePanel(@PathVariable Long panelId, @RequestBody PanelUpdateReq req) {
        PanelUpdateRes panel = panelService.updatePanel(panelId, req);
        return ResultResponse.success("분전반 수정 성공", panel);
    }

    // 분전반 삭제 (DELETE /api/panels/{panelId})
    // 물리 삭제하지 않고 deleted_at만 기록
    @Operation(summary = "분전반 삭제", description = "물리 삭제하지 않고 deleted_at을 기록한다. 일반 활성 목록에서는 하위 회로와 함께 제외된다.")
    @DeleteMapping("/panels/{panelId}")
    public ResultResponse<Void> deletePanel(@PathVariable Long panelId) {
        panelService.deletePanel(panelId);
        return ResultResponse.success("분전반 삭제 성공", null);
    }
}
