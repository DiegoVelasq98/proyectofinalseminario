#!/usr/bin/env bash
# Respaldo diario de Blautech: base de datos MySQL + currículums subidos.
# Uso manual:  ./scripts/backup.sh
# Uso por cron: ver scripts/README-backups.md
set -euo pipefail

PROYECTO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_ROOT="/root/backups/blautech"
RETENCION_DIAS=14
FECHA=$(date +%Y-%m-%d_%H%M)
DESTINO="$BACKUP_ROOT/$FECHA"

cd "$PROYECTO_DIR"
set -a
source .env
set +a

mkdir -p "$DESTINO"

echo "[$(date)] Respaldando base de datos (${DB_NAME:-blautech_bd})..."
docker compose exec -T mysql sh -c "exec mysqldump -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --single-transaction --routines --triggers \"${DB_NAME:-blautech_bd}\"" \
  | gzip > "$DESTINO/blautech_bd.sql.gz"

echo "[$(date)] Respaldando currículums subidos..."
VOLUMEN_CURRICULUMS=$(docker volume ls --format '{{.Name}}' | grep -i curriculums_data | head -1)
if [ -n "$VOLUMEN_CURRICULUMS" ]; then
  docker run --rm \
    -v "$VOLUMEN_CURRICULUMS":/data:ro \
    -v "$DESTINO":/backup \
    alpine sh -c "cd /data && tar czf /backup/curriculums.tar.gz ."
else
  echo "ADVERTENCIA: no se encontró el volumen de currículums, se omitió ese respaldo."
fi

echo "[$(date)] Respaldando variables de entorno (.env)..."
cp "$PROYECTO_DIR/.env" "$DESTINO/.env.backup"

echo "[$(date)] Eliminando respaldos con más de $RETENCION_DIAS días..."
find "$BACKUP_ROOT" -maxdepth 1 -type d -mtime "+$RETENCION_DIAS" -exec rm -rf {} \;

echo "[$(date)] Backup completo en $DESTINO"
du -sh "$DESTINO"
