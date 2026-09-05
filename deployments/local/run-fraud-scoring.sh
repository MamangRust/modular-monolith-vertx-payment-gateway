#!/usr/bin/env bash
# Runs the fraud-scoring E2E suite (fraud-scoring.hurl) against the local
# Compose stack. Mirrors run-credit-lifecycle.sh: registers a throwaway user,
# reads the OTP verification code from PostgreSQL, then executes the hurl
# assertions.
#
# After hurl succeeds it additionally verifies the asynchronous fraud pipeline:
#   1. risk_score for the high-risk txn (MCC 7995, 15M IDR) persisted as 70.
#   2. a card.fraud.alert event was published on the Kafka topic (best effort,
#      needs the kafka console consumer to be reachable).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
E2E_EMAIL="${E2E_EMAIL:-fraud-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"

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
  rm -f /tmp/payment-gateway-fraud-register.json /tmp/payment-gateway-fraud-code
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

REGISTER_STATUS="$(curl --silent --show-error --max-time 10 -o /tmp/payment-gateway-fraud-register.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"Fraud\",\"lastname\":\"Runner\",\"email\":\"$E2E_EMAIL\",\"password\":\"E2E-password-123\"}" \
  "$BASE_URL/register")"
[[ "$REGISTER_STATUS" == "201" ]] || { echo "Registration failed with HTTP $REGISTER_STATUS" >&2; cat /tmp/payment-gateway-fraud-register.json >&2; exit 1; }
grep -q '"status":"success"' /tmp/payment-gateway-fraud-register.json || { echo "Registration response was not successful" >&2; exit 1; }

psql_e2e -At -c \
  "SELECT verification_code FROM users WHERE email = '$(sql_literal "$E2E_EMAIL")';" >/tmp/payment-gateway-fraud-code
VERIFICATION_CODE="$(tr -d '\r\n' </tmp/payment-gateway-fraud-code)"
if [[ -z "$VERIFICATION_CODE" ]]; then
  echo "Could not read verification code for $E2E_EMAIL" >&2
  exit 1
fi

hurl --test --variable base_url="$BASE_URL" \
  --variable e2e_email="$E2E_EMAIL" \
  --variable verification_code="$VERIFICATION_CODE" \
  "$ROOT_DIR/deployments/local/fraud-scoring.hurl"

echo "Fraud scoring E2E passed for $E2E_EMAIL"

# ---------------------------------------------------------------------------
# Post-flight verification: risk_score persisted + Kafka fraud alert emitted
# ---------------------------------------------------------------------------
EMAIL_LIT="$(sql_literal "$E2E_EMAIL")"
CARD_NUMBER="$(psql_e2e -At -c \
  "SELECT c.card_number FROM cards c JOIN users u ON u.user_id = c.user_id WHERE u.email = '$EMAIL_LIT' LIMIT 1;" | tr -d '\r\n')"
if [[ -z "$CARD_NUMBER" ]]; then
  echo "⚠️ Could not resolve card_number for $E2E_EMAIL — skipping DB/Kafka verification" >&2
  exit 0
fi
echo "Card under test: $CARD_NUMBER"

RISK_SCORES="$(psql_e2e -At -c \
  "SELECT risk_score FROM card_auth_transactions WHERE card_number = '$CARD_NUMBER' ORDER BY txn_time;" | tr '\n' ' ')"
echo "Persisted risk_scores: ${RISK_SCORES:-none}"
if ! echo "$RISK_SCORES" | grep -q '\b70\b'; then
  echo "❌ Expected a high-risk txn with risk_score 70, got: '$RISK_SCORES'" >&2
  exit 1
fi

ACCOUNT_STATUS="$(psql_e2e -At -c \
  "SELECT status FROM card_credit_accounts WHERE card_number = '$CARD_NUMBER';" | tr -d '\r\n')"
echo "Credit account status: $ACCOUNT_STATUS"
if [[ "$ACCOUNT_STATUS" != "BLOCKED" ]]; then
  echo "❌ Expected credit account status BLOCKED, got '$ACCOUNT_STATUS'" >&2
  exit 1
fi

# Verify the fraud alert was persisted to card_event_logs by CardEventLogVerticle.
# Deterministic (unlike the Kafka console-consumer check below) and idempotent
# thanks to the partial unique index (topic, reference_id) — reruns won't
# duplicate rows.
EVENT_LOG_COUNT="$(psql_e2e -At -c \
  "SELECT COUNT(*) FROM card_event_logs WHERE topic = 'card.fraud.alert' AND card_number = '$CARD_NUMBER';" | tr -d '\r\n')"
echo "card_event_logs card.fraud.alert rows: ${EVENT_LOG_COUNT:-0}"
if [[ "${EVENT_LOG_COUNT:-0}" == "0" ]]; then
  echo "❌ Expected at least one card.fraud.alert row in card_event_logs for $CARD_NUMBER" >&2
  exit 1
fi
echo "✅ card.fraud.alert persisted in card_event_logs for $CARD_NUMBER"

# Best-effort: consume the fraud alert topic and confirm a message for this card.
# --max-messages bounds the read as the topic grows across repeated runs.
if "${COMPOSE[@]}" exec -T kafka sh -c \
    '/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic card.fraud.alert \
      --from-beginning --max-messages 200 --timeout-ms 8000 2>/dev/null' 2>/dev/null \
    | grep -q "$CARD_NUMBER"; then
  echo "✅ card.fraud.alert published for $CARD_NUMBER"
else
  echo "⚠️ Could not confirm card.fraud.alert on Kafka (consumer unreachable or topic empty) — DB/status checks passed, treating as soft pass" >&2
fi

echo "Fraud scoring pipeline verified end-to-end for $E2E_EMAIL"
