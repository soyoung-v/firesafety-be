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

check_frontend() {
    echo "[health] frontend (nginx -> frontend)"
    curl -fsS -o /dev/null http://localhost/
}

check_backend() {
    # 전용 /actuator/health 엔드포인트가 없어(TBD, Phase 14 발견) 인증 없이 접근 가능한
    # Swagger UI 페이지로 Spring Boot가 실제로 응답하는지 확인한다.
    echo "[health] backend (nginx -> backend, swagger-ui)"
    curl -fsS -o /dev/null http://localhost/swagger-ui.html
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
    local attempt
    for attempt in $(seq 1 10); do
        if "${COMPOSE[@]}" exec -T ai-service python -c \
            "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health')" 2>/dev/null; then
            return 0
        fi
        echo "[health] ai-service not ready yet (attempt $attempt/10), retrying in 2s..."
        sleep 2
    done
    echo "[health] ai-service did not become ready in time" >&2
    return 1
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
