-- V22__add_ai_diagnosis_result_extended_ai_fields.sql
-- firesafety-ai Phase 7~8에서 완성된 Risk Classifier/Anomaly Detector/Current Regressor 결과를
-- 저장하기 위한 확장 컬럼. 기존 verdict/confidence(Legacy ARC 판정)는 그대로 유지하고,
-- 신규 결과는 별도 컬럼에 저장한다 - DANGER를 ARC로, anomaly=true를 ARC로 강제 변환하지 않는다(ADR-002).
-- 전부 NULL 허용 - request에 context가 없거나 불충분하면 AI 서버가 null을 반환하고 그대로 저장한다.

ALTER TABLE ai_diagnosis_result
    ADD COLUMN risk_level ENUM('NORMAL', 'WARNING', 'DANGER') NULL AFTER trigger_type,
    ADD COLUMN risk_score FLOAT NULL AFTER risk_level,
    ADD COLUMN anomaly TINYINT(1) NULL AFTER risk_score,
    ADD COLUMN anomaly_score FLOAT NULL AFTER anomaly,
    ADD COLUMN predicted_current DECIMAL(6, 3) NULL AFTER anomaly_score,
    ADD CONSTRAINT chk_ai_diagnosis_result_risk_score CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 1)),
    ADD CONSTRAINT chk_ai_diagnosis_result_anomaly CHECK (anomaly IS NULL OR anomaly IN (0, 1)),
    ADD CONSTRAINT chk_ai_diagnosis_result_anomaly_score CHECK (anomaly_score IS NULL OR (anomaly_score >= 0 AND anomaly_score <= 1));
