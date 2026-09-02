-- V23__add_ai_diagnosis_result_analysis_summary.sql
-- Phase 11: LLM(firesafety-ai POST /explain)이 생성한 진단 설명을 저장하는 캐시 컬럼.
-- 진단 생성 시점에는 채워지지 않는다(자동 호출 없음, ADR-013) - 사용자가 설명 생성을 요청한 뒤
-- 최초 1회만 채워지고, 이후 요청은 이 컬럼 값을 그대로 반환한다(외부 API 재호출 방지 캐시).
-- 길이를 특정 자릿수로 제한할 이유가 없어(짧은 한국어 2~4문장이지만 상한을 미리 좁게 못박을 근거 없음)
-- VARCHAR 대신 TEXT를 쓴다. 검색/정렬 대상이 아니라 인덱스도 만들지 않는다.
-- 기존 row는 전부 NULL로 유지되며 기존 데이터와 완전 호환된다.

ALTER TABLE ai_diagnosis_result
    ADD COLUMN analysis_summary TEXT NULL AFTER predicted_current;
