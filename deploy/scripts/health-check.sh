#!/bin/bash
# 배포 완료 판단을 "docker compose up 성공"만으로 하지 않는다 - 서비스별로 실제 응답을 확인한다.
# 사용법: ./health-check.sh <frontend|backend|ai-service|mysql|all>
# 실패하면 non-zero exit으로 끝나 CI 배포 job이 실제로 실패 처리된다(실패를 숨기지 않는다).
set -euo pipefail
cd "$(dirname "$0")/../.."

# docker-compose.yml의 mysql.environment가 ${DB_USERNAME} 등 compose-file 변수 치환을 쓰므로
# --env-file 없이 실행하면 "variable is not set" 경고가 뜬다(기존 볼륨 재사용 중엔 무해하지만,
# 모든 docker compose 실행 지점을 일관되게 --env-file .env.production으로 통일한다).
COMPOSE=(docker compose --env-file .env.production)

# N회까지 2초 간격으로 재시도하고, 전부 실패하면 1을 반환한다. "$@"는 매 시도마다 실행할 명령.
_retry() {
    local label="$1"
    shift
    local attempt
    for attempt in $(seq 1 10); do
        if "$@" 2>/dev/null; then
            return 0
        fi
        echo "[health] $label not ready yet (attempt $attempt/10), retrying in 2s..."
        sleep 2
    done
    echo "[health] $label did not become ready in time" >&2
    return 1
}

check_frontend() {
    echo "[health] frontend (nginx -> frontend)"
    curl -fsS -o /dev/null http://localhost/
    # nginx가 실제로 떠 있어야만 의미 있는 integration check라 frontend 배포(=전체 스택이 갖춰진
    # 시점) 쪽에서만 검증한다 - backend 배포 시점엔 nginx가 아직 없을 수 있어 여기서는 하지 않는다.
    echo "[health] nginx -> backend (swagger-ui integration route)"
    curl -fsS -o /dev/null http://localhost/swagger-ui.html
}

check_backend() {
    # nginx를 거치지 않고 backend container 자체가 떴는지만 확인한다(전용 /actuator/health
    # 엔드포인트 없음, TBD Phase 14 발견 - 인증 없이 접근 가능한 Swagger UI로 대체).
    # AI->Backend->Frontend 최초 bootstrap 순서에서는 backend 배포 시점에 nginx가 아직 배포되지
    # 않은 상태일 수 있어(nginx는 frontend repo 소유) nginx 경유 체크를 여기서 강제하면 안 된다
    # - nginx 통합 확인은 check_frontend에서 한다.
    #
    # container Started != Spring Boot ready(DB 연결/Flyway 마이그레이션에 시간이 걸릴 수 있음) -
    # 첫 시도 실패를 즉시 배포 실패로 처리하지 않고 짧게 재시도한다(최대 10회, 2초 간격 = 20초).
    echo "[health] backend (internal, swagger-ui - nginx 없이 backend 자체만 검증)"
    _retry "backend" "${COMPOSE[@]}" exec -T backend wget -q -O /dev/null http://127.0.0.1:8080/swagger-ui.html
}

check_ai_service() {
    echo "[health] ai-service (internal, /health)"
    # localhost는 컨테이너 /etc/hosts에서 127.0.0.1과 ::1 둘 다로 resolve되는데, 이 컨테이너는
    # IPv6가 비활성화돼 있어(net.ipv6.conf.all.disable_ipv6=1) urllib이 127.0.0.1 시도가 실패한
    # 뒤 ::1로 재시도하면 "Errno 99 Cannot assign requested address"로 원인이 가려진다.
    # 127.0.0.1을 명시해 애초에 IPv6 후보 자체를 시도하지 않게 한다.
    #
    # container Started != FastAPI ready(모델 4개 로드 + uvicorn bind에 시간이 걸릴 수 있음) -
    # 첫 시도 실패를 즉시 배포 실패로 처리하지 않고 짧게 재시도한다(최대 10회, 2초 간격 = 20초).
    _retry "ai-service" "${COMPOSE[@]}" exec -T ai-service python -c \
        "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health')"
}

check_mysql() {
    echo "[health] mysql"
    "${COMPOSE[@]}" exec -T mysql mysqladmin ping -h localhost --silent
}

target="${1:-all}"
case "$target" in
    frontend) check_frontend ;;
    backend) check_backend ;;
    ai-service) check_ai_service ;;
    mysql) check_mysql ;;
    all)
        check_mysql
        check_ai_service
        check_backend
        check_frontend
        ;;
    *)
        echo "unknown target: $target (frontend|backend|ai-service|mysql|all)" >&2
        exit 1
        ;;
esac

echo "[health] $target OK"
