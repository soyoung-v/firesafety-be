package com.arcguard.firesafety.facility.service;

import com.arcguard.firesafety.auth.model.UserRole;
import com.arcguard.firesafety.common.exception.BusinessException;
import com.arcguard.firesafety.common.exception.CommonErrorCode;
import com.arcguard.firesafety.common.security.UserPrincipal;
import com.arcguard.firesafety.facility.config.AddressSearchProperties;
import com.arcguard.firesafety.facility.dto.res.AddressSearchPageRes;
import com.arcguard.firesafety.facility.dto.res.AddressSearchRes;
import com.arcguard.firesafety.facility.dto.res.JusoAddressApiResponse;
import com.arcguard.firesafety.facility.exception.FacilityErrorCode;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// 행정안전부 도로명주소 검색(현장 등록 시 주소 자동완성용). 현장 등록이 SUPER_ADMIN 전용(REQ-501)이라 동일하게 제한한다.
@Service
@Slf4j
@RequiredArgsConstructor
public class AddressSearchService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 6;
    private static final int MAX_SIZE = 20;

    private final JusoAddressClient jusoAddressClient;
    private final AddressSearchProperties addressSearchProperties;

    public AddressSearchPageRes searchAddress(String keyword, Integer page, Integer size) {
        requireSuperAdmin(getCurrentUser());

        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(FacilityErrorCode.ADDRESS_SEARCH_KEYWORD_REQUIRED);
        }
        String trimmedKeyword = keyword.trim();

        int normalizedPage = page == null ? DEFAULT_PAGE : page;
        int normalizedSize = size == null ? DEFAULT_SIZE : size;
        if (normalizedPage < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        if (normalizedSize < 1 || normalizedSize > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }

        if (!addressSearchProperties.isReady()) {
            log.error("행안부 주소 검색 API 키 설정이 비어 있습니다.");
            throw new BusinessException(FacilityErrorCode.EXTERNAL_ADDRESS_API_ERROR);
        }

        JusoAddressApiResponse response;
        try {
            response = jusoAddressClient.search(
                    addressSearchProperties.getApiKey(),
                    normalizedPage + 1,
                    normalizedSize,
                    trimmedKeyword,
                    "json"
            );
        } catch (Exception exception) {
            log.error("주소 검색 외부 API 호출 실패 — keyword={}", trimmedKeyword, exception);
            throw new BusinessException(FacilityErrorCode.EXTERNAL_ADDRESS_API_ERROR);
        }

        if (response == null || response.getResults() == null || response.getResults().getCommon() == null) {
            log.warn("주소 검색 외부 API 응답 구조가 비정상입니다.");
            throw new BusinessException(FacilityErrorCode.EXTERNAL_ADDRESS_API_ERROR);
        }

        if (!"0".equals(response.getResults().getCommon().getErrorCode())) {
            log.warn("주소 검색 외부 API가 오류를 반환했습니다. errorCode={}, errorMessage={}",
                    response.getResults().getCommon().getErrorCode(),
                    response.getResults().getCommon().getErrorMessage());
            throw new BusinessException(FacilityErrorCode.EXTERNAL_ADDRESS_API_ERROR);
        }

        List<JusoAddressApiResponse.Juso> jusoList = response.getResults().getJuso();
        long totalElements = parseTotalCount(response.getResults().getCommon().getTotalCount());
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);
        boolean hasNext = normalizedPage + 1 < totalPages;

        List<AddressSearchRes> content = jusoList == null ? Collections.emptyList()
                : jusoList.stream().map(this::toResponse).toList();

        return AddressSearchPageRes.builder()
                .content(content)
                .page(normalizedPage)
                .size(normalizedSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .build();
    }

    private AddressSearchRes toResponse(JusoAddressApiResponse.Juso juso) {
        String address = StringUtils.hasText(juso.getRoadAddr()) ? juso.getRoadAddr() : defaultString(juso.getJibunAddr());
        return AddressSearchRes.builder()
                .address(address)
                .zipCode(defaultString(juso.getZipNo()))
                .buildingName(defaultString(juso.getBdNm()))
                .build();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    // SUPER_ADMIN 권한 확인
    private void requireSuperAdmin(UserPrincipal actor) {
        if (!UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }

    private long parseTotalCount(String totalCount) {
        if (!StringUtils.hasText(totalCount)) {
            return 0L;
        }
        try {
            return Long.parseLong(totalCount);
        } catch (NumberFormatException exception) {
            log.warn("주소 검색 외부 API totalCount가 숫자가 아닙니다. totalCount={}", totalCount, exception);
            throw new BusinessException(FacilityErrorCode.EXTERNAL_ADDRESS_API_ERROR);
        }
    }
}
