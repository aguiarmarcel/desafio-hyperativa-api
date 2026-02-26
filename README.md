# Hyperativa API – Card Import & Lookup

API for batch card import and PAN lookup, focusing on:

* True idempotency
* Duplicate protection
* High write performance
* Efficient lookup
* Secure storage (hash + encryption)

---

## 📌 Architecture

Architecture based on separation of responsibilities:

* **Controller** → HTTP layer
* **UseCase** → business logic
* **Repository** → persistence
* **CryptoService** → cryptography isolation
* **Database** → responsible for ensuring uniqueness

This separation enables:

* Testability
* Independent evolution
* Low coupling
* Scalability

---

# 🆕 Asynchronous Processing Architecture (AWS SQS)

The batch import endpoint now operates asynchronously using **Amazon SQS**.

Instead of writing directly to the database:

1. The file is parsed in streaming mode.
2. Records are grouped into SQS-safe batches.
3. Batches are published to an SQS queue (`card-import`).
4. A worker (`CardImportWorker`) consumes messages.
5. The worker performs batch insert with idempotent guarantees.

### Why SQS?

* Decouples API from heavy database operations.
* Prevents request blocking.
* Enables horizontal scaling.
* Provides automatic retry.
* Supports Dead-Letter Queues (DLQ).
* Fully managed infrastructure (no cluster maintenance).

This design increases resilience and throughput under load.

---

## 🗂 Updated API Route Flow – Hyperativa API

```text
Client
   │
   │ POST /cards/import
   ▼
ImportCardsUseCase
   │
   │ → Publish batches to SQS
   ▼
AWS SQS (card-import)
   │
   ▼
CardImportWorker
   │
   │ → Batch insert (INSERT IGNORE)
   ▼
MySQL (UNIQUE pan_hash)
```

The synchronous card creation and lookup flow remain unchanged.

---

## 🔁 Retry & Dead-Letter Handling

### Automatic Retry

If the worker fails while processing a message:

* The message is NOT deleted.
* After the visibility timeout, SQS re-delivers the message.
* Retries occur automatically.

### Dead-Letter Queue (DLQ)

If a message exceeds `maxReceiveCount`:

* It is automatically moved to `card-import-dlq`.
* Prevents infinite retry loops.
* Allows operational inspection.

This guarantees:

* No silent message loss.
* Safe retry of transient failures.
* Isolation of permanent failures.

---

## 📊 Observability

### Application-Level Monitoring (Micrometer + Actuator)

The application exposes operational metrics via:

```
/actuator/health
/actuator/metrics
/actuator/prometheus
```

Custom metrics include:

* `card_import_messages_processed_total`
* `card_import_message_failures_total`
* `card_import_records_inserted_total`
* `card_import_records_duplicates_total`
* `card_import_message_processing_seconds`
* `card_import_queue_poll_empty_total`

This enables:

* Throughput tracking
* Failure rate monitoring
* Processing latency measurement
* Operational visibility

---

### AWS CloudWatch Monitoring

From SQS metrics:

* `ApproximateNumberOfMessagesVisible` → backlog size
* `ApproximateNumberOfMessagesNotVisible` → in-flight messages
* `ApproximateAgeOfOldestMessage` → SLA indicator
* DLQ `ApproximateNumberOfMessagesVisible` → critical failure indicator

Recommended alarms:

* DLQ messages ≥ 1
* AgeOfOldestMessage > threshold
* Backlog continuously increasing

This ensures production-grade monitoring.

---

## ⚠ SQS Message Size Constraint

SQS enforces a hard limit of **256KB per message**.

To prevent overflow:

* Import batches are limited to safe record counts.
* Database batch size can remain larger internally.

This avoids runtime message rejection and ensures stable async processing.

---

## 📌 Architecture (Original Flow – Preserved)

Architecture based on separation of responsibilities:

* **Controller** → HTTP layer
* **UseCase** → business logic
* **Repository** → persistence
* **CryptoService** → cryptography isolation
* **Database** → responsible for ensuring uniqueness

---

## 🗂 API Route Flow – Hyperativa API

```text
           ┌─────────────┐
           │   Client    │
           └─────┬───────┘
                 │
                 │ POST /auth
                 │ { login, password }
                 ▼
           ┌─────────────┐
           │ AuthController │
           └─────┬───────┘
                 │
                 │ returns JWT
                 ▼
           ┌─────────────┐
           │  Client    │ (uses token)
           └─────┬───────┘
                 │
      ┌──────────┴───────────┐
      │                      │
      ▼                      ▼
POST /cards               GET /cards?pan=...
{ "pan": "..."}           Returns ID if exists
      │                      │
      ▼                      ▼
┌─────────────┐          ┌─────────────┐
│ CardController │        │ CardController │
└─────┬───────┘          └─────────────┘
      │
      │
      ▼
RegisterCardUseCase  ← creates card
LookupCardUseCase    ← looks up card
ImportCardsUseCase   ← parses file & publishes to SQS
      │
      ▼
┌─────────────┐
│ CardRepository │
│ Database (MySQL) │
└─────────────┘
      │
      ├── hash + encrypt PAN
      ├── UNIQUE pan_hash ensures idempotency
      └── batch insert / fast lookup
```

---

## 🔐 Persistence Model

```sql
CREATE TABLE card (
  id binary(16) NOT NULL,
  pan_hash binary(32) NOT NULL,
  pan_enc varbinary(512) NOT NULL,
  pan_iv varbinary(12) NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY pan_hash (pan_hash)
);
```

(Original justifications preserved)

---

## 🔁 True Idempotency

Ensured by **UNIQUE KEY pan_hash (pan_hash)**.

Impact:

* The same card is never inserted twice
* Reprocessing does not create duplicates
* Safe retries from SQS
* Database remains the single source of truth

---

## 🚀 High Write Performance

1. **Batch Insert**
2. **Batch Transaction**
3. **Lean unique index**
4. **Asynchronous offloading via SQS**

Now enhanced with queue-based decoupling.

---

## 🧩 Scalability (Extended)

* File streaming
* Configurable DB batch size
* SQS decoupling
* Horizontal worker scaling
* Managed retry & DLQ
* CloudWatch monitoring
* Metrics-driven performance tuning

System scales both vertically and horizontally.

---

## 📈 Strengths (Extended)

✔ Idempotent
✔ Secure
✔ High-performance
✔ Asynchronous
✔ Retry-safe
✔ DLQ-protected
✔ Observable
✔ Horizontally scalable
✔ Clean architecture
✔ Production-ready

---

## 📌 Possible Evolutions (Updated)

* Auto-scaling workers
* Dedicated dashboard (Grafana / CloudWatch Dashboard)
* Import status endpoint
* Distributed tracing (OpenTelemetry)
* Circuit breaker for DB pressure

---

## Conclusion

* Sensitive data security
* Data integrity
* Write performance
* Structural simplicity
* Real scalability
* Fault tolerance
* Observability

Database ensures consistency.
SQS ensures delivery.
Application ensures security and correctness.

Production-ready architecture with asynchronous resilience and operational monitoring.

---