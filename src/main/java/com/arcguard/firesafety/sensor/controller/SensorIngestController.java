package com.arcguard.firesafety.sensor.controller;

import com.arcguard.firesafety.common.response.ResultResponse;
import com.arcguard.firesafety.sensor.dto.req.SensorFrameIngestReq;
import com.arcguard.firesafety.sensor.dto.res.SensorFrameIngestRes;
import com.arcguard.firesafety.sensor.service.SensorIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "데이터수집", description = "센서 디바이스 데이터 수신")
public class SensorIngestController {

    private final SensorIngestService sensorIngestService;

    // 레거시 센서 디바이스 수신 엔드포인트 (GET /m_noUpload.php)
    // 장비 프로토콜 고정 경로라서 /api prefix를 붙이지 않는다.
    @Operation(summary = "디바이스 센서 데이터 수신", description = "레거시 센서 프로토콜 고정 경로다. 표준 /api prefix를 붙이지 않으며, 쿼리스트링으로 10회로 센서값을 전달받는다. 디바이스 인증 방식은 추후 확정한다.")
    @GetMapping("/m_noUpload.php")
    public ResultResponse<SensorFrameIngestRes> ingest(@ParameterObject SensorFrameIngestReq req,
                                                       @Parameter(hidden = true) @RequestParam Map<String, String> params) {
        SensorFrameIngestRes result = sensorIngestService.ingest(params);
        return ResultResponse.success("센서 데이터 수신 성공", result);
    }
}
