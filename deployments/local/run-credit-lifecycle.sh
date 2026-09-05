#!/usr/bin/env bash
# Runs the credit-card lifecycle E2E suite (credit-lifecycle.hurl) against the
# local Compose stack. Mirrors run-e2e.sh: registers a throwaway user, reads the
# OTP verification code from PostgreSQL (the API deliberately never returns it),
# then executes the hurl assertions.
#
# Note: cleanup removes the user row only. Card-linked rows
# (card_credit_accounts, card_auth_transactions, card_payments,
# card_billing_statements) for the run's card accumulate in the local DB across
# runs — the hurl assertions are per-card ("== 100000") and >= 1, so they never
# flake, but the tables grow. This matches the behavior of run-e2e.sh.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
E2E_EMAIL="${E2E_EMAIL:-clc-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"

psql_e2e() {
  "${COMPOSE[@]}" exec -T postgres psql -X -v ON_ERROR_STOP=1 -U DRAGON -d PAYMENT_GATEWAY "$@"
}

sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

cleanup() {
  local email_lit
  email_lit="$(sql_literal "$E2E_EMAIL")"
  psql_e2e -c "DELETE FROM users WHERE email = '$email_lit';" >/dev/null 2>&1 || true
  rm -f /tmp/payment-gateway-clc-register.json /tmp/payment-gateway-clc-code
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

REGISTER_STATUS="$(curl --silent --show-error --max-time 10 -o /tmp/payment-gateway-clc-register.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"CLC\",\"lastname\":\"Runner\",\"email\":\"$E2E_EMAIL\",\"password\":\"E2E-password-123\"}" \
  "$BASE_URL/register")"
[[ "$REGISTER_STATUS" == "201" ]] || { echo "Registration failed with HTTP $REGISTER_STATUS" >&2; cat /tmp/payment-gateway-clc-register.json >&2; exit 1; }
grep -q '"status":"success"' /tmp/payment-gateway-clc-register.json || { echo "Registration response was not successful" >&2; exit 1; }

psql_e2e -At -c \
  "SELECT verification_code FROM users WHERE email = '$(sql_literal "$E2E_EMAIL")';" >/tmp/payment-gateway-clc-code
VERIFICATION_CODE="$(tr -d '\r\n' </tmp/payment-gateway-clc-code)"
if [[ -z "$VERIFICATION_CODE" ]]; then
  echo "Could not read verification code for $E2E_EMAIL" >&2
  exit 1
fi

hurl --test --variable base_url="$BASE_URL" \
  --variable e2e_email="$E2E_EMAIL" \
  --variable verification_code="$VERIFICATION_CODE" \
  "$ROOT_DIR/deployments/local/credit-lifecycle.hurl"

echo "Credit lifecycle E2E passed for $E2E_EMAIL"
