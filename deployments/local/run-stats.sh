#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
STATS_EMAIL="${STATS_EMAIL:-stats-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"
STATS_RUN="stats-$(date -u +%Y%m%d%H%M%S)-$$"
STATS_MERCHANT="Stats Merchant ${STATS_RUN}"

psql_stats() {
  "${COMPOSE[@]}" exec -T postgres psql -X -v ON_ERROR_STOP=1 -U DRAGON -d PAYMENT_GATEWAY "$@"
}

sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

cleanup() {
  local email_lit
  email_lit="$(sql_literal "$STATS_EMAIL")"
  psql_stats -c "DELETE FROM users WHERE email = '$email_lit';" >/dev/null 2>&1 || true
  rm -f /tmp/payment-gateway-stats-register.json /tmp/payment-gateway-stats-code
}
trap cleanup EXIT

wait_for_gateway() {
  for _ in $(seq 1 "${STATS_WAIT_ATTEMPTS:-60}"); do
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

REGISTER_STATUS="$(curl --silent --show-error --max-time 10 -o /tmp/payment-gateway-stats-register.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"Stats\",\"lastname\":\"Runner\",\"email\":\"$STATS_EMAIL\",\"password\":\"E2E-password-123\"}" \
  "$BASE_URL/register")"
[[ "$REGISTER_STATUS" == "201" ]] || { echo "Registration failed with HTTP $REGISTER_STATUS" >&2; cat /tmp/payment-gateway-stats-register.json >&2; exit 1; }
grep -q '"status":"success"' /tmp/payment-gateway-stats-register.json || { echo "Registration response was not successful" >&2; exit 1; }

psql_stats -At -c \
  "SELECT verification_code FROM users WHERE email = '$(sql_literal "$STATS_EMAIL")';" >/tmp/payment-gateway-stats-code
VERIFICATION_CODE="$(tr -d '\r\n' </tmp/payment-gateway-stats-code)"
if [[ -z "$VERIFICATION_CODE" ]]; then
  echo "Could not read verification code for $STATS_EMAIL" >&2
  exit 1
fi

STATS_EMAIL="$STATS_EMAIL" VERIFICATION_CODE="$VERIFICATION_CODE" \
  hurl --test --variable base_url="$BASE_URL" \
  --variable stats_email="$STATS_EMAIL" \
  --variable verification_code="$VERIFICATION_CODE" \
  --variable stats_merchant="$STATS_MERCHANT" \
  "$ROOT_DIR/deployments/local/stats.hurl"

echo "Stats E2E passed for $STATS_EMAIL"
