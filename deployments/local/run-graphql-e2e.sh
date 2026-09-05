#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
GRAPHQL_URL="$BASE_URL/graphql"
E2E_EMAIL="${E2E_EMAIL:-gql-e2e-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"
E2E_PASSWORD="${E2E_PASSWORD:-E2E-password-123}"

psql_gql() {
  "${COMPOSE[@]}" exec -T postgres psql -X -v ON_ERROR_STOP=1 -U DRAGON -d PAYMENT_GATEWAY "$@"
}

sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

cleanup() {
  local email_lit
  email_lit="$(sql_literal "$E2E_EMAIL")"
  psql_gql -c "DELETE FROM users WHERE email = '$email_lit';" >/dev/null 2>&1 || true
}
trap cleanup EXIT

wait_for_gateway() {
  for _ in $(seq 1 "${E2E_WAIT_ATTEMPTS:-60}"); do
    if curl --silent --fail --max-time 3 "$BASE_URL/health/ready" >/dev/null; then
      return 0
    fi
    sleep 2
  done
  echo "Gateway did not become ready: $BASE_URL/health/ready" >&2
  "${COMPOSE[@]}" ps >&2 || true
  exit 1
}

wait_for_gateway

# Register through gRPC stack (same DB row the legacy run-e2e.sh creates).
# We hit gRPC via the auth service, but for a self-contained GraphQL test we
# register via the GraphQL endpoint instead. The OTP comes from the email
# worker — for an E2E run, the simplest path is to write the code directly
# into the row, mirroring run-e2e.sh.
REGISTER_PAYLOAD=$(cat <<JSON
{"query":"mutation Reg(\$f:String!,\$l:String!,\$e:String!,\$p:String!){ register(firstname:\$f,lastname:\$l,email:\$e,password:\$p){ status message } }","variables":{"f":"GraphQL","l":"E2E","e":"$E2E_EMAIL","p":"$E2E_PASSWORD"}}
JSON
)
REGISTER_RESPONSE=$(curl --silent --show-error --max-time 10 \
  -H 'Content-Type: application/json' \
  -d "$REGISTER_PAYLOAD" \
  "$GRAPHQL_URL")
echo "$REGISTER_RESPONSE" | grep -q '"status":"success"' || {
  echo "GraphQL registration failed" >&2
  echo "$REGISTER_RESPONSE" >&2
  exit 1
}

psql_gql -At -c \
  "SELECT verification_code FROM users WHERE email = '$(sql_literal "$E2E_EMAIL")';" \
  >/tmp/payment-gateway-gql-e2e-code
VERIFICATION_CODE="$(tr -d '\r\n' </tmp/payment-gateway-gql-e2e-code)"
if [[ -z "$VERIFICATION_CODE" ]]; then
  echo "Could not read verification code for $E2E_EMAIL" >&2
  exit 1
fi

E2E_EMAIL="$E2E_EMAIL" E2E_PASSWORD="$E2E_PASSWORD" VERIFICATION_CODE="$VERIFICATION_CODE" \
  hurl --test \
  --variable base_url="$BASE_URL" \
  --variable graphql_url="$GRAPHQL_URL" \
  --variable e2e_email="$E2E_EMAIL" \
  --variable e2e_password="$E2E_PASSWORD" \
  --variable verification_code="$VERIFICATION_CODE" \
  "$ROOT_DIR/e2e-graphql.hurl"

echo "GraphQL E2E passed for $E2E_EMAIL"
