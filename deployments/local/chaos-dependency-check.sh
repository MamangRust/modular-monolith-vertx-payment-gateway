#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
AUTH_HEADER="${AUTH_HEADER:?Set AUTH_HEADER='Authorization: Bearer <admin-token>'}"
POLICY_FILE="${CHAOS_CONFIG_PATH:-$ROOT_DIR/chaos.yaml}"
BACKUP="$(mktemp)"
RUN_MARKER="chaos-check-$(date -u +%Y%m%d%H%M%S)-$$"
restore_chaos_config() {
  cp "$BACKUP" "$POLICY_FILE"
  rm -f "$BACKUP"
  # ChaosManager has an authenticated reload endpoint; SIGHUP is not a Java watcher signal.
  curl --silent --show-error --max-time 10 \
    -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
    -X POST "$BASE_URL/api/chaos/policies/reload" >/dev/null 2>&1 || true
}
trap restore_chaos_config EXIT
cp "$POLICY_FILE" "$BACKUP"

curl_json() {
  curl --silent --show-error --fail --max-time "${CHAOS_TIMEOUT_SECONDS:-15}" \
    -H "$AUTH_HEADER" -H 'Content-Type: application/json' "$@"
}

wait_for_gateway() {
  for _ in $(seq 1 "${CHAOS_WAIT_ATTEMPTS:-30}"); do
    curl --silent --fail --max-time 3 "$BASE_URL/health/ready" >/dev/null && return 0
    sleep 2
  done
  echo "Gateway is not ready" >&2
  exit 1
}

set_policies() {
  local enabled_name="$1"
  python3 - "$POLICY_FILE" "$enabled_name" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
enabled_name = sys.argv[2]
text = path.read_text()
blocks = text.split('\n  - name:')
output = [blocks[0]]
for raw in blocks[1:]:
    block = '  - name:' + raw
    first_line = raw.splitlines()[0]
    name = first_line.strip().strip('"')
    block = block.replace('enabled: true', 'enabled: false')
    if name == enabled_name:
        block = block.replace('enabled: false', 'enabled: true', 1)
        block = block.replace('errorChance: 0.15', 'errorChance: 1.0')
        block = block.replace('errorChance: 0.2', 'errorChance: 1.0')
        block = block.replace('errorChance: 0.25', 'errorChance: 1.0')
    output.append(block)
path.write_text('\n'.join(output))
PY
  curl_json -X POST "$BASE_URL/api/chaos/policies/reload" >/dev/null
  local policy_json
  policy_json="$(curl_json "$BASE_URL/api/chaos/policies")"
  grep -q "\"name\": \"$enabled_name\"" <<<"$policy_json"
  grep -q '"enabled": true' <<<"$policy_json"
}

request_status() {
  local output="$1"
  shift
  curl --silent --show-error --max-time 10 -o "$output" -w '%{http_code}' "$@"
}

wait_for_gateway

# The authenticated control-plane endpoint is the source of truth for the running
# process. A 404/401 here means the stack is not enabled or the supplied token is not
# an admin token; the caller must fix the running deployment, not this shell env.
POLICY_STATUS="$(curl --silent --show-error --max-time 10 -o /tmp/chaos-policies.json -w '%{http_code}' \
  -H "$AUTH_HEADER" "$BASE_URL/api/chaos/policies")"
[[ "$POLICY_STATUS" == "200" ]] || {
  echo "Chaos control plane unavailable (HTTP $POLICY_STATUS); start services with CHAOS_ENABLED=true and provide an admin token" >&2
  cat /tmp/chaos-policies.json >&2 || true
  exit 2
}

# SQL: auth login must traverse the auth repository and fail with the injected SQL error.
set_policies "sql-users-deadlock"
SQL_STATUS="$(request_status /tmp/chaos-sql.json \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$RUN_MARKER@example.test\",\"password\":\"bad\"}" \
  "$BASE_URL/login")"
[[ "$SQL_STATUS" != 2* ]] || { echo "SQL chaos did not fail login: HTTP $SQL_STATUS" >&2; exit 1; }
"${COMPOSE[@]}" logs --no-color --since "${CHAOS_LOG_SINCE:-30s}" auth 2>/dev/null \
  | grep -q 'Policy: sql-users-deadlock' || {
    echo "SQL request failed, but the injected SQL policy was not observed in auth logs" >&2
    cat /tmp/chaos-sql.json >&2
    exit 1
  }

# gRPC: disable SQL and force the outbound AuthService/LoginUser call to fail.
set_policies "grpc-auth-login-unavailable"
GRPC_STATUS="$(request_status /tmp/chaos-grpc.json \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$RUN_MARKER@example.test\",\"passwordnjected SQL policy was not observed in auth logs" >&2
    cat /tmp/chaos-sql.json >&2
    exit 1
  }

# gRPC: disable SQL and force the outbound AuthService/LoginUser call to fail.
set_policies "grpc-auth-login-unavailable"
GRPC_STATUS="$(request_status /tmp/chaos-grpc.json \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$RUN_MARKER@example.test\",\"password\":\"bad\"}" \
  "$BASE_URL/login")"
[[ "$GRPC_STATUS" != 2* ]] || { echo "gRPC chaos did not fail login: HTTP $GRPC_STATUS" >&2; exit 1; }
"${COMPOSE[@]}" logs --no-color --since "${CHAOS_LOG_SINCE:-30s}" apigateway 2>/dev/null \
  | grep -q 'Policy: grpc-auth-login-unavailable' || {
    echo "gRPC request failed, but the injected gRPC policy was not observed in apigateway logs" >&2
    cat /tmp/chaos-grpc.json >&2
    exit 1
  }

# Kafka: registration is the real producer path. A dropped message is intentionally
# fire-and-forget, so success alone cannot prove delivery; verify the producer log too.
set_policies "kafka-auth-register-drop"
KAFKA_EMAIL="$RUN_MARKER@example.test"
KAFKA_STATUS="$(request_status /tmp/chaos-kafka.json \
  -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"Chaos\",\"lastname\":\"Check\",\"email\":\"$KAFKA_EMAIL\",\"password\":\"E2E-password-123\"}" \
  "$BASE_URL/register")"
[[ "$KAFKA_STATUS" == "201" ]] || { echo "Kafka exercise registration failed: HTTP $KAFKA_STATUS" >&2; cat /tmp/chaos-kafka.json >&2; exit 1; }
"${COMPOSE[@]}" logs --no-color --since "${CHAOS_LOG_SINCE:-30s}" auth 2>/dev/null \
  | grep -q 'Dropping Kafka message' || {
    echo "Registration succeeded, but Kafka drop was not observable in auth logs" >&2
    exit 1
  }

# Restore the original file through EXIT trap and report only what was proved.
echo "Chaos dependency check passed: SQL fault, gRPC fault, and Kafka drop were exercised."
