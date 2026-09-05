# =============================================================================
# PAYMENT GATEWAY — FULL CROSS-MODULE E2E
# Covers: health, auth, user, role, card, saldo, merchant, merchant-documents,
#         topup, transfer, withdraw, transaction (API-key), credit lifecycle
# =============================================================================

# ---------------------------------------------------------------------------
# 1. HEALTH
# ---------------------------------------------------------------------------
GET {{base_url}}/health
HTTP 200
[Asserts]
jsonpath "$.status" == "UP"

GET {{base_url}}/health/live
HTTP 200
[Asserts]
jsonpath "$.status" == "alive"

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
refresh_token: jsonpath "$.data.refresh_token"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.access_token" exists
jsonpath "$.data.refresh_token" exists

GET {{base_url}}/me
Authorization: Bearer {{access_token}}

HTTP 200
[Captures]
user_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.email" == "{{e2e_email}}"

# ---------------------------------------------------------------------------
# 3. USER — read + update profile
# ---------------------------------------------------------------------------
GET {{base_url}}/users/{{user_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.id" == {{user_id}}

POST {{base_url}}/users/update/{{user_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "firstname": "E2E-Updated",
  "lastname": "Runner",
  "email": "{{e2e_email}}",
  "password": ""
}

HTTP 200
[Asserts]
jsonpath "$.data.firstname" == "E2E-Updated"

# RBAC negative: admin-only listing must be rejected for a plain ROLE_USER token
GET {{base_url}}/users/active
Authorization: Bearer {{access_token}}

HTTP 403

# ---------------------------------------------------------------------------
# 4. ROLE — create + read
# ---------------------------------------------------------------------------
POST {{base_url}}/roles
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "name": "{{e2e_role}}"
}

HTTP 201
[Captures]
role_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.data.name" == "{{e2e_role}}"

GET {{base_url}}/roles/{{role_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.id" == {{role_id}}

# ---------------------------------------------------------------------------
# 5. CARD — create (card 1 = sender), read
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
card_id: jsonpath "$.data.id"
card_number: jsonpath "$.data.card_number"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.card_number" exists

GET {{base_url}}/api/card/{{card_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.id" == {{card_id}}

# ---------------------------------------------------------------------------
# 6. SALDO — create + read for the sender card
# ---------------------------------------------------------------------------
POST {{base_url}}/api/saldos
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "balance": 1000000
}

HTTP 201
[Captures]
saldo_id: jsonpath "$.data.saldo_id"
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/saldos/{{saldo_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.saldo_id" == {{saldo_id}}

# ---------------------------------------------------------------------------
# 7. MERCHANT — create + read
# ---------------------------------------------------------------------------
POST {{base_url}}/api/merchants
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "user_id": {{user_id}},
  "name": "{{e2e_merchant}}"
}

HTTP 201
[Captures]
merchant_id: jsonpath "$.data.id"
api_key: jsonpath "$.data.api_key"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.api_key" exists

GET {{base_url}}/api/merchants/{{merchant_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.id" == {{merchant_id}}

# ---------------------------------------------------------------------------
# 8. MERCHANT DOCUMENT — create + read
# ---------------------------------------------------------------------------
POST {{base_url}}/api/merchant-documents
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "merchant_id": {{merchant_id}},
  "document_type": "BUSINESS_LICENSE",
  "document_path": "/documents/e2e-license.pdf"
}

HTTP 201
[Captures]
document_id: jsonpath "$.data.document_id"
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/merchant-documents/{{document_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.document_id" == {{document_id}}

# ---------------------------------------------------------------------------
# 9. TOPUP — credit the sender card
# ---------------------------------------------------------------------------
POST {{base_url}}/api/topups/create
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "amount": 200000,
  "topup_method": "bank_transfer",
  "idempotency_key": "e2e-topup-{{e2e_email}}"
}

HTTP 201
[Captures]
topup_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/topups/{{topup_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.id" == {{topup_id}}

# ---------------------------------------------------------------------------
# 10. TRANSFER — second card + saldo, then P2P transfer sender -> receiver
# ---------------------------------------------------------------------------
POST {{base_url}}/api/card/create
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "user_id": {{user_id}},
  "card_type": "debit",
  "expiry_date": "2035-12-31T00:00:00+07:00",
  "cvv": "456",
  "card_provider": "MASTERCARD"
}

HTTP 201
[Captures]
receiver_card_number: jsonpath "$.data.card_number"
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/saldos
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{receiver_card_number}}",
  "balance": 100000
}

HTTP 201

POST {{base_url}}/api/transfers/create
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "sender_card_number": "{{card_number}}",
  "receiver_card_number": "{{receiver_card_number}}",
  "amount": 50000,
  "idempotency_key": "e2e-transfer-{{e2e_email}}"
}

HTTP 201
[Captures]
transfer_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/transfers/{{transfer_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.id" == {{transfer_id}}

# ---------------------------------------------------------------------------
# 11. WITHDRAW — small cash-out from the sender card
# ---------------------------------------------------------------------------
POST {{base_url}}/api/withdraws/create
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "amount": 10000,
  "idempotency_key": "e2e-withdraw-{{e2e_email}}"
}

HTTP 201
[Captures]
withdraw_id: jsonpath "$.data.withdraw_id"
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/withdraws/{{withdraw_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.withdraw_id" == {{withdraw_id}}

# ---------------------------------------------------------------------------
# 12. TRANSACTION — merchant API-key transaction (X-Api-Key header)
# ---------------------------------------------------------------------------
POST {{base_url}}/transactions/create
X-Api-Key: {{api_key}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "amount": 25000,
  "payment_method": "bank_transfer",
  "idempotency_key": "e2e-txn-{{e2e_email}}"
}

HTTP 201
[Captures]
transaction_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.merchant_id" == {{merchant_id}}

GET {{base_url}}/transactions/{{transaction_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.id" == {{transaction_id}}

# ---------------------------------------------------------------------------
# 13. CREDIT LIFECYCLE — limit, authorize, reverse, billing
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
jsonpath "$.data.credit_limit" == "5000000"

GET {{base_url}}/api/v1/cards/{{card_number}}/limit
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.data.card_number" == "{{card_number}}"

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
  "idempotency_key": "e2e-auth-{{e2e_email}}"
}

HTTP 200
[Captures]
auth_txn_id: jsonpath "$.data.txn_id"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.txn_id" exists

POST {{base_url}}/api/v1/cards/reverse
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "txn_id": "{{auth_txn_id}}",
  "card_number": "{{card_number}}",
  "amount": 100000,
  "idempotency_key": "e2e-reverse-{{e2e_email}}"
}

HTTP 200
[Asserts]
jsonpath "$.reversed" == true

POST {{base_url}}/api/v1/cards/billing/trigger
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "billing_cycle_day": 5
}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"

GET {{base_url}}/api/v1/cards/{{card_number}}/statement
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"

# ---------------------------------------------------------------------------
# 14. AUTH SESSION — refresh + logout
# ---------------------------------------------------------------------------
POST {{base_url}}/refresh-token
Content-Type: application/json

{
  "refresh_token": "{{refresh_token}}"
}

HTTP 200
[Captures]
access_token_2: jsonpath "$.data.access_token"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.access_token" exists

GET {{base_url}}/logout
Authorization: Bearer {{access_token_2}}

HTTP 200
