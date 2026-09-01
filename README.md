# ArcGuard (아크가드) — 백엔드

현장 분전반 센서 디바이스가 보내는 데이터를 수신해 하드웨어 판정 + AI 아크 판정을 종합하고, 실시간 관제화면(WebSocket)과 경보(FCM 푸시 포함)로 이어주는 백엔드. 단일 Spring Boot 애플리케이션 안에서 도메인 패키지로 분리된 Modular Monolith 구조를 쓴다.

---

## 구조 (Modular Monolith)

| 패키지 | 역할 |
|---|---|
| `common` | 공통 응답(`ResultResponse`), 에러코드, JWT, 인증 컨텍스트 |
| `auth` | 로그인/로그아웃/재발급, 계정 CRUD·소프트삭제·복구, 비밀번호 재설정, 감사 로그 |
| `facility` | 현장/분전반/회로 CRUD, 담당현장배정, 설비 감사 로그 |
| `sensor` | 하드웨어 센서 데이터 수신(`GET /m_noUpload.php`), Mock 센서 스케줄러(발표/데모용) |
| `diagnosis` | AI 서버(`POST /predict`) 연동, 회로별 진단결과 |
| `monitoring` | 대시보드 요약, WebSocket(STOMP) 실시간 갱신, 분전반 상태 집계, 통신두절 감지 |
| `alert` | 경보 생성/조회, 상태 전이(미확인→확인→조치), FCM 발송, 엑셀 다운로드 |
| `statistics` | 기간별 통계 조회 |

경계는 네트워크 서비스가 아니라 패키지 기준이다. 도메인 간 협력은 Application/Domain Service로 처리하고, Controller에서 다른 도메인 Mapper를 직접 호출하지 않는다.

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4.0.7 |
| 인증 | Spring Security + JWT(HttpOnly Cookie, `jjwt` 0.12.6) |
| DB 접근 | MyBatis |
| DB | MySQL |
| 마이그레이션 | Flyway |
| 실시간 통신 | WebSocket (STOMP) |
| API 문서 | springdoc-openapi 2.8.9 |
| 엑셀 | Apache POI 5.3.0 (경보 이력 다운로드) |
| 푸시 알림 | Firebase Admin SDK 9.4.3 (FCM) |
| 외부 API 연동 | Spring Cloud OpenFeign (AI 예측 서버) |
| 메일 발송 | Spring Mail (SMTP, Gmail/SES 등 교체 가능) |
| 환경변수 로드 | dotenv-java 3.0.0 (`.env`를 UTF-8로 직접 읽어 인코딩 깨짐 방지) |
| 빌드 | Gradle |

---

## 실행

### 사전 요구사항

- Java 21
- MySQL (로컬 실행 기준, 원격 접속 정보도 가능)

### 1. 환경변수 설정

```bash
cp .env.example .env
# .env를 열어 DB_URL/DB_USERNAME/DB_PASSWORD, JWT_SECRET_KEY, MAIL_* 등 실제 값으로 채운다
```

`.env`는 Java Properties가 아니라 `dotenv-java`로 직접 읽으므로 한글 값을 그대로 써도 된다.

### 2. 서버 실행

```bash
./gradlew bootRun
```

기동 시 Flyway가 마이그레이션을 자동 적용하고, `BOOTSTRAP_SUPER_ADMIN_ENABLED=true`면 마스터 계정을 자동 생성한다.

### 3. 하드웨어 없이 데모하기 (Mock 센서)

```bash
SENSOR_MOCK_ENABLED=true   # .env에서 설정
SENSOR_MOCK_DELAY_MS=5000  # 생성 주기(ms)
```

활성 분전반이 있으면 실제 장비 없이도 센서 프레임 → DEVICE 경보 → 대시보드/WebSocket 갱신까지 기존 수신 흐름 그대로 태운다. 운영에서는 반드시 `false`.

### 4. 검증

```bash
./gradlew test
./gradlew clean build
```

### 문서/테스트 계정

| 항목 | 값 |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| 테스트 계정 (local) | `master@safety.com` / `master1234!` (SUPER_ADMIN, `BOOTSTRAP_SUPER_ADMIN_ENABLED=true`로 자동 생성) |

그 외 ADMIN/GENERAL 계정은 로그인 후 SUPER_ADMIN → ADMIN → GENERAL 순으로 직접 등록해서 사용한다.

---

## API

`GET /m_noUpload.php`는 표준 REST 컨벤션(`/api/...`)을 따르지 않는 예외 경로다. 센서 디바이스가 쿼리스트링(전부 String, 필드별 고정 자리수)으로 직접 호출하는 레거시 프로토콜이라 임의로 바꾸지 않는다.

```
GET /m_noUpload.php?m_no=00001&mode=0&volt=230&am1=023&count1=0000&...&tem=272&humi=484&...
→ { "frameId": 100, "receivedAt": "2026-07-30T12:00:00" }
```

- 필드 자리수 누락/불일치 시 `INVALID_FRAME_PARAMETER`(400, 원본 쿼리값은 로그에만 보관 — 하드웨어 원인 추적용).
- `tem`/`humi`는 영하 값이 올 수 있어 부호(`-`)를 제외한 나머지 자리수만 검증한다 (다른 필드는 전부 크기값이라 부호 허용 안 함).
- `m_no`에 해당하는 활성 분전반이 없으면 `PANEL_NOT_FOUND`(404).
- 회로가 삭제/미등록인 채널은 그 채널만 건너뛰고 나머지 채널/프레임 저장은 정상 진행한다.

나머지 API는 전부 `/api/...` 표준 REST, 상세 스펙은 Swagger 참고.

---

## 설계 근거

**분전반 상태(`panel.status`)는 조회 시점 계산이 아니라 컬럼에 집계·저장한다.** 회로 상태(하드웨어 판정 + 최신 AI 판정 조합)를 매 조회마다 다시 계산하면 대시보드/통계 조회가 느려진다. `PanelStatusAggregationService`가 프레임 수신 시점에 소속 회로 중 최고 위험도(+통신두절 `OFFLINE`)를 집계해 저장하고, 조회는 그 값을 그대로 읽는다.

**Enum은 DB에 영문 상수명으로 저장하고, 화면용 한글은 `label` 필드로만 관리한다.** 원본 설계 문서(엑셀)에는 `ENUM('정상','주의','위험')`처럼 한글 리터럴로 되어 있었지만, 한글 enum은 코드 리팩터링(이름 변경)에 취약하고 DB 마이그레이션 비용이 커서 영문 상수로 전환했다. 화면 표시는 별도 label 매핑으로 분리한다.

**스키마 변경은 Flyway로 버전 관리한다 (MyBatis + 워크벤치 수동 DDL 아님).** 이전 프로젝트에서 MyBatis + 워크벤치 직접 DDL 방식은 변경 이력이 흩어지고 팀원 간 공유가 안 되는 문제가 있었다. Flyway는 `V{n}__설명.sql` 파일로 이력을 남기고 Git으로 공유되며, 앱 기동 시 자동 적용돼 팀원 간 스키마 불일치를 구조적으로 막는다.

**신규 Flyway 마이그레이션은 로컬 Docker로 실제 DB에 적용해보고 검증한 뒤에만 "완료"로 본다.** 로컬 H2/인메모리 검증만으로는 실제 MySQL 마이그레이션 실행 순서·제약조건 문제를 못 잡는다.

---


