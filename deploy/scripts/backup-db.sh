#!/bin/bash
# 최소 수동 MySQL 백업. RDS를 쓰지 않으므로(Phase 14 결정) 자동화 인프라 없이 수동 mysqldump로
# 최소한의 복구 수단만 마련한다. cron/S3 자동 백업은 P2로 남겨둔다.
#
# 사용법(EC2 host에서, ~/arcguard 안에서 실행):
#   ./deploy/scripts/backup-db.sh
#
# 컨테이너 내부의 MYSQL_ROOT_PASSWORD는 컨테이너 최초 생성 시점 값이 그대로 굳어있어(mysql
# 공식 이미지는 데이터 디렉토리가 비어있을 때만 이 값을 반영), .env.production을 나중에 갱신해도
# 컨테이너가 재생성되지 않으면 실제 DB 비밀번호와 어긋날 수 있다(운영 중 실제로 확인된 케이스).
# 그래서 host shell에서 .env.production을 직접 읽되, 전체를 source하지 않고 필요한 키만 grep으로
# 읽는다 - 값에 공백이 섞여도(예: "아크가드 ArcGuard") word-splitting으로 깨지지 않는다.
set -euo pipefail
cd "$(dirname "$0")/../.."

if [ ! -f .env.production ]; then
    echo ".env.production not found in $(pwd)" >&2
    exit 1
fi

DB_ROOT_PASSWORD=$(grep -m1 '^DB_ROOT_PASSWORD=' .env.production | cut -d= -f2-)
if [ -z "$DB_ROOT_PASSWORD" ]; then
    echo "DB_ROOT_PASSWORD not found in .env.production" >&2
    exit 1
fi

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
