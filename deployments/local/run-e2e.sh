#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
E2E_EMAIL="${E2E_EMAIL:-e2e-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"
E2E_RUN="e2e-$(date -u +%Y%m%d%H%M%S)-$$"
E2E_ROLE="E2E_ROLE_${E2E_RUN}"
E2E_MERCHANT="E2E Merchant ${E2E_RUN}"

psql_e2e() {
  "${COMPOSE[@]}" exec -T postgres psql -X -v ON_ERROR_STOP=1 -U DRAGON -d PAYMENT_GATEWAY "$@"
}

# The E2E email is generated from a safe charset; escaping single quotes keeps the
# literal safe for inline interpolation (psql -v :'var' is unreliable in this image).
sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

cleanup() {
  local email_lit
  email_lit="$(sql_literal "$E2E_EMAIL")"
  psql_e2e -c "DELETE FROM users WHERE email = '$email_lit';" >/dev/null 2>&1 || true
  rm -f /tmp/payment-gateway-e2e-register.json /tmp/payment-gateway-e2e-code
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

REGISTER_STATUS="$(curl --silent --show-error --max-time 10 -o /tmp/payment-gateway-e2e-register.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"E2E\",\"lastname\":\"Runner\",\"email\":\"$E2E_EMAIL\",\"password\":\"E2E-password-123\"}" \
  "$BASE_URL/register")"
[[ "$REGISTER_STATUS" == "201" ]] || { echo "Registration failed with HTTP $REGISTER_STATUS" >&2; cat /tmp/payment-gateway-e2e-register.json >&2; exit 1; }
grep -q '"status":"success"' /tmp/payment-gateway-e2e-register.json || { echo "Registration response was not successful" >&2; exit 1; }

# Registration stores the verification code in PostgreSQL. Read only this run's row;
# the code is intentionally not exposed by the production API response.
psql_e2e -At -c \
  "SELECT verification_code FROM users WHERE email = '$(sql_literal "$E2E_EMAIL")';" >/tmp/payment-gateway-e2e-code
VERIFICATION_CODE="$(tr -d '\r\n' </tmp/payment-gateway-e2e-code)"
if [[ -z "$VERIFICATION_CODE" ]]; then
  echo "Could not read verification code for $E2E_EMAIL" >&2
  exit 1
fi

E2E_EMAIL="$E2E_EMAIL" VERIFICATION_CODE="$VERIFICATION_CODE" \
  hurl --test --variable base_url="$BASE_URL" \
  --variable e2e_email="$E2E_EMAIL" \
  --variable verification_code="$VERIFICATION_CODE" \
  --variable e2e_role="$E2E_ROLE" \
  --variable e2e_merchant="$E2E_MERCHANT" \
  "$ROOT_DIR/deployments/local/e2e.hurl"

echo ""
# Login to get access token for authenticated GraphQL tests
LOGIN_STATUS="$(curl --silent --show-error --max-time 10 -o /tmp/payment-gateway-e2e-login.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"query\":\"mutation Login(\\\$e:String!,\\\$p:String!){ login(email:\\$e,password:\\$p){ status accessToken refreshToken } }\",\"variables\":{\"e\":\"$E2E_EMAIL\",\"p\":\"E2E-password-123\"}}" \
  "$BASE_URL/graphql")"
ACCESS_TOKEN=$(python3 -c "import json; print(json.load(open('/tmp/payment-gateway-e2e-login.json'))['data']['login']['accessToken'])" 2>/dev/null || echo "")

echo ""
echo "=== Running GraphQL comprehensive E2E ==="
hurl --test \
  --variable base_url="$BASE_URL" \
  --variable graphql_url="$BASE_URL/graphql" \
  --variable e2e_email="$E2E_EMAIL" \
  --variable e2e_password="E2E-password-123" \
  --variable access_token="$ACCESS_TOKEN" \
  --variable verification_code="$VERIFICATION_CODE" \
  "$ROOT_DIR/e2e-graphql.hurl"

echo ""
echo "=== Running partial-update E2E (COALESCE tests) ==="
hurl --test \
  --variable base_url="$BASE_URL" \
  --variable graphql_url="$BASE_URL/graphql" \
  --variable e2e_email="$E2E_EMAIL" \
  --variable access_token="$ACCESS_TOKEN" \
  "$ROOT_DIR/e2e-graphql-partial-update.hurl"

echo "Partial-update E2E passed for $E2E_EMAIL"
echo "REST E2E passed for $E2E_EMAIL"
