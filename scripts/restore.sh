#!/usr/bin/env bash
# Restauración de un respaldo generado por backup.sh.
# Uso: ./scripts/restore.sh /root/backups/blautech/2026-10-20_0300
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Uso: $0 /ruta/al/respaldo (ej. /root/backups/blautech/2026-10-20_0300)"
  exit 1
fi
BACKUP_DIR="$1"
PROYECTO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ ! -f "$BACKUP_DIR/blautech_bd.sql.gz" ]; then
  echo "ERROR: no se encontró blautech_bd.sql.gz en $BACKUP_DIR"
  exit 1
fi

cd "$PROYECTO_DIR"
set -a
source .env
set +a

echo "=========================================================="
echo "ADVERTENCIA: esto REEMPLAZA los datos actuales de ${DB_NAME:-blautech_bd}"
echo "con el respaldo de: $BACKUP_DIR"
echo "=========================================================="
read -r -p "Escribe CONFIRMAR para continuar: " confirmacion
[ "$confirmacion" = "CONFIRMAR" ] || { echo "Cancelado."; exit 1; }

echo "[$(date)] Restaurando base de datos..."
gunzip -c "$BACKUP_DIR/blautech_bd.sql.gz" | docker compose exec -T mysql sh -c "exec mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" \"${DB_NAME:-blautech_bd}\""

if [ -f "$BACKUP_DIR/curriculums.tar.gz" ]; then
  echo "[$(date)] Restaurando currículums..."
  VOLUMEN_CURRICULUMS=$(docker volume ls --format '{{.Name}}' | grep -i curriculums_data | head -1)
  docker run --rm \
    -v "$VOLUMEN_CURRICULUMS":/data \
    -v "$BACKUP_DIR":/backup:ro \
    alpine sh -c "cd /data && tar xzf /backup/curriculums.tar.gz"
fi

echo "[$(date)] Restauración completa. Reiniciando backend para que tome los datos..."
docker compose restart backend

echo "Listo. Verifica el sistema en https://blautech.online"
