# API Specification

This table outlines the core synchronous and asynchronous endpoints required for the SALESTORM pipeline.

| Method | Endpoint | Purpose | Request Body | Response | Status Codes | Auth Req. | Idempotent |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/products/{id}` | Product Discovery | N/A | Product details, price | `200 OK`, `404 Not Found` | No | Yes |
| `POST` | `/cart/items` | Add item to cart | `{"product_id": "uuid", "quantity": 1}` | Cart summary | `200 OK`, `400 Bad Request` | Yes | Yes (if same request) |
| `POST` | `/reservations` | Reserve inventory for checkout | `{"product_id": "uuid", "quantity": 1}` | Reservation details, expiry time | `201 Created`, `409 Conflict` (Out of stock) | Yes | **Yes** (via `Idempotency-Key` header) |
| `POST` | `/checkout` | Initiate order placement | `{"reservation_id": "uuid", "payment_method": "..."}` | Order ID, Payment Intent | `201 Created`, `400 Bad Request` | Yes | **Yes** |
| `POST` | `/payments` | Process payment | `{"order_id": "uuid", "amount": 100.00}` | Payment status | `202 Accepted`, `402 Payment Required` | Yes | **Yes** (via `Idempotency-Key` header) |
| `GET` | `/orders/{id}` | Check order status | N/A | Order details, status | `200 OK`, `404 Not Found` | Yes | Yes |
| `POST` | `/reservations/{id}/release` | Manually release a reservation | N/A | Success message | `200 OK`, `404 Not Found` | Yes | Yes |

### Important Error Handling
- **409 Conflict**: Returned during `POST /reservations` when the `available_quantity` constraint fails. The client should inform the user that the item is sold out.
- **429 Too Many Requests**: Returned by the API Gateway if the traffic exceeds the 10,000 req/sec limit, protecting downstream services.
