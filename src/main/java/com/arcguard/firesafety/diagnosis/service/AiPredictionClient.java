package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.diagnosis.dto.req.AiPredictionReq;
import com.arcguard.firesafety.diagnosis.dto.res.AiPredictionRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "aiPredictionClient", url = "${constants.ai-prediction.base-url:http://localhost:8000}")
public interface AiPredictionClient {

    // 외부 Python AI 서버에 예측 요청
    @PostMapping("${constants.ai-prediction.path:/predict}")
    AiPredictionRes predict(@RequestBody AiPredictionReq request);

}
