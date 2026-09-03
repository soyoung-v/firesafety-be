# ArcGuard 배포 (GHCR 기반 CI/CD)

```
GitHub main push
  → GitHub Actions (test/build)
  → Docker image → GHCR push
  → EC2: docker compose pull <service> && docker compose up -d --no-deps <service>
  → health check
```

EC2는 소스를 빌드하지 않는다(`git pull`/`npm install`/`gradle build`/`pip install` 안 함) - GHCR
image를 pull해서 실행만 한다. 3개 repo(frontend/backend/ai-service)는 각자 독립적으로 배포되며,
한 repo에 push해도 다른 서비스는 재배포되지 않는다.

## 최종 런타임 구조

```
Internet
   ↓ 80 / 443
  Nginx (Docker)
   ├─ /            → frontend (Docker, 내부망)
   ├─ /api/*       → backend (Docker, 내부망)
   ├─ /ws/*        → backend (WebSocket)
   ├─ /m_noUpload.php → backend (레거시 센서 수신)
   └─ (없음)        ai-service는 외부에 노출하지 않는다

backend  → mysql(Docker, 내부망), ai-service(Docker, 내부망)
ai-service → OpenAI API (LLM_EXPLANATION_ENABLED=true일 때만)
```

- backend/ai-service/mysql은 호스트 포트로 노출되지 않는다(mysql만 SSH 터널용으로
  `127.0.0.1:23306`에 바인딩 - 인터넷에는 절대 노출되지 않는다).
- RDS는 쓰지 않는다. MySQL은 `mysql:8.0` 컨테이너 + named volume(`arcguard-mysql-data`)으로
  운영하며, `docker compose down -v`는 이 스택 어디에서도 실행하지 않는다(이 volume이 삭제됨).

## EC2 최초 1회 준비

```
~/arcguard/                       # 배포 디렉터리(이 repo의 docker-compose.yml + deploy/만 필요)
├── docker-compose.yml
├── deploy/
│   ├── nginx/{nginx.conf,conf.d/,certs/}
│   └── scripts/{health-check.sh,backup-db.sh,rollback.sh}
├── .env.production                # 실값. .env.production.example 기준으로 직접 채운다. chmod 600
└── secrets/
    └── firebase-credentials.json  # Firebase Admin SDK JSON. Git/GHCR/image 어디에도 없음, SFTP로 직접 올림
```

`docker-compose.yml`/`deploy/`는 이 repo(firesafety-be)의 파일을 그대로 서버에 복사한다(git clone
전체가 아니라 이 두 가지만 - EC2는 source build machine이 아니다). `.env.production`은
`.env.production.example`(firesafety-be)과 firesafety-ai의 `.env.production.example`을 합쳐서
채운다(비밀번호/키 등 실값은 여기 문서에도, 어떤 git repo에도 넣지 않는다).

```bash
chmod 600 ~/arcguard/.env.production
```

## GitHub Actions Secrets (각 repo의 Settings → Secrets and variables → Actions에서 등록)

3개 repo(firesafety-fe-react / firesafety-be / firesafety-ai) 전부 동일하게 등록:

| Secret | 값 |
|---|---|
| `EC2_HOST` | EC2 Public IP 또는 도메인 (Elastic IP 권장 - 재시작마다 바뀌는 임시 IP를 쓰면 이 값을 매번 갱신해야 함) |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_PRIVATE_KEY` | EC2 키페어(`fairway-key`)의 **개인키 파일 내용 전체**(`-----BEGIN ... PRIVATE KEY-----`부터 `END`까지) |

`GITHUB_TOKEN`(GHCR push/pull용)은 별도 등록 불필요 - GitHub Actions가 자동 제공하는 토큰을 그대로
쓴다(`packages: write` 권한은 워크플로 파일의 `permissions:`에 이미 선언돼 있음).

**private key 내용을 채팅으로 붙여넣지 않는다** - GitHub UI(Settings → Secrets)에 직접 입력한다.

## GHCR package 공개 여부

첫 push 시 GHCR package는 기본 **private**로 생성된다. EC2가 private image를 pull하려면 EC2에서도
`docker login ghcr.io`가 필요한데, 이번 구조는 EC2에 장기 자격증명을 두지 않는 방향을 우선했다(대신
GitHub Actions가 이미 인증된 상태로 pull까지 SSH 안에서 수행하지 않고, **compose up 자체는 EC2에서
직접 `docker compose pull`을 실행**하므로 EC2도 GHCR 인증이 필요하다는 점에 주의).

포트폴리오 프로젝트 특성상, **소스 공개 여부와 별개로 image만 public으로 바꾸는 것**을 권장한다
(Package Settings → Change visibility → Public). Public으로 바꾸면 EC2에서 별도 로그인 없이
`docker compose pull`이 그대로 된다. Private로 유지하고 싶다면 EC2에 `docker login ghcr.io`용 PAT를
별도로 준비해야 한다(이번 Phase에서는 만들지 않음 - 필요 여부를 먼저 결정할 것).

## 첫 배포 (수동, 1회)

Actions가 아직 한 번도 안 돌았다면 GHCR에 image가 없다 - 처음에는 로컬에서 build+push하거나,
그냥 각 repo에 아무 커밋이나 main에 push해서 워크플로가 처음부터 GHCR push까지 하게 만든다.

이미지가 GHCR에 있는 상태에서 EC2 최초 기동:

```bash
cd ~/arcguard
docker compose --env-file .env.production pull
docker compose --env-file .env.production up -d
./deploy/scripts/health-check.sh all
```

## 부분 재배포 (평소, CI가 자동으로 함)

```bash
export IMAGE_TAG=<커밋 SHA 12자리>   # CI가 자동으로 넣어줌, 수동이면 직접 지정
docker compose --env-file .env.production pull backend
docker compose --env-file .env.production up -d --no-deps backend
./deploy/scripts/health-check.sh backend
```

## Rollback

GHCR에는 `latest`와 커밋 SHA 태그(예: `a1b2c3d4e5f6`)가 함께 올라간다. 문제가 생기면:

```bash
./deploy/scripts/rollback.sh backend <이전 커밋 SHA>
```

## DB 백업/복구 (최소, RDS 미사용에 따른 수동 전략)

```bash
./deploy/scripts/backup-db.sh          # ~/arcguard-backups/에 gzip 저장
gunzip -c <파일> | docker compose exec -T mysql mysql -u root -p<DB_ROOT_PASSWORD>
```

cron으로 주기 실행하거나 S3로 옮기는 자동화는 이번 Phase에서 구축하지 않는다(P2).

## HTTPS (Phase 16 적용 완료)

`arcguard.duckdns.org`로 Let's Encrypt 인증서를 발급해 HTTPS를 적용했다. 인증서는
`certbot/certbot` 공식 이미지를 `docker compose run --rm certbot ...`로 1회성 실행해 발급/갱신하며
(webroot 방식 - `/var/www/certbot`을 nginx/certbot 컨테이너가 공유), 상시 기동하는 서비스가 아니다.

### 최초 발급 (완료됨, 재실행 불필요 - 참고용)

```bash
cd ~/arcguard
docker compose --env-file .env.production run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d arcguard.duckdns.org \
  --email <연락용 이메일> \
  --agree-tos --non-interactive --no-eff-email
```

발급된 인증서는 `deploy/nginx/certs/live/arcguard.duckdns.org/`에 저장된다(Git 커밋 금지,
서버에만 존재). `arcguard.conf`가 이 경로를 `ssl_certificate`/`ssl_certificate_key`로 참조한다.

### 자동 갱신

`deploy/scripts/renew-cert.sh`를 systemd timer(`deploy/systemd/arcguard-renew.{service,timer}`)가
매일 새벽 3시(+최대 30분 랜덤 지연)에 실행한다. `certbot renew`는 만료 30일 이내인 인증서만 실제로
갱신하므로 매일 돌려도 안전하고, 실제 갱신이 일어난 경우에만 `nginx -s reload`로 새 인증서를 반영한다.

최초 설치(완료됨, 재설치 시에만 참고):
```bash
sudo cp deploy/systemd/arcguard-renew.service deploy/systemd/arcguard-renew.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now arcguard-renew.timer
```

상태 확인: `systemctl list-timers arcguard-renew.timer`, 로그: `~/arcguard-renew.log`.
dry-run 검증: `docker compose --env-file .env.production run --rm -T certbot renew --dry-run`
(`-T`로 TTY 할당을 끄지 않으면 비대화형 실행 시 멈출 수 있다 - 실제로 겪은 문제).

### 재발급이 필요한 경우(도메인 변경 등)

1. 새 도메인 DNS가 EC2를 가리키는지 먼저 확인
2. 위 "최초 발급" 명령을 새 도메인으로 실행
3. `arcguard.conf`의 `server_name`/`ssl_certificate*` 경로를 새 도메인 기준으로 수정
4. `docker compose exec nginx nginx -t && docker compose exec nginx nginx -s reload`

## Security Group (AWS Console)

현재 inbound: `22`(0.0.0.0/0), `80`(0.0.0.0/0), `443`(0.0.0.0/0).

Phase 16에서 정리 완료:
- `8080` 규칙 삭제됨 (Fairway 시절 잔재, nginx가 유일한 진입점이 된 뒤로는 불필요했음)

남은 검토 항목:
- `22`를 `0.0.0.0/0` 대신 본인 IP 대역으로 제한 (TBD, 사용자 결정 필요)

## Elastic IP (검토)

현재 EC2는 Elastic IP가 아니다 - stop/start할 때마다 Public IP가 바뀐다(이번 Phase에서 실제로
`3.35.22.238`로 바뀐 것을 확인). HTTPS/도메인을 붙여 장기 운영하려면 Elastic IP 할당을 권장한다
(고정 IP 필요 - 새 리소스 생성이라 이번 Phase에서 임의로 만들지 않았다. 사용자 결정 필요, TBD).
