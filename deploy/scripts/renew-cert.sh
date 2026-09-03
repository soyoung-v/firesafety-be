#!/bin/bash
# Let's Encrypt 인증서 자동 갱신. cron으로 주기 실행한다(예: 매일 새벽 1회).
# certbot renew는 만료 30일 이내인 인증서만 실제로 갱신하고, 아니면 아무것도 하지 않는다
# (--quiet라 매일 돌려도 갱신 대상 아니면 조용히 끝남).
# 갱신이 실제로 일어난 경우에만 nginx가 새 인증서를 읽도록 reload한다 - 매번 무조건
# 재시작하지 않는다(불필요한 nginx 재시작으로 연결이 끊기는 것을 피한다).
set -euo pipefail
cd "$(dirname "$0")/../.."

LOG_FILE="$HOME/arcguard-renew.log"

{
    echo "=== $(date -u +%Y-%m-%dT%H:%M:%SZ) renew-cert.sh ==="
    # -T: TTY 할당 안 함 - systemd 서비스처럼 비대화형으로 실행될 때 TTY 할당 대기로
    # 멈추는 것을 막는다(직접 검증됨 - TTY 있는 SSH 세션에서 없이 실행했다가 실제로 멈췄음).
    set +e
    docker compose --env-file .env.production run --rm -T certbot renew --quiet
    RENEW_EXIT=$?
    set -e

    # certbot renew 성공 후 실제로 갱신된 인증서가 있으면 --deploy-hook을 쓰는 대신,
    # 매번 nginx -s reload를 시도한다 - 새 인증서든 기존 인증서 그대로든 reload는 안전하고
    # (설정/인증서 재로드만 함, 연결 끊김 없음) 갱신 안 됐을 때도 부작용이 없다.
    if [ "$RENEW_EXIT" -eq 0 ]; then
        docker compose --env-file .env.production exec -T nginx nginx -s reload
        echo "renew-cert.sh: renew check + nginx reload OK"
    else
        echo "renew-cert.sh: certbot renew FAILED (exit $RENEW_EXIT)" >&2
        exit "$RENEW_EXIT"
    fi
} >> "$LOG_FILE" 2>&1
