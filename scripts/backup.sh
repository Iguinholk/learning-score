#!/usr/bin/env bash
# Backup diário do PostgreSQL (RNF: backup diário às 03h). Agende com:  crontab -e
#   0 3 * * * DB_PASSWORD=... /caminho/scripts/backup.sh
set -euo pipefail
DESTINO="${BACKUP_DIR:-./backups}"; mkdir -p "$DESTINO"
PGPASSWORD="${DB_PASSWORD:?defina DB_PASSWORD}" pg_dump -h "${DB_HOST:-localhost}" -U "${DB_USER:-learningscore}" "${DB_NAME:-learningscore}" \
  | gzip > "$DESTINO/learningscore-$(date +%F).sql.gz"
find "$DESTINO" -name 'learningscore-*.sql.gz' -mtime +14 -delete   # mantém 14 dias
