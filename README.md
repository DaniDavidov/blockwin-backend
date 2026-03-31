# **BlockWin Protocol API**

A distributed, validator-based platform monitoring system that leverages a Proof-of-Stake-inspired mechanism, consensus algorithms, and real-time processing to deliver reliable and trust-minimized health insights for web platforms.

## **Overview**

BlockWin is designed as a **modular monolith** with a clear path toward microservices. It enables decentralized validators to submit reports (uptime, TLS, etc.) about monitored platforms, aggregates them through a consensus mechanism, and produces reliable system-wide insights.

The system emphasizes:

- Fault tolerance through distributed validation
- Trust weighting via validator reputation
- High-throughput processing using queues and worker pools
- Event-driven architecture using Kafka
- Low-latency access via Redis caching

## **1. Validator Onboarding (PoS-inspired mechanism)**

The system uses a **Proof-of-Stake-like validator registration flow**:

1. Validator stakes tokens in a smart contract
2. Validator submits to backend:
    - Transaction hash
    - Validator metadata
    - ECDSA signed message
3. Backend:
    - Verifies the transaction on-chain
    - Recovers public address from signature
    - Validates ownership
4. On success:
    - Generates an **API key**
    - Registers validator in the system

This ensures:

- Sybil resistance
- Economic accountability
- Cryptographic identity binding

---

## **2. Communication Layer (WebSocket)**

The communication layer is responsible for securely onboarding validators and maintaining efficient, stateful, bidirectional communication via WebSockets. This layer is designed for **low-latency ingestion**, **secure identity verification**, and **connection-aware state management**.

#### **2.1 WebSocket Authentication Flow**

Before a validator is allowed to establish a WebSocket connection, it must pass through a custom handshake validation process:

- A **`WebSocketHandshakeInterceptor`** intercepts every incoming connection request.
- The validator includes its **API key** (issued during staking registration) in the request headers.
- The interceptor:
    - Validates the API key against the backend (typically via a database or cache lookup).
    - Resolves the validator identity (UUID).
    - Injects validator metadata into the **WebSocket session attributes**.

This ensures that:

- Only authenticated validators can connect.
- The validator identity is **bound to the session context**, eliminating the need for repeated authentication during message exchange.

---

#### **2.2 ValidatorWebSocketHandler**

Once the handshake is successful, the connection is handled by the **`ValidatorWebSocketHandler`**, which is the core component responsible for managing validator sessions and incoming reports.

Its responsibilities include:

**1. Connection Establishment (`afterConnectionEstablished`)**

- Extracts validator identity from session attributes.
- Registers the connection into the **Connection Registry**.
- Initializes validator-specific context if needed (e.g., rate limiting, session tracking).
- Optionally triggers:
    - Reputation cache warm-up (fetch from Redis or DB).
    - Session-scoped metadata initialization.

**2. Message Handling (`handleTextMessage`)**

- Receives incoming messages (typically JSON payloads).
- Deserializes them into domain objects (e.g., `UptimeReport`, `TLSReport`).
- Performs:
    - Basic validation (schema, required fields).
    - Enrichment with validator metadata (validatorId from session).
- Forwards the processed report to the **Ingestion Service**, which pushes it into the **BlockingQueue**.

This design ensures:

- The WebSocket layer remains **thin and non-blocking**.
- Heavy processing is delegated downstream (worker threads).

**3. Connection Termination (`afterConnectionClosed`)**

- Removes the validator session from the **Connection Registry**.
- Cleans up any session-bound resources.
- Prevents stale or zombie connections from affecting the system.

---

#### **2.3 Connection Registry**

The **Connection Registry** is a critical component that maintains active validator sessions.

Structure:
```java
ConcurrentHashMap<UUID, WebSocketSession>
```

Responsibilities:

- **Fast lookup** of active validators by ID.
- Enables:
    - Broadcasting messages (future extensibility).
    - Targeted communication (e.g., challenge/response, slashing alerts).
- Ensures **thread-safe access** using `ConcurrentHashMap`.

Design considerations:

- The registry acts as an **in-memory source of truth** for active connections.
- It is optimized for:
    - O(1) lookup
    - High concurrency
- On disconnect:
    - Entries are removed immediately to prevent memory leaks.

---

## **3. Ingestion Pipeline**
The ingestion pipeline is responsible for transforming incoming validator messages into structured, processable data and reliably feeding them into the round-based consensus system. It is designed around **decoupling, concurrency, and backpressure control**.

---

#### **3.1 Core Design Principles**

The ingestion layer follows a few critical architectural principles:

- **Decoupling WebSocket I/O from processing**
- **Asynchronous, queue-driven processing**
- **Deterministic routing into round states**
- **Thread-safe state mutation**

This ensures that high-throughput validator traffic does not overwhelm the system and that processing remains predictable under load.

---

#### **3.2 Blocking Queue as the Ingestion Buffer**

At the core of the ingestion pipeline is a **`BlockingQueue<Report>`**, which acts as a buffer between:

- Producers → WebSocket handlers
- Consumers → Worker threads (`MessageProcessingService`)

**Why `BlockingQueue`?**

- Built-in thread safety
- Natural backpressure mechanism
- Simple and predictable behavior under load

**Flow:**

1. `ValidatorWebSocketHandler` receives a report
2. The report is validated and enriched
3. The report is pushed into the queue
```java
ingestionQueue.put(report);
```

If the system is under heavy load:

- Producers will **block instead of overwhelming memory**
- This protects downstream components

---

#### **3.3 MessageProcessingService (Worker Engine)**

The **`MessageProcessingService`** is responsible for consuming reports from the queue and routing them into the correct round.

It runs a pool of **long-lived worker threads**:
```java
while (true) {  
    Report report = ingestionQueue.take();  
    process(report);  
}
```

**Key responsibilities:**

- Extract platform identifier from report
- Fetch or initialize the corresponding `RoundState`
- Register the report into the round
- Ensure thread-safe mutation of round state

---

#### **3.4 Report Routing → RoundState**

Each report is mapped to a **specific platform round** via:

platformURL → RoundState

This mapping is handled by the **StateRegistry**, which provides:

- O(1) lookup via `ConcurrentHashMap`
- Lifecycle management of rounds
- Expiration coordination via `DelayQueue`

---

#### **3.5 Backpressure and Load Behavior**

The system handles load gracefully:

- If ingestion rate > processing rate:
    - Queue fills up
    - Producers (WebSocket threads) slow down
- This prevents:
    - Memory exhaustion
    - Unbounded latency spikes

This is a **natural backpressure mechanism**, no custom logic needed.

---

## **4. Round-Based State Management**

The round-based state management layer is the **core coordination mechanism** of the system. It defines how reports are grouped, how time boundaries are enforced, and how state transitions happen safely under concurrency.

At its heart, this layer ensures that:

- Reports are **grouped deterministically per platform**
- Each group (round) has a **strict lifecycle**
- State transitions are **atomic and race-condition safe**

---

#### **4.1 RoundState as the Core Aggregation Unit**

Each monitored platform is associated with a **`RoundState`**, which acts as a temporary container for reports within a defined time window.

A `RoundState` encapsulates:

- Platform identifier (`platformURL`)
- Round timing (start + expiration)
- Aggregated reports (`reportsByType`)
- Validator participation tracking (`bitmapByValidator`)

Conceptually:
```
Platform → Active RoundState → Reports collected → Consensus execution
```

Each round is **isolated**, meaning:

- No report leaks between rounds
- Each execution operates on a fixed dataset

---

#### **4.2 StateRegistry – Centralized State Coordination**

The **`StateRegistry`** is the authoritative component managing all active rounds.

It maintains two critical data structures:

- `ConcurrentHashMap<String, RoundState> stateMap`
- `DelayQueue<RoundState> expirationQueue`

**Responsibilities:**

- Fast lookup of active rounds (O(1))
- Scheduling round expiration
- Coordinating state transitions

This dual-structure design enables:

- Efficient access during ingestion
- Time-based triggering for execution

---

#### **4.3 DelayQueue for Time-Based Execution**

The **`DelayQueue`** is the backbone of round expiration logic.

Each `RoundState` implements `Delayed`, meaning it carries:

- An expiration timestamp
- Logic to determine when it becomes eligible

**Behavior:**

- When a round expires → it becomes available in the queue
- Execution workers block on:

```java
RoundState state = expirationQueue.take();
```

This gives:

- Precise timing guarantees
- No polling
- No manual scheduling complexity

---

#### **4.4 Round Lifecycle**

Each round follows a strict lifecycle:

1. **Initialization**
    - Triggered lazily on first report or platform event
    - Registered in both:
        - `stateMap`
        - `DelayQueue`
2. **Active Phase**
    - Reports are continuously added
    - Validators are tracked via bitmap
    - No structural changes allowed
3. **Expiration**
    - Round becomes available in `DelayQueue`
    - Picked up by execution workers
4. **Execution**
    - Consensus is applied
    - Results are produced
5. **Reset**
    - Old state is removed
    - New round is initialized

---

#### **4.5 Atomic State Reset (Critical Section)**

One of the most important parts of your design is **safe round resetting**.

The challenge:

- Avoid a gap between removing old state and inserting new state
- Prevent race conditions with ingestion threads

**Correct approach:**

Use `ConcurrentHashMap.compute()`:
```java
stateMap.compute(platformURL, (key, oldState) -> newState);
```

This ensures:

- Atomic replacement
- No intermediate “null” state
- Safe concurrent access

---

#### **4.6 Handling Platform Updates Without Corrupting State**

A major complexity arises when:

- A platform is updated (e.g. `checkIntervalSeconds`)
- While a round is already in progress

Naive approach:

- Replace the current state immediately - this breaks consensus consistency

Current implementation -> **Deferred update application:**

**Mechanism:**

- Updates are stored in a **secondary cache/map**
- When a round expires:
    - Execution worker checks for pending update
    - Applies update when creating the next round

This guarantees:

- No mid-round mutation
- Deterministic execution
- Strong consistency

---

#### **4.7 Validator Participation Tracking (Bitmap Integration)**

Each `RoundState` includes:
```java
Map<UUID, Bitmap>
```

This serves multiple purposes:

- Prevent duplicate submissions per report type
- Track validator participation across report types
- Enable efficient validation during ingestion

Why Bitmap?

- Constant-time checks
- Memory efficient
- Easily extensible for new report types

---

#### **4.8 Concurrency Model**

Round state management is designed for **high concurrency**:

**Concurrent components:**

- Multiple ingestion workers writing to the same round
- Execution workers removing expired rounds
- Event listeners updating platform configurations

**Key guarantees:**

- `stateMap` → thread-safe access
- `DelayQueue` → thread-safe scheduling
- `compute()` → atomic state transitions

---

#### **4.9 Memory and Lifecycle Management**

Each round is:

- Created → used → executed → discarded

No long-term memory retention inside `RoundState`.

Benefits:

- Predictable memory usage
- No stale data accumulation
- Clean lifecycle boundaries

---

#### **4.10 Integration with Execution Layer**

This layer seamlessly connects with execution:

- `DelayQueue` → triggers execution
- `ExecutionService` → consumes expired rounds
- `ConsensusService` → processes them

Flow:

RoundState (expired)  
    ↓  
ExecutionService  
    ↓  
ConsensusService  
    ↓  
Kafka event emission

---

## **5. Consensus Mechanism**

The system uses a **pluggable consensus architecture**:
```java
public interface ConsensusMechanism<T extends Report, R extends ConsensusResult>
```

Each report type has its own implementation:

- `UptimeConsensusMechanism` (implemented)
- TLS / Content (future)

---

## **6. Uptime Consensus Algorithm**

### Step 1: Region-Based Partitioning

Reports are grouped by validator region:
```java
Map<Continent, List<UptimeReport>>
```

This enables:

- Detection of regional outages (e.g. CDN issues)
- More accurate fault detection

---

### Step 2: Regional Consensus Execution

For each region:

#### Voting

- Reports mapped to categories:
    - HEALTHY
    - NETWORK_FAILURE
    - SERVER_FAILURE
    - CLIENT_ERROR
    - SECURITY_FAILURE
- Voting is **reputation-weighted**:

voteWeight = validatorReputation (bps)

---

#### Winner Selection

winner = category with highest weighted votes  
agreement = winnerVotes / totalVotingPower

---

#### Latency Aggregation

Latency metrics (DNS, TCP, TLS, TTFB, TOTAL):

- Only from `Status.OK` reports
- Aggregated using **median** (resistant to outliers)

---

#### Faulty Validator Detection

Validators are marked faulty if:

- Category mismatch with consensus
- Latency deviates beyond threshold:

median ± deviationFactor (bps)

---

### Step 3: Global Aggregation

After regional execution:

- Global category determined from regional results
- Per-region latency preserved
- Validator correctness merged

Special handling:

- If validator behaves inconsistently across regions → marked faulty

---

## **7. Reputation System**

Each validator maintains a **reliability score (bps: 0–10,000)**.

### Formula:
```
reliability = correctReports * MAX_BPS / totalReports
```

### Enhancements:

- Initial reputation = **5000 (50%)**
- Used for:
    - Weighted consensus voting
    - Reward distribution
    - Slashing penalties

---

## **8. Redis Caching Layer**

To optimize consensus performance:

### ValidatorReputationCacheService

- Stores:
```
validator:rep:{id} -> { reliabilityBps }
```

### Key Optimization: **Pipelining**

Instead of multiple round trips:

- All GET operations sent in batch
- Redis executes them in a pipeline
- Results returned in a single response

This drastically reduces:

- Network overhead
- Latency during consensus

---

## **9. Round Execution Engine**

Execution is handled by **worker threads**:

- Threads continuously poll expired rounds from DelayQueue
- Execute consensus
- Emit result event

### Flow:

DelayQueue → Execution Worker → Consensus → Kafka Event

---

## **10. Kafka Event System**

After consensus:

- Event is published to topic:

round.execution

### Consumers:

#### 1. HealthAggregationService

- Persists platform health state
- Stores regional + global results

#### 2. ValidatorScoringService

- Updates validator statistics:
    - correctReports
    - totalReports
- Recalculates reputation
- Updates Redis cache

---

## **11. Idempotency & Round Tracking**

Each round is uniquely identified by:

(platformId, roundId)

Ensures:

- No duplicate processing
- Safe retries
- Consistency across services

---

## **12. Database Design (Health Module)**

Stores:

- Global round results
- Regional breakdown
- Latency metrics
- Validator correctness

Optimized for:

- Fast writes during ingestion
- Efficient reads for analytics

---

## **13. Scalability Considerations**

The system is designed to scale:

- Stateless workers
- Kafka-based async processing
- Redis for low-latency reads
- Modular architecture → microservice-ready

---

## **14. Future Improvements**

- TLS consensus mechanism
- Content validation consensus
- Rewarding mechanism
- Improved thread pools
