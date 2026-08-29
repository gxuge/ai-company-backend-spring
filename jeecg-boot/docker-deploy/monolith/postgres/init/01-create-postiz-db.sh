#!/usr/bin/env bash
set -euo pipefail

: "${POSTIZ_DB_NAME:?POSTIZ_DB_NAME is required}"
: "${POSTIZ_DB_USER:?POSTIZ_DB_USER is required}"
: "${POSTIZ_DB_PASSWORD:?POSTIZ_DB_PASSWORD is required}"

psql -v ON_ERROR_STOP=1 \
  --username "${POSTGRES_USER}" \
  --dbname "${POSTGRES_DB}" \
  --set=postiz_db="${POSTIZ_DB_NAME}" \
  --set=postiz_user="${POSTIZ_DB_USER}" \
  --set=postiz_password="${POSTIZ_DB_PASSWORD}" <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN', :'postiz_user')
WHERE NOT EXISTS (
  SELECT 1 FROM pg_roles WHERE rolname = :'postiz_user'
)
\gexec

SELECT format(
  'ALTER ROLE %I WITH LOGIN PASSWORD %L',
  :'postiz_user',
  :'postiz_password'
)
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'postiz_db', :'postiz_user')
WHERE NOT EXISTS (
  SELECT 1 FROM pg_database WHERE datname = :'postiz_db'
)
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', :'postiz_db', :'postiz_user')
\gexec
EOSQL
