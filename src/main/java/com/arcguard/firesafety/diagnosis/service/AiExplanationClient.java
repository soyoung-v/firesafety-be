package com.arcguard.firesafety.diagnosis.service;

import com.arcguard.firesafety.diagnosis.dto.req.AiExplanationReq;
import com.arcguard.firesafety.diagnosis.dto.res.AiExplanationRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// firesafety-ai와 같은 서버(AiPredictionClient와 동일 base-url)를 쓰므로 별도 base-url을 만들지 않는다.
@FeignClient(name = "aiExplanationClient", url = "${constants.ai-prediction.base-url:http://localhost:8000}")
public interface AiExplanationClient {

    // 외부 Python AI 서버에 진단 설명 요청
    @PostMapping("${constants.ai-explanation.path:/explain}")
    AiExplanationRes explain(@RequestBody AiExplanationReq request);

}
