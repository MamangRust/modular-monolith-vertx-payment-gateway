POST http://localhost:8080/api/products
Content-Type: application/json

{
  "name": "Produk Test",
  "description": "Ini produk untuk testing Hurl",
  "price": 150000.0,
  "stock": 50
}

HTTP 201
[Asserts]
header "Content-Type" contains "application/json"
jsonpath "$.success" == true
jsonpath "$.data.id" exists

[Captures]
product_id: jsonpath "$.data.id"


GET http://localhost:8080/api/products

HTTP 200
[Asserts]
header "Content-Type" contains "application/json"
jsonpath "$.success" == true
jsonpath "$.data" exists


GET http://localhost:8080/api/products/{{product_id}}

HTTP 200
[Asserts]
header "Content-Type" contains "application/json"
jsonpath "$.success" == true
jsonpath "$.data.id" == "{{product_id}}"


PUT http://localhost:8080/api/products/{{product_id}}
Content-Type: application/json

{
  "name": "Produk Test Updated",
  "description": "Update deskripsi produk",
  "price": 175000.0,
  "stock": 60
}

HTTP 200
[Asserts]
header "Content-Type" contains "application/json"
jsonpath "$.success" == true
jsonpath "$.data.name" == "Produk Test Updated"


DELETE http://localhost:8080/api/products/{{product_id}}

HTTP 200
[Asserts]
header "Content-Type" contains "application/json"
jsonpath "$.success" == true
