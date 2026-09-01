package com.arcguard.firesafety.facility.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.config.swagger.OpenApiConfig;
import com.arcguard.firesafety.facility.dto.res.AddressSearchPageRes;
import com.arcguard.firesafety.facility.service.AddressSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facilities/address")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비관리-주소검색", description = "행정안전부 도로명주소 검색(현장 등록용, SUPER_ADMIN 전용)")
public class AddressSearchController {

    private final AddressSearchService addressSearchService;

    // 주소 검색 (GET /api/facilities/address/search)
    // 현장 등록 화면에서 키워드로 도로명주소를 검색해 선택할 수 있게 제공
    @Operation(summary = "주소 검색", description = "행정안전부 도로명주소 API로 키워드 검색 결과를 반환한다. SUPER_ADMIN 전용.")
    @GetMapping("/search")
    public ResultResponse<AddressSearchPageRes> searchAddress(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResultResponse.success("주소 검색 성공", addressSearchService.searchAddress(keyword, page, size));
    }
}
