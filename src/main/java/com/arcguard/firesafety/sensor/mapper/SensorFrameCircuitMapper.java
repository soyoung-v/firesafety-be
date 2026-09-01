package com.arcguard.firesafety.sensor.mapper;

import com.arcguard.firesafety.sensor.model.SensorFrameCircuit;
import org.apache.ibatis.annotations.Mapper;

// 프레임별 회로 측정값(sensor_frame_circuit) 저장/조회용 MyBatis Mapper
@Mapper
public interface SensorFrameCircuitMapper {

    // 프레임별 회로 측정값 저장
    void insertSensorFrameCircuit(SensorFrameCircuit sensorFrameCircuit);
}
