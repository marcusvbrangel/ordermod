#!/usr/bin/env bash
set -euo pipefail

application_database="$PGDATABASE"

psql --dbname=postgres \
  --set=ON_ERROR_STOP=1 \
  --set=monitor_password="$POSTGRES_MONITOR_PASSWORD" <<'SQL'
SELECT format(
    'CREATE ROLE postgres_monitor LOGIN PASSWORD %L',
    :'monitor_password'
)
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_catalog.pg_roles
    WHERE rolname = 'postgres_monitor'
) \gexec

SELECT format(
    'ALTER ROLE postgres_monitor PASSWORD %L',
    :'monitor_password'
) \gexec

GRANT pg_monitor TO postgres_monitor;
ALTER ROLE postgres_monitor SET pg_stat_statements.track = 'none';
SQL

# The receiver opens its administrative connection against `postgres`, while the
# application runs against `ordermod`. The extension view must exist in both.
for database in postgres "$application_database"; do
  psql --dbname="$database" --set=ON_ERROR_STOP=1 \
    --command="CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"
done
