#!/usr/bin/env bash
# Genera k8s/06-mysql-init-configmap.yaml a partir de los .sql de database/.
#
# Por que existe: ese ConfigMap lleva el esquema, los roles y (opcional) los
# usuarios de ejemplo para que el MySQL de Kubernetes nazca listo. Como
# incluye cuentas con sus contrasenas cifradas, el archivo generado NO se
# sube a git (ver .gitignore); se genera en el momento del despliegue.
#
#   database/schema.sql          estructura de las tablas (sin datos)   [en git]
#   database/roles.sql           ADMIN / RRHH / CANDIDATO               [en git]
#   database/usuarios.local.sql  cuentas de ejemplo (opcional)          [NO en git]
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
SALIDA="$RAIZ/k8s/06-mysql-init-configmap.yaml"

agregar() {  # $1 = nombre de la clave en el ConfigMap, $2 = archivo .sql
  echo "  $1: |" >> "$SALIDA"
  sed 's/^/    /' "$2" >> "$SALIDA"
  echo >> "$SALIDA"
}

cat > "$SALIDA" <<CABECERA
apiVersion: v1
kind: ConfigMap
metadata:
  name: mysql-init
  namespace: dvelasquezp4
data:
CABECERA

agregar 01-schema.sql   "$RAIZ/database/schema.sql"
agregar 02-roles.sql    "$RAIZ/database/roles.sql"
if [ -f "$RAIZ/database/usuarios.local.sql" ]; then
  agregar 03-usuarios.sql "$RAIZ/database/usuarios.local.sql"
else
  echo "Aviso: no hay database/usuarios.local.sql; el MySQL nacera sin usuarios (se pueden crear luego)."
fi
echo "Generado: $SALIDA"
