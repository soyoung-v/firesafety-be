package com.arcguard.firesafety.facility.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 행정안전부 도로명주소 검색 API 원본 응답
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JusoAddressApiResponse {

    private Results results;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Results {
        private Common common;
        private List<Juso> juso;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Common {
        private String errorCode;
        private String errorMessage;
        private String totalCount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Juso {
        private String roadAddr;
        private String jibunAddr;
        private String bdNm;
        private String zipNo;
    }
}
