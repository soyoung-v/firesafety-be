package com.arcguard.firesafety.facility.service;

import com.arcguard.firesafety.facility.dto.res.JusoAddressApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 행정안전부 도로명주소 검색 API(Juso.go.kr) 연동
@FeignClient(name = "jusoAddressClient", url = "${constants.address-search.search-url:https://business.juso.go.kr/addrlink/addrLinkApi.do}")
public interface JusoAddressClient {

    @GetMapping
    JusoAddressApiResponse search(@RequestParam("confmKey") String apiKey,
                                   @RequestParam("currentPage") int currentPage,
                                   @RequestParam("countPerPage") int countPerPage,
                                   @RequestParam("keyword") String keyword,
                                   @RequestParam("resultType") String resultType);
}
