#!/bin/bash
# 최소 수동 MySQL 백업. RDS를 쓰지 않으므로(Phase 14 결정) 자동화 인프라 없이 수동 mysqldump로
# 최소한의 복구 수단만 마련한다. cron/S3 자동 백업은 P2로 남겨둔다.
#
# 사용법(EC2 host에서, ~/arcguard 안에서 실행):
#   ./deploy/scripts/backup-db.sh
#
# host shell에서 비밀번호를 직접 읽거나 파싱하지 않는다. mysql 컨테이너는 이미
# docker-compose.yml environment(MYSQL_ROOT_PASSWORD/MYSQL_DATABASE)로 값을 받아 갖고
# 있으므로, exec로 그 컨테이너 내부 환경변수를 그대로 사용한다 - .env.production의 값
# 포맷(공백 포함 등)과 무관하게 안전하다.
set -euo pipefail
cd "$(dirname "$0")/../.."

if [ ! -f .env.production ]; then
    echo ".env.production not found in $(pwd)" >&2
    exit 1
fi

BACKUP_DIR="${BACKUP_DIR:-$HOME/arcguard-backups}"
mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUT_FILE="$BACKUP_DIR/arcguard_db-$TIMESTAMP.sql.gz"

docker compose --env-file .env.production exec -T mysql \
    sh -c 'exec mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --databases "$MYSQL_DATABASE" --routines --triggers --set-gtid-purged=OFF' \
    | gzip > "$OUT_FILE"

echo "backup saved: $OUT_FILE"
echo "restore with:"
echo "  gunzip -c $OUT_FILE | docker compose exec -T mysql mysql -u root -p<DB_ROOT_PASSWORD>"
