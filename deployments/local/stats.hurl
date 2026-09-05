# =============================================================================
# PAYMENT GATEWAY — STATS ENDPOINT E2E SUITE
# Covers: saldo stats, card stats (global + by-card), merchant analytics
#         (global + by-merchant + by-apikey), topup stats, transfer stats,
#         withdraw stats, transaction stats.
#
# Wire-format notes:
#   - protobuf int64 fields serialize as JSON strings -> assert with quotes
#     (e.g. card/merchant `total_balance`/`total_amount` are strings)
#   - protobuf int32 fields (topup/transfer/withdraw/txn `total_success`,
#     `total_amount`, saldo `total_balance`) serialize as JSON numbers
#   - every stats response wraps in `{ "status": "success", "data": ... }`
# =============================================================================

# ---------------------------------------------------------------------------
# 1. AUTH — verify, login, profile (setup)
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
  "email": "{{stats_email}}",
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
jsonpath "$.data.email" == "{{stats_email}}"

# ---------------------------------------------------------------------------
# 2. DATA SETUP — card, saldo, merchant, topup, withdraw, transaction
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
jsonpath "$.data.card_number" exists

POST {{base_url}}/api/saldos
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "balance": 1000000
}

HTTP 201
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/merchants
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "user_id": {{user_id}},
  "name": "{{stats_merchant}}"
}

HTTP 201
[Captures]
merchant_id: jsonpath "$.data.id"
api_key: jsonpath "$.data.api_key"
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data.api_key" exists

POST {{base_url}}/api/topups/create
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "amount": 200000,
  "topup_method": "bank_transfer",
  "idempotency_key": "stats-topup-{{stats_email}}"
}

HTTP 201
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/transactions/create
X-Api-Key: {{api_key}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "amount": 25000,
  "payment_method": "bank_transfer",
  "idempotency_key": "stats-txn-{{stats_email}}"
}

HTTP 201
[Asserts]
jsonpath "$.status" == "success"

POST {{base_url}}/api/withdraws/create
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "card_number": "{{card_number}}",
  "amount": 10000,
  "idempotency_key": "stats-wd-{{stats_email}}"
}

HTTP 201
[Asserts]
jsonpath "$.status" == "success"

# ---------------------------------------------------------------------------
# 3. SALDO STATS — data: 12 months, total_balance as JSON number
# ---------------------------------------------------------------------------
GET {{base_url}}/api/saldos/stats/yearly-total-balances?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/api/saldos/stats/monthly-balances?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].month" exists
jsonpath "$.data[0].total_balance" exists

GET {{base_url}}/api/saldos/stats/yearly-balances?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

# ---------------------------------------------------------------------------
# 4. CARD STATS — global (dashboard is an object; others 12-month arrays with
#    string amounts due to int64 protobuf serialization)
# ---------------------------------------------------------------------------
GET {{base_url}}/api/card/dashboard
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isObject
jsonpath "$.data.total_balance" exists
jsonpath "$.data.total_topup" exists
jsonpath "$.data.total_transaction" exists

GET {{base_url}}/api/card/monthly-balance?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].month" exists
jsonpath "$.data[0].total_balance" exists

GET {{base_url}}/api/card/yearly-balance?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/api/card/monthly-topup-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-topup-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/monthly-withdraw-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-withdraw-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/monthly-transaction-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-transaction-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/monthly-transfer-sender-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-transfer-sender-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/monthly-transfer-receiver-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-transfer-receiver-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

# ---------------------------------------------------------------------------
# 5. CARD STATS — by card number (query param)
# ---------------------------------------------------------------------------
GET {{base_url}}/api/card/dashboard/{{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isObject
jsonpath "$.data.total_balance" exists

GET {{base_url}}/api/card/monthly-balance-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_balance" exists

GET {{base_url}}/api/card/yearly-balance-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection


GET {{base_url}}/api/card/monthly-topup-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-topup-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection


GET {{base_url}}/api/card/monthly-withdraw-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-withdraw-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection


GET {{base_url}}/api/card/monthly-transaction-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-transaction-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection


GET {{base_url}}/api/card/monthly-transfer-sender-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-transfer-sender-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection


GET {{base_url}}/api/card/monthly-transfer-receiver-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/card/yearly-transfer-receiver-amount-by-card?year=2026&card_number={{card_number}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection


# ---------------------------------------------------------------------------
# 6. MERCHANT ANALYTICS — global (string amounts, int64 wire format)
# ---------------------------------------------------------------------------
GET {{base_url}}/api/merchants/transactions
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].card_number" exists

GET {{base_url}}/api/merchants/monthly-payment-methods?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/api/merchants/yearly-payment-methods?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/api/merchants/monthly-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/yearly-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/monthly-total-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/yearly-total-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

# ---------------------------------------------------------------------------
# 7. MERCHANT ANALYTICS — by merchant id + api key
# ---------------------------------------------------------------------------
GET {{base_url}}/api/merchants/transactions/{{merchant_id}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].merchant_id" exists

GET {{base_url}}/api/merchants/transactions/api-key/{{api_key}}
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].card_number" exists

GET {{base_url}}/api/merchants/monthly-payment-methods-by-merchant/{{merchant_id}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/api/merchants/yearly-payment-methods-by-merchant/{{merchant_id}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/api/merchants/monthly-amount-by-merchant/{{merchant_id}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/yearly-amount-by-merchant/{{merchant_id}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/monthly-totalamount-by-merchant/{{merchant_id}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/yearly-totalamount-by-merchant/{{merchant_id}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/monthly-payment-methods-by-apikey/{{api_key}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/api/merchants/yearly-payment-methods-by-apikey/{{api_key}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/api/merchants/monthly-amount-by-apikey/{{api_key}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/yearly-amount-by-apikey/{{api_key}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/monthly-totalamount-by-apikey/{{api_key}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/merchants/yearly-totalamount-by-apikey/{{api_key}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

# ---------------------------------------------------------------------------
# 8. TOPUP STATS — global + by card (total_success/total_amount are numbers)
# ---------------------------------------------------------------------------
GET {{base_url}}/api/topups/monthly-success?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].month" exists
jsonpath "$.data[0].total_success" exists

GET {{base_url}}/api/topups/yearly-success?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/api/topups/monthly-failed?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/topups/yearly-failed?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/topups/monthly-methods?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].topup_method" exists

GET {{base_url}}/api/topups/yearly-methods?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].topup_method" exists

GET {{base_url}}/api/topups/monthly-amounts?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/topups/yearly-amounts?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/topups/monthly-success-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_success" exists

GET {{base_url}}/api/topups/yearly-success-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/api/topups/monthly-failed-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/topups/yearly-failed-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/topups/monthly-methods-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].topup_method" exists

GET {{base_url}}/api/topups/yearly-methods-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].topup_method" exists

GET {{base_url}}/api/topups/monthly-amounts-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/topups/yearly-amounts-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

# ---------------------------------------------------------------------------
# 9. TRANSFER STATS — global + by card
# ---------------------------------------------------------------------------
GET {{base_url}}/api/transfers/monthly-success?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_success" exists

GET {{base_url}}/api/transfers/yearly-success?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/api/transfers/monthly-failed?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/transfers/yearly-failed?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/transfers/monthly-amount?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/transfers/yearly-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/transfers/monthly-success-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_success" exists

GET {{base_url}}/api/transfers/yearly-success-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/api/transfers/monthly-failed-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/transfers/yearly-failed-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/transfers/monthly-by-sender/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/transfers/yearly-by-sender/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection


GET {{base_url}}/api/transfers/monthly-by-receiver/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/transfers/yearly-by-receiver/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection


# ---------------------------------------------------------------------------
# 10. WITHDRAW STATS — global + by card
# ---------------------------------------------------------------------------
GET {{base_url}}/api/withdraws/monthly-success?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_success" exists

GET {{base_url}}/api/withdraws/yearly-success?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/api/withdraws/monthly-failed?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/withdraws/yearly-failed?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/withdraws/monthly-amount?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/withdraws/yearly-amount?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/withdraws/monthly-success-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_success" exists

GET {{base_url}}/api/withdraws/yearly-success-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/api/withdraws/monthly-failed-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/withdraws/yearly-failed-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/api/withdraws/monthly-amount-bycard/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/api/withdraws/yearly-amount-bycard/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

# ---------------------------------------------------------------------------
# 11. TRANSACTION STATS — global + by card
# ---------------------------------------------------------------------------
GET {{base_url}}/transactions/monthly-success?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_success" exists

GET {{base_url}}/transactions/yearly-success?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/transactions/monthly-failed?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/transactions/yearly-failed?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/transactions/monthly-methods?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/transactions/yearly-methods?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/transactions/monthly-amounts?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/transactions/yearly-amounts?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/transactions/monthly-methods-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/transactions/yearly-success-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].year" exists

GET {{base_url}}/transactions/monthly-failed-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/transactions/yearly-failed-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_failed" exists

GET {{base_url}}/transactions/monthly-methods-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/transactions/yearly-methods-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].payment_method" exists

GET {{base_url}}/transactions/monthly-amounts-by-card/{{card_number}}?year=2026&month=8
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists

GET {{base_url}}/transactions/yearly-amounts-by-card/{{card_number}}?year=2026
Authorization: Bearer {{access_token}}

HTTP 200
[Asserts]
jsonpath "$.status" == "success"
jsonpath "$.data" isCollection
jsonpath "$.data[0].total_amount" exists
