#!/bin/bash
# SHA 태그로 이전 이미지로 되돌린다(GHCR에 latest 외 commit-sha 태그도 함께 올려두는 이유).
# 사용법(EC2 host, ~/arcguard 안에서):
#   ./deploy/scripts/rollback.sh backend <이전-commit-sha-12자리>
set -euo pipefail
cd "$(dirname "$0")/../.."

service="${1:?usage: rollback.sh <frontend|backend|ai-service> <image-tag>}"
tag="${2:?usage: rollback.sh <frontend|backend|ai-service> <image-tag>}"

case "$service" in
    frontend|backend|ai-service) ;;
    *) echo "unknown service: $service" >&2; exit 1 ;;
esac

export IMAGE_TAG="$tag"
docker compose --env-file .env.production pull "$service"
docker compose --env-file .env.production up -d --no-deps "$service"
./deploy/scripts/health-check.sh "$service"

echo "rolled back $service to $tag"
