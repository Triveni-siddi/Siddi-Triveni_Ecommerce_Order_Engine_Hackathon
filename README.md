# Siddi-Triveni_Ecommerce_Order_Engine_Hackathon# Distributed E-Commerce Order Engine

---

# Project Overview

This project is a **menu-driven CLI application** built in Java that simulates a robust, scalable backend engine for an e-commerce platform — inspired by real-world systems like Amazon, Flipkart, and Meesho.

Modern e-commerce platforms face complex backend challenges every second:
- Multiple users competing for the same product stock
- Payment failures mid-transaction
- Orders being cancelled or partially returned
- Fraudulent bulk ordering
- System failures requiring safe recovery

This application simulates all of these scenarios using clean, modular Java — demonstrating distributed system concepts like atomic transactions, stock reservation, event-driven architecture, concurrency control, and fault tolerance — entirely in memory without any external dependencies.

---

##  Features Implemented

| # | Task | Description |
|---|------|-------------|
| 1 | **Product Management** | Add products with unique IDs, view all products, update stock. Constraints: no negative stock, no duplicate IDs. |
| 2 | **Multi-User Cart System** | Each user maintains an independent cart. Supports add, remove, and quantity update. |
| 3 | **Real-Time Stock Reservation** | Stock is reserved immediately when added to cart and released when removed — prevents overselling. |
| 4 | **Concurrency Simulation** | Simulates multiple users accessing the same product simultaneously using `ReentrantLock` per product (logical locking). Only one user wins when stock is limited. |
| 5 | **Order Placement Engine** | Atomic cart-to-order conversion: validate cart → calculate total → lock stock → create order → clear cart. All-or-nothing. |
| 6 | **Payment Simulation** | Simulates payment with 75% success rate. Handles both success and random failure paths. |
| 7 | **Transaction Rollback** | If payment fails, all previous steps are undone: order is deleted, stock reservations are fully restored. |
| 8 | **Order State Machine** | Orders follow strict lifecycle: `CREATED → PENDING_PAYMENT → PAID → SHIPPED → DELIVERED`. Invalid transitions are blocked. Side states: `FAILED`, `CANCELLED`. |
| 9 | **Discount & Coupon Engine** | Auto-discount: 10% on orders above ₹1000, extra 5% if qty > 3 for same product. Coupon codes: `SAVE10` (10% off), `FLAT200` (₹200 flat off). Invalid combinations are rejected. |
| 10 | **Inventory Alert System** | Flags products with low available stock (≤ 3 units). Blocks purchase when stock = 0. |
| 11 | **Order Management** | View all orders, search by Order ID, filter by status (PAID, CANCELLED, etc.). |
| 12 | **Order Cancellation Engine** | Cancel an order and restore stock. Edge case: cannot cancel an already-cancelled order. |
| 13 | **Return & Refund System** | Supports partial returns. Updates stock and recalculates order total on each return. |
| 14 | **Event-Driven System** | Simulates an event queue: `ORDER_CREATED → PAYMENT_SUCCESS → INVENTORY_UPDATED`. Events execute in order; a failure stops all subsequent events. |
| 15 | **Inventory Reservation Expiry** | Reservations automatically expire after 5 minutes if the order is not placed. Expired reservations are released back to available stock. |
| 16 | **Audit Logging System** | Immutable, append-only timestamped log of all system actions (e.g., `[14:32:01] ORDER_101 created for USER_A total=₹4999`). |
| 17 | **Fraud Detection System** | Flags users placing 3+ orders within 1 minute. Also flags high-value orders (above ₹10,000) as suspicious. |
| 18 | **Failure Injection System** | Toggle payment, order creation, or inventory update failures on/off at runtime to test system recovery. |
| 19 | **Idempotency Handling** | Prevents duplicate orders if the user triggers "Place Order" multiple times within the same session window. |
| 20 | **Microservice Simulation** | System is divided into independent service modules: `ProductService`, `CartService`, `OrderService`, `FraudService`, `EventService` — with loose coupling via constructor injection. |

---

##  Design Approach

### Architecture
The project follows a **layered clean architecture** with clear separation of concerns:

```
com.ecommerce
├── Main.java                  ← CLI entry point & menu router
├── model/                     ← Pure data classes (no logic)
│   ├── Product.java
│   ├── CartItem.java
│   ├── Order.java             ← Embeds the state machine
│   └── AuditLog.java          ← Append-only static log store
└── service/                   ← Business logic layer
    ├── ProductService.java
    ├── CartService.java
    ├── OrderService.java
    ├── FraudService.java
    ├── EventService.java
    └── ConcurrencyService.java
```

### Key Design Decisions

**Loose Coupling via Constructor Injection**
Services receive their dependencies through constructors, making each module independently testable and replaceable.

**Concurrency Safety**
A `ReentrantLock` is maintained per product ID in a `ConcurrentHashMap`. This ensures that when multiple threads try to reserve the same product simultaneously, only one can hold the lock — preventing race conditions and overselling.

**Atomic Order Placement**
Order placement is designed as an all-or-nothing transaction. If payment fails, the system rolls back every completed step (releases stock reservations, discards the order object) before returning. This mirrors the SAGA pattern used in real distributed systems.

**State Machine with Guard Transitions**
The `Order` class contains a static transition map. Any call to `transitionTo()` is validated against this map. Invalid transitions (e.g., DELIVERED → PAID) are silently rejected, maintaining correct lifecycle integrity.

**Immutable Audit Log**
`AuditLog` stores entries in an unmodifiable list view. All actions are appended with a timestamp and the list is never cleared, ensuring a full and honest audit trail throughout the session.

**Microservice Boundaries**
Each service class represents a conceptual microservice. In production, each could be deployed as an independent service with its own database. The current design uses in-memory maps with interface-level decoupling to simulate this.

---

##  Assumptions

- **In-memory storage only** — all data (products, carts, orders, logs) is stored in Java collections and resets when the application exits. No database or file persistence is used.
- **Single JVM process** — microservices are simulated as classes within the same process. Thread safety is handled at the service level using locks and concurrent collections.
- **Payment success rate** is set at 75% (random). This can be adjusted in `OrderService.java`.
- **Fraud threshold**: a user is flagged if they place 3 or more orders within a 1-minute window, or if any single order exceeds ₹10,000.
- **Reservation expiry** is set to 5 minutes per reservation entry. The expiry check is triggered manually via menu option 16.
- **Idempotency window** is scoped to the current session — duplicate order detection is based on user ID and a 10-second time bucket.
- **Coupon codes** are case-insensitive and cannot be stacked (only one type is applied per order calculation).
- **Partial returns** are limited to the quantity originally ordered. Already-returned quantities are tracked and deducted from the returnable amount.
- **Concurrency simulation** uses Java threads with a `CountDownLatch` to release all users simultaneously, mimicking a real race condition.

---

##  How to Run the Project

### Prerequisites
- **Java JDK 11 or higher** must be installed
- Verify with: `java -version`

### Option 1 — Using the run script (recommended)

```bash
chmod +x run.sh
./run.sh
```

### Option 2 — Manual compile and run

```bash
# From the project root directory
mkdir -p out
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out com.ecommerce.Main
```

### Option 3 — Using an IDE (IntelliJ / Eclipse / VS Code)

1. Open the project root folder in your IDE
2. Mark `src/main/java` as the **Sources Root**
3. Run `com.ecommerce.Main`

---

###  CLI Menu Reference

```
─────────────────────────────────────────────────────
 1.  Add Product          2.  View Products
 3.  Add to Cart          4.  Remove from Cart
 5.  View Cart            6.  Apply Coupon (preview)
 7.  Place Order          8.  Cancel Order
 9.  View Orders         10.  Low Stock Alert
11.  Return Product      12.  Simulate Concurrent Users
13.  View Logs           14.  Trigger Failure Mode
15.  Run Event Queue     16.  Expire Reservations
17.  Microservice Status 18.  Search Order
19.  Advance Order State
 0.  Exit
─────────────────────────────────────────────────────
```

###  Suggested Test Walkthrough

```
1.  Launch the app — 5 products are pre-seeded (P001–P005)
2.  Option 2  → View all products and their stock
3.  Option 3  → Add P001 (iPhone 15) qty=2 for USER_A
4.  Option 6  → Preview total with coupon SAVE10
5.  Option 7  → Place order for USER_A (payment may randomly fail & rollback)
6.  Option 9  → View all orders and their status
7.  Option 12 → Simulate 3 users concurrently trying to buy P003 (stock=2)
8.  Option 13 → View the full immutable audit log
9.  Option 14 → Toggle payment failure mode ON, then place another order
10. Option 15 → Run the event-driven order queue simulation
```

---

##  Author

**Repository:** `Siddi Triveni_Ecommerce_Order_Engine_Hackathon`
