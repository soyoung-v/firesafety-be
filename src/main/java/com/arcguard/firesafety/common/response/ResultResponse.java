package com.arcguard.firesafety.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 모든 API의 성공/실패 응답 envelope
@Getter
@AllArgsConstructor
public class ResultResponse<T> {

    private String resultMessage;
    private T resultData;

    // 성공 응답 생성
    public static <T> ResultResponse<T> success(String message, T data) {
        return new ResultResponse<>(message, data);
    }

    // 실패 응답 생성
    public static <T> ResultResponse<T> error(String message) {
        return new ResultResponse<>(message, null);
    }
}
