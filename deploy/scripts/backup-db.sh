#!/bin/bash
# 최소 수동 MySQL 백업. RDS를 쓰지 않으므로(Phase 14 결정) 자동화 인프라 없이 수동 mysqldump로
# 최소한의 복구 수단만 마련한다. cron/S3 자동 백업은 P2로 남겨둔다.
#
# 사용법(EC2 host에서, ~/arcguard 안에서 실행):
#   ./deploy/scripts/backup-db.sh
#
# 비밀번호를 스크립트에 하드코딩하지 않는다 - .env.production에서 읽는다.
set -euo pipefail
cd "$(dirname "$0")/../.."

if [ ! -f .env.production ]; then
    echo ".env.production not found in $(pwd)" >&2
    exit 1
fi

# shellcheck disable=SC1091
set -a; source .env.production; set +a

BACKUP_DIR="${BACKUP_DIR:-$HOME/arcguard-backups}"
mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUT_FILE="$BACKUP_DIR/arcguard_db-$TIMESTAMP.sql.gz"

docker compose exec -T mysql \
    mysqldump -u root -p"$DB_ROOT_PASSWORD" --databases arcguard_db --routines --triggers --set-gtid-purged=OFF \
    | gzip > "$OUT_FILE"

echo "backup saved: $OUT_FILE"
echo "restore with:"
echo "  gunzip -c $OUT_FILE | docker compose exec -T mysql mysql -u root -p<DB_ROOT_PASSWORD>"
