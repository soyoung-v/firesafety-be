package com.arcguard.firesafety.sensor.mapper;

import com.arcguard.firesafety.sensor.model.SensorFrame;
import org.apache.ibatis.annotations.Mapper;

// 디바이스 1회 수신 프레임(sensor_frame) 저장/조회용 MyBatis Mapper
@Mapper
public interface SensorFrameMapper {

    // 디바이스 1회 전송분 공통 필드 저장
    void insertSensorFrame(SensorFrame sensorFrame);
}
