# =============================================================================
# PAYMENT GATEWAY — CREDIT CARD LIFECYCLE E2E (standalone)
# Flow: limit → authorize (hold) → reverse (void) → capture (billing settlement)
#       → billing → payment → payment history → rewards
#
# Mapping note: this gateway has no dedicated "capture" RPC. Per the system
# design, an approved authorization immediately reserves credit, and the
# billing cycle materializes those holds into a statement (settlement =
# capture). This file exercises the real settlement path:
#   authorize (hold) → trigger billing (= capture/settle) → statement
#   → payment (pay down statement) → payment history → void → rewards
#
# Runner: deployments/local/run-credit-lifecycle.sh
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
# 3. CARD — create a credit card (needed for the credit lifecycle)
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

# ---------------------------------------------------------------------------
# 4. MERCHANT — authorize needs a merchant id
# ---------------------------------------------------------------------------
POST {{base_url}}/api/merchants
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "user_id": {{user_id}},
  "name": "CLC Merchant {{e2e_email}}"
}

HTTP 201
[Captures]
merchant_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"

# ---------------------------------------------------------------------------
# 5. LIMIT — configure credit line (5M IDR, cycle day 5)
# ---------------------------------------------------------------------------
POST {{base_url}}/api/v1/cards/{{card_number}}/limit
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "credit_limit": 5000000,
  "billing_cycle_day": 5,
  "annual_rate_bps": 1800
}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.credit_limit" == "5000000"
jsonpath "$.data.card_number" == "{{card_number}}"

GET {{base_url}}/api/v1/cards/{{card_number}}/limit
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.credit_limit" == "5000000"
jsonpath "$.data.available_credit" == "5000000"
jsonpath "$.data.used_credit" == "0"

# ---------------------------------------------------------------------------
# 6. AUTHORIZE — hold 1 & hold 2 against the credit line
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
  "idempotency_key": "clc-auth-1-{{e2e_email}}"
}

HTTP 200
[Captures]
txn_id_1: jsonpath "$.data.txn_id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.approval_status" == "APPROVED"
jsonpath "$.data.txn_id" exists

POST {{base_url}}/api/v1/cards/authorize
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "merchant_id": {{merchant_id}},
  "amount": 50000,
  "currency": "IDR",
  "pos_entry_mode": "tap",
  "mcc": "5812",
  "idempotency_key": "clc-auth-2-{{e2e_email}}"
}

HTTP 200
[Captures]
txn_id_2: jsonpath "$.data.txn_id"
[Asserts]
jsonpath "$.data.approval_status" == "APPROVED"
jsonpath "$.data.txn_id" exists

# Both holds must be visible in used credit
GET {{base_url}}/api/v1/cards/{{card_number}}/limit
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.used_credit" == "150000"

# ---------------------------------------------------------------------------
# 7. VOID (reverse) — cancel the second hold BEFORE settlement
# ---------------------------------------------------------------------------
POST {{base_url}}/api/v1/cards/reverse
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "txn_id": "{{txn_id_2}}",
  "card_number": "{{card_number}}",
  "amount": 50000,
  "idempotency_key": "clc-rev-2-{{e2e_email}}"
}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.reversed" == true

# Only the 100K hold remains
GET {{base_url}}/api/v1/cards/{{card_number}}/limit
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.used_credit" == "100000"

# ---------------------------------------------------------------------------
# 8. CAPTURE / SETTLEMENT — billing cycle materializes the hold into a statement
# ---------------------------------------------------------------------------
POST {{base_url}}/api/v1/cards/billing/trigger
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "billing_cycle_day": 5
}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.statements_generated" >= 1

# ---------------------------------------------------------------------------
# 9. BILLING — read the generated statement
# ---------------------------------------------------------------------------
GET {{base_url}}/api/v1/cards/{{card_number}}/statement
Authorization: Bearer {{access_token}}

HTTP 200
[Captures]
statement_id: jsonpath "$.data.statement_id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.statement_id" exists
jsonpath "$.data.purchases" == "100000"
jsonpath "$.data.payment_status" == "UNPAID"
jsonpath "$.data.closing_balance" exists

GET {{base_url}}/api/v1/cards/{{card_number}}/statements
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.total" >= 1

# ---------------------------------------------------------------------------
# 10. PAYMENT — settle the statement principal (releases the held credit).
#     Amount is a hard-coded number (not a captured protobuf string) because the
#     gateway's getJsonLong only accepts a JSON Number.
# ---------------------------------------------------------------------------
POST {{base_url}}/api/v1/cards/payment
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "reference_id": "clc-pay-{{e2e_email}}",
  "card_number": "{{card_number}}",
  "amount": 100000,
  "payment_channel": "bank_transfer",
  "statement_id": {{statement_id}}
}

HTTP 200
[Captures]
payment_id: jsonpath "$.data.payment_id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.reference_id" == "clc-pay-{{e2e_email}}"
jsonpath "$.data.payment_id" exists

# ---------------------------------------------------------------------------
# 11. PAYMENT HISTORY — payment must be listed
# ---------------------------------------------------------------------------
GET {{base_url}}/api/v1/cards/{{card_number}}/payments
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.total" >= 1
jsonpath "$.data[0].payment_id" exists

# ---------------------------------------------------------------------------
# 12. REWARDS — points/status endpoint is reachable
# ---------------------------------------------------------------------------
GET {{base_url}}/api/v1/cards/{{card_number}}/rewards
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ---------------------------------------------------------------------------
# 13. POST-PAYMENT — credit released back to the account
# ---------------------------------------------------------------------------
GET {{base_url}}/api/v1/cards/{{card_number}}/limit
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.used_credit" == "0"
