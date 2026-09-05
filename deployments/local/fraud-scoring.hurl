# =============================================================================
# PAYMENT GATEWAY — FRAUD SCORING E2E (standalone)
# Flow: auth → create credit card → merchant → set limit → authorize LOW risk
#       → authorize HIGH risk → verify card BLOCKED by FraudScoringConsumer
#
# How it works:
#   CardAuthorizationServiceImpl publishes every approved authorization to the
#   Kafka topic "card.txn.created". FraudScoringConsumerVerticle (a worker
#   verticle inside the card service) consumes that topic, computes a risk
#   score via computeRiskScore(amount, mcc) and:
#     - score >= 70 → updates the credit account status to BLOCKED and
#                     publishes a "card.fraud.alert" Kafka event.
#     - score 30-69 → flags the txn for review (publishes card.fraud.alert).
#   The high-risk transaction below (amount 15M IDR + gambling MCC 7995) scores
#   30 + 40 = 70, which must flip the card's credit account to BLOCKED.
#
# Runner: deployments/local/run-fraud-scoring.sh
# =============================================================================

# ---------------------------------------------------------------------------
# 1. HEALTH
# ---------------------------------------------------------------------------
GET {{base_url}}/health/ready
HTTP 200
[Asserts]
jsonpath "$.status" == "ready"

# ---------------------------------------------------------------------------
# 2. AUTH — verify, login, profile
# ---------------------------------------------------------------------------
POST {{base_url}}/verify-code
Content-Type: application/json

{
  "code": "{{verification_code}}"
}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/login
Content-Type: application/json

{
  "email": "{{e2e_email}}",
  "password": "E2E-password-123"
}

HTTP 200
[Captures]
access_token: jsonpath "$.data.access_token"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.access_token" exists

GET {{base_url}}/me
Authorization: Bearer {{access_token}}

HTTP 200
[Captures]
user_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.email" == "{{e2e_email}}"

# ---------------------------------------------------------------------------
# 3. CARD + MERCHANT + LIMIT — setup a credit card with a 100M IDR line
# ---------------------------------------------------------------------------
POST {{base_url}}/api/card/create
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "user_id": {{user_id}},
  "card_type": "credit",
  "expiry_date": "2035-12-31T00:00:00+07:00",
  "cvv": "123",
  "card_provider": "VISA"
}

HTTP 201
[Captures]
card_number: jsonpath "$.data.card_number"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.card_number" exists

POST {{base_url}}/api/merchants
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "user_id": {{user_id}},
  "name": "Fraud Merchant {{e2e_email}}"
}

HTTP 201
[Captures]
merchant_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/v1/cards/{{card_number}}/limit
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "credit_limit": 100000000,
  "billing_cycle_day": 5,
  "annual_rate_bps": 1800
}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.credit_limit" == "100000000"

# ---------------------------------------------------------------------------
# 4. AUTHORIZE LOW RISK — small amount, normal MCC (score = 0)
# ---------------------------------------------------------------------------
POST {{base_url}}/api/v1/cards/authorize
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "merchant_id": {{merchant_id}},
  "amount": 100000,
  "currency": "IDR",
  "pos_entry_mode": "chip",
  "mcc": "5411",
  "idempotency_key": "fraud-low-{{e2e_email}}"
}

HTTP 200
[Captures]
low_txn_id: jsonpath "$.data.txn_id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.approval_status" == "APPROVED"
jsonpath "$.data.txn_id" exists

# ---------------------------------------------------------------------------
# 5. AUTHORIZE HIGH RISK — 15M IDR at gambling MCC 7995 (score = 30 + 40 = 70)
#    Approved first; FraudScoringConsumer then blocks the card asynchronously.
# ---------------------------------------------------------------------------
POST {{base_url}}/api/v1/cards/authorize
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "merchant_id": {{merchant_id}},
  "amount": 15000000,
  "currency": "IDR",
  "pos_entry_mode": "chip",
  "mcc": "7995",
  "idempotency_key": "fraud-high-{{e2e_email}}"
}

HTTP 200
[Captures]
high_txn_id: jsonpath "$.data.txn_id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.approval_status" == "APPROVED"
jsonpath "$.data.txn_id" exists

# ---------------------------------------------------------------------------
# 6. VERIFY BLOCK — FraudScoringConsumerVerticle must flip the credit account
#    to BLOCKED. Wait a few seconds for the async Kafka round-trip.
# ---------------------------------------------------------------------------
GET {{base_url}}/api/v1/cards/{{card_number}}/limit
Authorization: Bearer {{access_token}}
[Options]
delay: 4000

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.card_number" == "{{card_number}}"
jsonpath "$.data.status" == "BLOCKED"
