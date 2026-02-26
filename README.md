Here’s the full English translation of your README, keeping it technical, concise, and justifying architectural decisions:

---

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
ImportCardsUseCase   ← processes CSV/JSON file
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

### How the flow works:

1. **Authentication (`POST /auth`)**

    * Returns JWT used for all other routes

2. **Create Card (`POST /cards`)**

    * Receives PAN → hash + encrypt → stores in database
    * Idempotent → prevents duplicates

3. **Lookup Card (`GET /cards?pan=...`)**

    * Searches by `pan_hash`
    * Returns `id` without exposing PAN

4. **Import Cards (`POST /cards/import`)**

    * Receives CSV/JSON file → streaming + batch insert
    * Database ensures uniqueness → true idempotency
    * Returns summary (inserted, duplicates, invalid)

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

**Decisions:**

* **id binary(16)** → compact UUID, better performance than varchar(36), smaller index → less IO
* **pan_hash binary(32)** → SHA-256, used as logical key, allows secure comparison without exposing PAN
* **pan_enc varbinary** → stores encrypted PAN (AES-GCM), ensures confidentiality, allows future decryption
* **pan_iv** → required for authenticated encryption (AES-GCM), ensures per-record security

---

## 🔁 True Idempotency

Ensured by **UNIQUE KEY pan_hash (pan_hash)**.

Impact:

* The same card is never inserted twice
* Reprocessing the same file does not create duplicates
* System tolerant to retries
* Does not rely on application logic; database is the source of truth

---

## 🚫 Duplicate Protection

Uses **INSERT IGNORE** with unique index:

* Duplicate insertion → ignored
* No unnecessary exceptions
* Batch processing not interrupted

Result: high resilience, continuous processing.

---

## 🚀 High Write Performance

1. **Batch Insert** → `jdbcTemplate.batchUpdate(...)`

    * Reduces round trips to database
    * Reduces transaction overhead
    * Higher throughput

2. **Batch Transaction** → `TransactionTemplate` per batch

    * Prevents giant transactions
    * Reduces lock time
    * Better memory usage

3. **Lean unique index** → only on `pan_hash`

    * Lower write cost
    * Less B-Tree maintenance
    * Better overall performance

---

## 🔎 Efficient Lookup Indexing

Lookup performed by:

```sql
WHERE pan_hash = ?
```

* `pan_hash` is **UNIQUE** → O(log n) lookup, index used directly, no full scan
* **Never use functions on the column** (e.g., HEX, CAST) → would break index usage

---

## 🧠 Resilient Parsing

* PAN extraction based on regex
* Reason: files may vary in padding, comments may exist
* Result: robust parser, lower risk of production failure

---

## 🔒 Security

* Never store PAN in plaintext
* Hash for comparison
* Encryption for confidentiality
* Isolated crypto service

Allows PCI-DSS compliance and security best practices.

---

## 📝 API Routes

1. **Create Card (POST /cards)**

```http
POST /cards
Content-Type: application/json

{
  "pan": "1234567890123456"
}
```

* Creates a new card using hash and AES-GCM encryption
* Returns `201 Created` and card `id`
* Idempotent: if the card exists, returns the same `id` without creating duplicates

---

2. **Lookup Card by PAN (GET /cards?pan=...)**

```http
GET /cards?pan=1234567890123456
```

* Efficient lookup using `pan_hash` index
* Returns card details without exposing PAN in plaintext

---

3. **Batch Import Cards (POST /cards/import)**

```http
POST /cards/import
Content-Type: multipart/form-data
```

* Receives CSV/JSON file of cards
* Batch processing with minimal memory footprint (streaming)
* Idempotent, tolerant to duplicates
* Returns summary: total, inserted, duplicates, invalid

---

## 📊 Import Result

Example:

```json
{
  "totalLines": 13,
  "inserted": 8,
  "duplicates": 2,
  "invalid": 0
}
```

---

## 🧩 Scalability

* File streaming → avoids loading entire file in memory
* Configurable batch (`BATCH_SIZE`)
* Database ensures uniqueness
* Lean indices → millions of records processable
* Adjustable: batch size, connection pool, InnoDB settings

---

## 📈 Strengths

✔ Idempotent ✔ Secure ✔ High-performance ✔ Resilient to imperfect files ✔ Structurally correct ✔ Database ensures consistency ✔ Clean architecture

---

## 📌 Possible Evolutions

* Asynchronous import (queue + worker)
* Metrics (Micrometer / Prometheus)
* Rate limiting
* Dead-letter for invalid files
* Import audit

---

## Conclusion

* Sensitive data security
* Data integrity
* Write performance
* Structural simplicity
* Real scalability

Database ensures consistency. Application ensures security. Solid architecture for production-ready environments.

---