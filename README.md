# Stock App

A simplified stock market service. The bank holds shares of named stocks and wallets state. The user can buy and sell them.
Trades are atomic, audit log holds the information about the transactions. System also tolerates instance failure.

## Quick start

The only host requirement is **Docker** (Engine 20.10+ with the `compose` plugin).
The Java/Maven toolchain runs inside the build container, so no local JDK is needed.

#### Linux / macOS

```bash
./run.sh 8080
```

#### Windows (cmd.exe / PowerShell)

```cmd
run.cmd 8080
```

If you prefer to skip the launcher entirely, on any OS you can run:

```bash
PORT=8080 docker compose up --build
```

Replace `8080` with any port. The default if omitted is `8080`.
The API will be available at `http://localhost:<PORT>` once all services report
healthy (~10-15 seconds on first run; ~3 seconds on subsequent runs).

The Docker images are multi-arch (`linux/amd64` and `linux/arm64`), so the
same command works on Intel/AMD machines as well as Apple Silicon and ARM
Linux.


#### To stop:

```bash
docker compose down       # keep Postgres + Redis data
docker compose down -v    # also wipe Postgres + Redis data (true cold start)
```

> **State persistence.** Both Postgres (audit log) and Redis (bank/wallet state)
> are backed by named Docker volumes, so a plain `docker compose down` followed
> by `./run.sh` resumes exactly where you left off. To satisfy the spec line
> *"Initially there should be no wallets and bank account should be empty"*,
> use `docker compose down -v` for a clean slate.
### Overview what it does

- Bank - holds  the global pool of available stocks. Set with `POST /stocks`.
- Wallets - holds the various number of stocks. They are externally addressed accounts (w1, alice-wallet,...). Wallet exists once it has bought at least one share.
- Trades - single-share buys and sells. Each successful trade moves one share between bank and wallet.
- Audit log - append only every successful trade,
- Chaos - `POST /chaos` stops the receiving instance to demonstrate HA.

### API
All endpoints accept and return application/json unless noted.

`POST /stocks`

Replace the bank's stock holdings

```bash
curl -X POST http://localhost:8080/stocks \
    -H 'Content-Type: application/json' \
    -d '{"stocks":[{"name":"AAPL","quantity":100},{"name":"GOOG","quantity":50}]}'
```

`GET /stocks`

 Gets current bank holdings.
```bash
curl http://localhost:8080/stocks
# {"stocks":[{"name":"AAPL","quantity":100},{"name":"GOOG","quantity":50}]}
```

`POST /wallets/{wallet_id}/stocks/{stock_name}`

Buy or sell one share. 
Atomic: either the trade is fully completed on Redis and recorded in the audit log or no state changes.

```bash
curl -X POST http://localhost:8080/wallets/w1/stocks/AAPL \
    -H 'Content-Type: application/json' \
    -d '{"type":"buy"}'
```

The spec writes `{type: "sell|buy"}` in lowercase. Both casings are accepted
(`buy`/`BUY`/`Buy`) thanks to Jackson's `accept-case-insensitive-enums`, so
clients that follow the spec literally are not broken by our internal enum.

| Status          |                 Meaning                  |
|:----------------|:----------------------------------------:|
| 200 OK          |              Trade succeeded             |
| 400 Bad Request | Insufficient stock or malformed body     |
| 404 Not Found   | Stock not known to the bank              |

`GET /wallets/{wallet_id}`

Gets all stocks held by wallet.

```bash
curl http://localhost:8080/wallets/w1
# {"id":"w1","stocks":[{"name":"AAPL","quantity":3}]}
```

Returns `404 Not Found` if the wallet has never received a successful buy.

`GET /wallets/{wallet_id}/stocks/{stock_name}`

Returns the quantity of a single stock held by a wallet as a bare number.

```bash
curl http://localhost:8080/wallets/w1/stocks/AAPL
# 3
```

Returns `404 Not Found` if the stock is unknown to the bank or the wallet has
never received a successful buy. Returns `0` if the wallet exists but does not
currently hold the requested (known) stock.

`GET /log`

Return every successful trade, ordered by insertion.

```bash
curl http://localhost:8080/log
# {"log":[{"type":"buy","wallet_id":"w1","stock_name":"AAPL"},
#         {"type":"sell","wallet_id":"w1","stock_name":"AAPL"}]}
```

`POST /chaos`

Halt the receiving instance after returning `200`. Used to verify high availability.
```bash
curl -X POST http://localhost:8080/chaos
```
The container will be brought back up by Docker's `restart: unless-stopped` policy. While it's down, Nginx routes traffic to the surviving instance.

### Architecture

                    ┌─────────────────┐
                    │  Client         │
                    └────────┬────────┘
                             │ http://localhost:8080
                             ▼
                    ┌─────────────────┐
                    │  Nginx (LB)     │
                    │  passive HC     │
                    └────────┬────────┘
                             │
                ┌────────────┴────────────┐
                ▼                         ▼
        ┌─────────────┐           ┌─────────────┐
        │ stockapp-1  │           │ stockapp-2  │
        │ :8081       │           │ :8082       │
        │ (stateless) │           │ (stateless) │
        └──────┬──────┘           └──────┬──────┘
               │                         │
               └──────────┬──────────────┘
                          ▼
              ┌─────────────────────────┐
              │  Redis            (hot) │
              │  Postgres        (cold) │
              └─────────────────────────┘

### Why this shape

| Concern                         |                           Solution                           |
|:--------------------------------|:------------------------------------------------------------:|
| Atomic trades under concurrency | Redis Lua script — single-threaded execution on Redis server |
| Durable audit trail             |            Postgres, `BIGSERIAL` insertion order             |
| Schema versioning               |     Flyway migrations `src/main/resources/db/migration`      |
| Multi-instance HA               |     Stateless Spring app + shared Redis/Postgres + Nginx     |
| Crash recovery                  |      `restart: unless-stopped` + `proxy_next_upstream`       |
| State survives restarts         |  Postgres + Redis-AOF on named volumes (`pg_data`, `redis_data`) |
| Spec-required port flag         |       `run.sh` / `run.cmd` accept `$1`, default `8080`       |

### Tech stack
- Java 21 with virtual threads for request handling
- Spring Boot 3.5 (Web, Data Redis, Data JPA, Validation, Actuator)
- Maven wrapper for reproducible builds
- Redis 7 — bank stock counts, wallet stock counts, "known stocks" / "known wallets" sets
- PostgreSQL 16 — append-only audit_log table
- Flyway — schema migration on startup
- Nginx (alpine) — round-robin reverse proxy with passive health checks
- Docker Compose — orchestrates everything

### Project layout

```.
├── Dockerfile                   # multi-stage build, Java 21 + Alpine
├── start.sh                     # entrypoint accepting PORT argument
├── nginx.conf                   # LB config with passive health checks
├── docker-compose.yml           # multi-instance production-like setup
├── docker-compose.dev.yml       # Redis+Postgres only for local IDE dev
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/stockapp/
│   │   │   ├── controller/      # REST handlers
│   │   │   ├── service/         # business logic
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── model/           # JPA entities
│   │   │   ├── dto/             # request/response records
│   │   │   ├── exception/       # domain exceptions + global handler
│   │   │   └── config/          # Redis bean wiring
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── db/migration/    # Flyway scripts
│   │       └── scripts/         # Lua scripts (trade.lua)
│   └── test/
│       └── java/com/example/stockapp/
│           ├── controller/      # @WebMvcTest slice tests
│           ├── service/         # Mockito unit tests
│           ├── AbstractIntegrationTest.java   # Testcontainers base
│           ├── EndToEndIntegrationTest.java   # full HTTP→DB flow
│           └── ConcurrencyIntegrationTest.java  # Lua atomicity proof
```

### Testing 

To run the entire test suite:
```bash
./mvnw test
```

| Layer | Test class(es) | Count | What it proves                                                                                         |
| :--- | :--- | :--- |:-------------------------------------------------------------------------------------------------------|
| Controller slice | `*ControllerTest` | 20 | Routing, validation, JSON binding exception → status mapping                                           |
| Service unit (Mockito) | `*ServiceTest` | 19 | Business logic, error paths, correct delegation                                                        |
| Integration (Testcontainers) | `EndToEndIntegrationTest` | 6 | Full HTTP → Redis Lua → Postgres round-trip                                                            |
| Concurrency | `ConcurrencyIntegrationTest` | 1 | 100 parallel BUYs against 50 shares: exactly 50 succeed, audit log has exactly 50 rows, bank ends at 0 |
| Smoke | `StockAppApplicationTests` | 1 | Spring context loads against real Redis + Postgres                                                     |
| **Total** | | **47** |                                                                                                        |

The concurrency test is the most important one. It's the only thing that actually proves Lua atomicity holds under contention.

### Design decisions

#### Why Redis hashes plus a Lua script for trades 

The trade flow is a transactional move-one-share between two hashes (`bank:stocks` and `wallet:<id>:stocks`). A naive "GET-then-INCR" has a race: two concurrent buyers see the same count and both succeed. The Lua script runs atomically on the Redis server thread:

```Lua
local available = redis.call('HGET', KEYS[1], ARGV[1])
if not available or tonumber(available) < 1 then return 0 end
redis.call('HINCRBY', KEYS[1], ARGV[1], -1)
redis.call('HINCRBY', KEYS[2], ARGV[1], 1)
return 1
```

Returns `1` on success, `0` on insufficient stock. Spring's `StringRedisTemplate.execute(RedisScript, keys, args)` calls it via `EVALSHA` after the first invocation, so there's no per-trade script-transfer cost.

#### Why Postgres for the audit log 

Audit rows are append-only with a strict ordering requirement. A SQL database gives us:

- Durable persistence with WAL, decoupled from the hot Redis path.
- A `BIGSERIAL` ID that approximates commit order well enough for this scope.
- Standard tooling (psql) for incident investigation.

Flyway runs `V1__create_audit_log.sql` on startup, then JPA's `ddl-auto: validate` asserts that the schema matches the entity. If they ever drift, the app fails to start loudly.

#### Why separate JvmHalter bean

`POST /chaos` could call `Runtime.getRuntime().halt(1)` directly from the controller, but that's untestable — the test JVM would also die. Extracting the halt logic into a bean lets `ChaosControllerTest` swap in a mock and verify the endpoint contract without committing seppuku.

### Known limitations and trade-offs

This service prioritizes low-latency atomic trades and read scalability over strict cross-store consistency. Three trade-offs are worth calling out.

#### 1. Dual-write between Redis and Postgres 

Trades execute atomically in Redis via the Lua script, then an audit row is written to Postgres. If the Postgres write fails , the trade is durable in Redis but absent from the audit log.

In production this would be addressed with one of:

- A transactional outbox / two-phase commit-style protocol.
- Writing the audit row first as pending, then promoting to committed after the Redis Lua succeeds, with a background reaper for stuck rows.

For the scale of this assessment (~10K ops) the simpler dual-write was chosen for clarity.

#### 2. Audit log ordering

Audit rows are returned ordered by `BIGSERIAL` ID. Under concurrent writes, ID-assignment order can differ from transaction-commit order by a few rows.

For strict commit-order, `pg_xact_commit_timestamp` or logical-replication LSNs would be used. Neither was justified at this scope.

#### 3. Passive health checks on Nginx OSS 

Nginx OSS only supports passive health checks. A request must fail for an unhealthy backend to be marked out of the pool. During the chaos test, you may see a brief flicker of `502` while Nginx discovers the dead instance.

Active health checks require Nginx Plus, or a different LB like HAProxy or Traefik. For this scope the passive behaviour is sufficient and the chaos demo still works.

### What I would change for production

- Outbox pattern for the dual-write problem above.
- Authentication — every endpoint is currently open. Add JWT or mTLS for client identification.
- Rate limiting — Nginx-level `limit_req_zone` per source IP.
- Secrets management — Postgres credentials currently hardcoded in `docker-compose.yml`. Move to Docker secrets or a real secret store.


