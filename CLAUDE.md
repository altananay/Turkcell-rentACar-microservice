# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

# CLAUDE.md — Project Standards

> This file is read automatically by Claude Code at the start of every session.
> It is the single source of truth for how Claude should behave in this codebase.
> Keep it honest. Keep it short. Every line should earn its place.

---

## 1) Philosophy

1. **Write simple, unabstracted code.** Do not introduce patterns, interfaces, or boilerplate unless absolutely necessary to solve the immediate problem.
2. **Explicit over implicit.** Don't hide execution flow behind clever framework magic. Let the code read top-to-bottom like a script.
3. **No premature optimization.** Build the simplest thing that works. Don't build for "what if we need 10x scale" unless explicitly asked.
4. **Small diffs.** Change only what needs to be changed. Do not "clean up" adjacent code or switch syntax to your preference unless it's the requested feature.
5. **Readability is king.** Write code a junior engineer can read linearly and understand immediately.
6. **Focus on the data.** Understand the data structures and transformations first. The logic should naturally follow.
7. **Light dependencies.** Before adding a package, ask: "Can we write this in ~20 lines of standard library code?" If yes, write the code.

---

## 2) Repository Map

**10 independent Maven modules. There is no root `pom.xml` and no reactor — never run `mvn` from the repo root.** Each module inherits `spring-boot-starter-parent` directly and has its own `mvnw` wrapper.

| Module | Boot parent | Role | Local port | Datastore | Uses `common-package` |
|---|---|---|---|---|---|
| `config-server` | 3.0.6 | `@EnableConfigServer`, git-backed from `github.com/altananay/config-server`, `default-label: main`, profile `dev` | 8888 (declared) | — | no |
| `discovery-server` | 3.0.6 | `@EnableEurekaServer` | 8761 (Eureka default, not declared) | — | no |
| `api-gateway` | 3.0.6 | Spring Cloud Gateway (WebFlux), `discovery.locator.enabled` + `lower-case-service-id`, no explicit routes | 9010 (declared) | — | **no — see §4** |
| `common-package` | 3.0.6 | Shared library `com.kodlamaio:common-package:0.0.1-SNAPSHOT` | — | — | is |
| `inventory-service` | 3.0.6 | Brands / models / cars; Kafka producer | external config | MySQL 3307 | yes |
| `rental-service` | 3.0.6 | Rentals; Feign → inventory + payment | external config | PostgreSQL 5434 | yes |
| `maintenance-service` | **3.1.0** | Maintenances; Feign → inventory | external config | PostgreSQL 5431 | yes |
| `payment-service` | **3.1.0** | Payments + `FakePosServiceAdapter` | external config | PostgreSQL 5433 | yes |
| `invoice-service` | 3.0.6 | Invoices; Kafka-only writes | external config | MongoDB 27018 | yes |
| `filter-service` | 3.0.6 | CQRS read model; Kafka-only writes | external config | MongoDB 27017 | yes |

Java 17 everywhere. Spring Cloud `2022.0.2`. Maven wrapper versions differ across modules (3.8.7 / 3.9.1 / 3.9.2). The 6 business services do **not** declare `server.port` locally — their ports come from the external config repo.

---

## 3) Architecture & Package Layout

**Microservice architecture.** Each business service is independently deployable, owns its own datastore, and is internally layered: `api → business → repository → entities`. Services communicate only over Kafka events or Feign calls — never by sharing a database or importing each other's code.

Canonical skeleton per business service:

```
<service>/src/main/java/<base-package>/
  api/controllers/*Controller.java        @RestController, delegates to the *Service interface
  api/clients/*Client.java                @FeignClient          (rental-service, maintenance-service only)
  api/clients/*ClientFallback.java        @Component
  business/abstracts/*Service.java        interface
  business/concretes/*Manager.java        @Service, implements the interface
  business/rules/*BusinessRules.java      @Service, throws BusinessException
  business/dto/requests/  business/dto/responses/
  business/kafka/consumer/*Consumer.java  @KafkaListener        (note: singular "consumer")
  entities/*.java
  repository/*Repository.java             JpaRepository | MongoRepository
```

Bootstrap for every business service:

```java
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {Paths.ConfigurationBasePackage, Paths.<X>.ServiceBasePackage})
```

`@EnableFeignClients` is additionally on rental, maintenance and payment (payment declares it but has no client).

### Deviations — existing precedent, match it, do not "fix" it

- Only `inventory-service` nests DTOs: `dto/requests/{create,update}` and `dto/responses/{create,get,update}`. Every other service uses flat `dto/requests` + `dto/responses`.
- `payment-service` uses `entity` (singular) and adds `adapters/FakePosServiceAdapter` implementing `business/abstracts/PosService`.
- `filter-service` base package is `kodlamaio.filterservice` — **no `com.` prefix**; its pom `groupId` is `kodlamaio`; it has no `business/rules` package and only response DTOs.
- `rental-service` additionally has `business/saga/` (`RentalCreationSagaOrchestrator`, `SagaRecoveryScheduler`) — a deliberate deviation from the skeleton above, since a persisted state machine coordinating a `*Manager`, a Feign client, and Kafka publishing isn't a `*Manager` or `*BusinessRules`. See §12 for the saga itself.
- `rental-service/api/clients/` additionally holds `PaymentClientTokenConfiguration` — a Feign configuration class, not a `*Client`/`*ClientFallback`. It **deliberately carries no class-level annotation** (see §11 and §16); it is wired only by `@FeignClient(configuration = ...)` on `PaymentClient`.
- The 3 event-producing JPA services (`inventory-service`, `maintenance-service`, `rental-service`) each have `business/outbox/` (`OutboxRecorder`, `OutboxRelay`) plus `entities/OutboxMessage` and `repository/OutboxMessageRepository` — same placement convention as the saga (entity and repository in the standard packages, behaviour in its own `business/` sub-package). The four files are **near-identical across the three services on purpose**: `common-package` has no JPA and cannot get any (see §4), and an outbox table is part of a service's own schema rather than a shared contract. See §12.

---

## 4) Module Boundaries

1. `common-package` is the leaf. It imports no service package. Never add one.
2. Every business service depends on `common-package`. `config-server`, `discovery-server` and `api-gateway` must **not** — `common-package` pulls `spring-boot-starter-web` (MVC), which would flip the WebFlux gateway to a servlet stack and break Spring Cloud Gateway.
3. Services never import one another. Cross-service data crosses only via `commonpackage.utils.dto.*` (Feign) or `commonpackage.events.*` (Kafka).
4. A type shared by two services goes in `common-package`, never duplicated. Adding one requires `cd common-package && ./mvnw install` before the consumer compiles. **One deliberate exception, do not "fix" it:** the outbox quartet (`OutboxMessage`, `OutboxMessageRepository`, `OutboxRecorder`, `OutboxRelay`) is duplicated in the 3 JPA services. `common-package` declares no JPA, and adding `spring-boot-starter-data-jpa` there would activate `DataSourceAutoConfiguration` in `filter-service` and `invoice-service`, which are MongoDB-only and have no datasource — both would fail at startup.
5. Inside a service: the controller depends on `business/abstracts/*Service`, never on `*Manager` directly. A `*Manager` owns exactly one repository. A `*BusinessRules` throws only `BusinessException`.
6. Feign clients live in `api/clients` and are invoked from `business/rules` (precedent: `RentalBusinessRules.ensureCarIsAvailable`) or `business/concretes`/`business/saga` (precedent: `RentalCreationSagaOrchestrator` calling `PaymentClient`/`CarClient`) — never from a controller.
7. Kafka consumers live in `business/kafka/consumer` and delegate to the `*Service` interface — no direct repository access.
8. Only `common-package` declares infrastructure dependencies. A service pom adds only its persistence starter + driver + `common-package` + test.
9. Nothing `@Component`-scanned may live outside the two `scanBasePackages` roots.

---

## 5) Commands

```bash
# JDK 17 is MANDATORY. The system default here is JDK 25 and Lombok annotation
# processing fails on it with "cannot find symbol: variable log".
export JAVA_HOME="/c/Users/altan/.jdks/corretto-17.0.17"   # PowerShell: $env:JAVA_HOME = "..."

# 1) Shared library FIRST — all 6 business services resolve it from the local Maven repo
cd common-package && ./mvnw install                        # Windows: .\mvnw.cmd install

# 2) Backing infrastructure only. No Spring Boot app is containerized; no Dockerfile exists.
docker-compose up -d
#   kafka 9092 · kafka-ui 8090 (Kafbat UI, browses topics/messages/consumer groups)
#   keycloak 8081 · zipkin 9411 · prometheus 9090 · grafana 3000
#   mongo 27017 (filter) · mongo 27018 (invoice) · mysql 3307 (inventory)
#   postgres 5431 (maintenance) · 5433 (payment) · 5434 (rental)

# 3) Startup order matters
#    config-server (8888) -> discovery-server (8761) -> business services -> api-gateway (9010)
cd config-server     && ./mvnw spring-boot:run
cd discovery-server  && ./mvnw spring-boot:run
cd inventory-service && ./mvnw spring-boot:run             # ... and the other 5
cd api-gateway       && ./mvnw spring-boot:run

# Per module — there is no reactor, so every command runs from inside a module directory
cd <module> && ./mvnw test
cd <module> && ./mvnw package -DskipTests
cd <module> && ./mvnw -q dependency:tree
```

**Warnings:**

- **Turkish-locale JVM breaks gateway routing.** Under `tr-TR`, `lower-case-service-id: true` turns `INVENTORY-SERVICE` into `ınventory-servıce` (dotless `ı`) and every route 404s. Run `api-gateway` with `-Duser.language=en -Duser.country=US`.
- **Do not remove `<classifier>exec</classifier>`** from `common-package/pom.xml`'s `spring-boot-maven-plugin`. It keeps the plain library jar under the default coordinates so the 6 business services can resolve it.
- **All requests through the gateway are `/{service-id-lowercase}/...`** — e.g. `GET http://localhost:9010/inventory-service/api/cars`. There are no explicit route definitions.
- **`kafka` has dual listeners.** `PLAINTEXT://localhost:9092` for the 10 host-run Spring apps; `INTERNAL://kafka:29092` for other containers (e.g. `kafka-ui`) reaching it over the Docker network by service name. Point any new containerized Kafka client at `kafka:29092`, not `localhost:9092`.
- **`kafka`'s data volume must mount `/tmp/kraft-combined-logs`**, not `/opt/kafka/kafka-logs` — the `bashj79/kafka-kraft` image's actual log dir is the former regardless of what the folder naming implies. The wrong path silently no-ops the volume; the container looks persistent until `--force-recreate`, at which point all topics/offsets vanish with no error. Harmless here (Kafka is pure event transport, not a system of record — real state lives in each service's own DB), but confusing if unexpected.

---

## 6) Configuration & Secrets

- Each business service's local `application.yml` is a 6-line stub: `spring.application.name`, `spring.cloud.config.profile: dev`, `spring.config.import: optional:configserver:http://localhost:8888`. **Never add datasource / kafka / eureka / keycloak keys to it.**
- Real config (`<service>-dev.yml`, `<service>-prod.yml`) lives in `https://github.com/altananay/config-server`, served by `config-server` with `default-label: main`.
- `api-gateway` is the exception: it has a fat local yml and does **not** import config-server.
- The `optional:` prefix means a service starts silently on defaults when config-server is down. A missing datasource is usually a stopped config-server, not a code bug.
- If a config key is needed and isn't local, say so — do not invent it.
- There is no `.env` file. Every backing-service credential is written directly in `docker-compose.yml`, which is committed. These are throwaway local-dev credentials for containers that are never exposed beyond `localhost` — do not put anything real there, and do not introduce a credential that would matter if leaked.
- Note the two-layer split: `docker-compose.yml` credentials **create** the database containers; the connection settings the Spring services actually use come from the external config repo. If you change a Postgres user or database name in compose, the matching `<service>-dev.yml` in the config repo has to change too, or the service will fail to connect.

---

## 7) Non-Negotiables

If any item below is violated, the change is invalid.

1. No secrets in code, tests, logs, or docs. `docker-compose.yml` carries throwaway local-dev credentials only — never add one that would matter if leaked.
2. No unvalidated external input reaches business logic.
3. No breaking API change without explicit notes — and no change to an event class in `common-package/events` without checking every `@KafkaListener` that deserializes it.
4. No violation of the §4 module boundaries.
5. No TODO placeholders in shipped code.
6. No silent test skips.
7. No untyped escape hatches without a justification comment.
8. No dependency additions without rationale.

---

## 8) Working Agreement

### Before changing code

- Read nearby code and follow local patterns.
- Identify constraints from existing tests, types, and interfaces.
- Prefer editing existing modules over creating new ones.
- If multiple plausible implementations exist, pick the one that matches repository precedent.

### While changing code

- Keep edits minimal and localized.
- Keep public interfaces stable unless explicitly requested.
- Handle failure paths as first-class behavior.
- Add short intent comments only where logic is genuinely non-obvious. Comment the *why*, not the *what*.

### After changing code

- Run the module's tests. If `common-package` changed, reinstall it before testing consumers.
- Fix all regressions introduced by the change.
- Verify error messages and logs are actionable.
- Ensure commit-ready state: no debug leftovers, no dead code.

---

## 9) Code Style & Naming

### Formatting

- Standard Java conventions (4-space indentation, braces on same line for methods/classes).
- Max line length: 120 characters.
- Match the surrounding code. Existing controllers use `import org.springframework.web.bind.annotation.*;` — match the file you are in rather than reorganising its imports.

### Naming

- **Service triple**: interface `<Feature>Service` in `business/abstracts`, implementation `<Feature>Manager` in `business/concretes`, rules `<Feature>BusinessRules` in `business/rules`.
- **DTOs**: `Create*Request` / `Update*Request`; `Create*Response` / `Get*Response` / `GetAll*Response` / `Update*Response`. Mutable Lombok POJOs (`@Getter @Setter @AllArgsConstructor @NoArgsConstructor`) — **not** records.
- **Events**: `<Thing><Verb>edEvent` in `commonpackage/events/<domain>/`, implementing the `Event` marker interface.
- **Message keys**: UPPER_SNAKE_CASE string constants in `commonpackage.utils.constants.Messages.<Domain>`.
- **Base packages**: `com.kodlamaio.<x>service` — except `kodlamaio.filterservice`.
- **Booleans**: prefix with `is` / `has` / `can`.
- Use clear, descriptive names. `calculatePriceWithDiscount` beats `process`.

### Injection

Constructor injection via Lombok `@AllArgsConstructor` / `@RequiredArgsConstructor` on `final` fields. No field `@Autowired`.

### Null checks

Never write `x == null` / `x != null` directly. Use `java.util.Objects.isNull(x)` / `Objects.nonNull(x)` for a guard clause, or `Optional.ofNullable(x)` when the value flows into a chain (`.orElseThrow(...)`, `.map(...)`, `.ifPresentOrElse(...)`). Prefer `Optional` when it replaces a "fetch nullable value, then branch" shape (e.g. a repository lookup feeding a not-found exception); use `Objects.isNull`/`nonNull` for a simple early-return guard where wrapping in `Optional` would add ceremony without improving readability.

### Module-qualify colliding class names

Several class names exist in more than one module. Always write them as `<module>/<ClassName>` in prose, commits, and reports:

- `RentalConsumer` — inventory-service, filter-service, invoice-service
- `MaintenanceConsumer` — inventory-service, filter-service
- `CarClient`, `CarClientFallback` — rental-service, maintenance-service

### Logging

- Structured logs only. Include correlation/request IDs where available (Zipkin/Brave tracing is on the classpath).
- Never log credentials, tokens, or PII.

---

## 10) Error Handling

**A global handler exists**: `common-package/src/main/java/com/kodlamaio/commonpackage/configuration/exceptions/RestExceptionHandler.java` — a `@RestControllerAdvice` active in every service via `scanBasePackages`. The response body is `ExceptionResult` = `{timestamp, type, message}`.

| Exception | Status | `type` |
|---|---|---|
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | `VALIDATION_EXCEPTION` (message is a `Map<field, msg>`) |
| `jakarta.validation.ValidationException` | 422 UNPROCESSABLE_ENTITY | `VALIDATION_EXCEPTION` |
| `BusinessException` | 422 UNPROCESSABLE_ENTITY | `BUSINESS_EXCEPTION` |
| `DataIntegrityViolationException` | 409 CONFLICT | `DATA_INTEGRITY_VIOLATION_EXCEPTION` |
| `RuntimeException` | 500 INTERNAL_SERVER_ERROR | `RUNTIME_EXCEPTION` |

Rules:

- Domain failures throw `BusinessException(Messages.X.Y)` from a `*BusinessRules` class. Never invent a new exception type without adding a handler for it.
- Bare `orElseThrow()` raises `NoSuchElementException`, which has **no handler** and falls through to 500. Guard with a `checkIfXExists` rule first — that is the existing precedent.
- Fallback classes: `rental-service/CarClientFallback` and `maintenance-service/CarClientFallback` both throw `BusinessException` (→ 422). Match this for any new fallback.
- Log at `WARN` for expected failures, `ERROR` for unexpected ones. Don't log and rethrow.

---

## 11) Security & Auth

**Keycloak OIDC resource server. There is no jwt, no `JwtAuthenticationFilter`, and no homegrown token code.**

- Realm `RentACarMicroservice` at `localhost:8081` (the container maps host 8081 → container 8080). Realm roles: `user`, `admin`, `service`. Test user `altananay` / `12345` has `user` + `admin`; client `gateway-client` (public, direct access grants enabled) issues tokens via the password grant.
- **`service` is a machine-only role.** It belongs to the service account of the confidential client `rental-service-client` (Client authentication ON, Service accounts roles ON, standard flow and direct access grants OFF), never to a human. It exists so `/api/payments/process-rental-payment` and `/api/payments/refund-rental-payment` are callable *only* by rental-service and not by any logged-in user's token.
- **`keycloak`'s data now persists** via the `keycloak_data:/opt/keycloak/data` volume (added after a container restart wiped the realm — Keycloak's `start-dev` H2 database lives in the container's writable layer by default, gone on any recreate unless that path is mounted). If the realm ever goes missing again despite the volume, check `docker volume ls` for `turkcell-rentacar-microservice_keycloak_data` and whether the container is actually mounting it.
- One shared filter chain for every service: `common-package/src/main/java/com/kodlamaio/commonpackage/security/SecurityConfig.java`, annotated `@EnableMethodSecurity(securedEnabled = true)`. The `jwk-set-uri` comes from the external config repo.
- `utils/security/KeycloakJwtRoleConverter` maps the `realm_access.roles` claim to `ROLE_<role>` authorities.
- Matcher order — **first match wins**:
  1. `permitAll()`: `/api/filters`, `/api/cars/check-car-available/**`, `/api/payments/check`, `/api/cars`, `/api/cars/**`, `/actuator/**`
  2. `/api/payments/process-rental-payment`, `/api/payments/refund-rental-payment` → `hasRole("service")`
  3. `/api/**` → `hasAnyRole("user")`
  4. `anyRequest().authenticated()`
  `cors()` is enabled, `csrf()` is disabled.

### Outbound: how rental-service authenticates itself

**`rental-service` obtains its own Keycloak token via the `client_credentials` grant** — it does *not* forward the caller's header. `api/clients/PaymentClientTokenConfiguration` declares an `AuthorizedClientServiceOAuth2AuthorizedClientManager` plus a Feign `RequestInterceptor` that sets `Authorization: Bearer <service token>`.

Three things about it are load-bearing and easy to undo by accident:

- **The manager must be `AuthorizedClientService…`, not `Default…`.** `DefaultOAuth2AuthorizedClientManager` resolves the token from the current servlet request. `SagaRecoveryScheduler` and the charge/refund calls it drives run on a `@Scheduled` thread with no request, so the Default implementation would leave crash recovery and automatic refund broken — the exact scenarios the saga exists for. Same reason the principal is the constant `"rental-service"` and never `SecurityContextHolder`'s.
- **The configuration class must stay unannotated.** A `@Configuration`/`@Component` on it registers the interceptor in the parent context, from which *every* Feign client inherits it — including `CarClient`, whose inventory endpoints are `permitAll` and work today with Keycloak stopped. It would silently make Keycloak an availability dependency of `POST /api/rentals`. `PaymentClientTokenConfigurationTest` guards this.
- **Only `PaymentClient` gets the token.** `CarClient` (both here and in maintenance-service) sends none, by design.

Config lives in the external repo (`rental-service-dev.yml`) under `spring.security.oauth2.client.registration.keycloak` / `provider.keycloak`. `spring-boot-starter-oauth2-client` is declared in `rental-service/pom.xml` only — not `common-package`, since no other service needs it.
- Consequence to keep in mind: because `/api/cars/**` is `permitAll`, the `@Secured("ROLE_admin")` on `inventory-service/CarsController#getAll` — the only method-level security annotation in the repo — is the sole gate on that path.
- `/api/payments/check` is in the permitAll list but **no controller declares it**. Dead rule.
- **The gateway does not authenticate.** Every service validates its own token.
- Mandatory: validate all inputs at boundaries, authorize every protected operation, parameterize all queries, never log credentials or PII.

---

## 12) Inter-Service Communication

### Kafka (async, choreography)

7 topics. **Topic names are hardcoded string literals at every call site — there is no topic constants class.** The producer is `commonpackage.kafka.producer.KafkaProducer`, which has two methods:

- `sendMessage(T extends Event, String topic)` — fire-and-forget, discards the send future. A broker failure is never observed. **No business code calls this any more**; it is only still reachable for a caller that genuinely wants no delivery guarantee.
- `sendMessageAndWait(T extends Event, String topic)` — blocks on the future (10s) and throws `IllegalStateException` if the broker does not acknowledge. **Only `OutboxRelay` calls this.**

**No `*Manager` publishes to Kafka directly.** Every event goes through the outbox (see below).

| Topic | Producer | Consumers (`groupId`) |
|---|---|---|
| `car-created` | inventory-service `CarManager` | filter-service `InventoryConsumer` (`car-create`) |
| `car-deleted` | inventory-service `CarManager` | filter-service `InventoryConsumer` (`car-delete`) |
| `brand-deleted` | inventory-service `BrandManager` | filter-service `InventoryConsumer` (`brand-delete`) |
| `rental-created` | rental-service `RentalManager` | inventory-service `RentalConsumer` (`inventory-rental-create`), filter-service `RentalConsumer` (`filter-rental-create`) |
| `rental-deleted` | rental-service `RentalManager` | inventory-service `RentalConsumer` (`inventory-rental-delete`), filter-service `RentalConsumer` (`filter-rental-delete`) |
| `maintenance-created` | maintenance-service `MaintenanceManager` | inventory-service + filter-service `MaintenanceConsumer` |
| `maintenance-deleted` | maintenance-service `MaintenanceManager` | inventory-service + filter-service `MaintenanceConsumer` |
| `rental-payment-created` | rental-service `RentalManager` | invoice-service `RentalConsumer` (`rental-payment-create`) |

The Producer column above names the class that *records* the event; the actual Kafka publish always happens in that service's `OutboxRelay`.

Adding a topic requires: the event class in `common-package/events/<domain>/`, an `outboxRecorder.record(event, "topic")` call inside the producing transaction, **an entry in that service's `OutboxRelay.TOPIC_TYPES` map** (forget this and the row is never published — it logs an ERROR every 5s), a `@KafkaListener(topics = …, groupId = …)`, and a `common-package` reinstall.

### Transactional outbox (inventory-service, maintenance-service, rental-service)

Fixes the audit's finding #6: `repository.save(...)` and `producer.sendMessage(...)` used to be two independent non-transactional steps, so a crash between them — or simply a Kafka outage, which the fire-and-forget send never even detected — left the datastore and the event stream permanently inconsistent.

```
business method
  └─ transactionTemplate: entity write + outboxRecorder.record(event, topic)   [one local tx, commits together]

OutboxRelay  @Scheduled(fixedDelay = 5000)
  └─ findTop100ByPublishedFalseOrderByCreatedAtAsc
       └─ resolve class from TOPIC_TYPES -> readValue -> sendMessageAndWait -> mark published
```

- **`OutboxMessage` carries the topic, not the event class name.** The relay maps topic → class via a static `TOPIC_TYPES` map, so renaming or moving an event class is a compile error instead of a `ClassNotFoundException` on rows written months earlier.
- **A publish failure abandons the whole batch** (`return`, not `continue`) — the broker being unreachable means the rest would fail too, and each blocking send would otherwise monopolise the single shared `@Scheduled` thread, starving `SagaRecoveryScheduler`. A deserialize failure is the opposite: log ERROR and `continue`. Consequence to know: a genuinely poison row blocks everything behind it, loudly, with a WARN naming its id every 5s.
- **Delivery is at-least-once.** `@Version` prevents a double *mark*, not a double *send* — the claim happens after the publish. This is why consumer-side dedup exists (see §13).
- **The two delete flows read inside the transaction.** `MaintenanceManager.delete` and `RentalManager.delete` load the entity in-tx and call `repository.delete(entity)`, never `deleteById`. `deleteById` silently no-ops on an already-deleted row, so a pre-transaction read would let a concurrent delete commit an outbox row describing a deletion that never happened — and the outbox would then deliver it reliably. `CarManager`/`BrandManager` don't need this: their event payload is only the id.
- **Latency:** events now reach consumers up to 5 seconds after the HTTP response. `POST /api/cars` followed immediately by `GET /api/filters` will not show the car. Expected, not a bug.
- Published rows are never purged; the table grows unbounded. Acceptable for local dev, noted so nobody is surprised.

### OpenFeign (sync)

| Client | Target | Endpoint | Resilience | Fallback throws |
|---|---|---|---|---|
| rental-service `CarClient` | inventory-service | `GET /api/cars/check-car-available/{carId}`, `GET /api/cars/get-car-for-invoice/{carId}` | `@Retry("rentalToInventory")` on `checkIfCarAvailable` only | `BusinessException` → 422 |
| rental-service `PaymentClient` | payment-service | `POST /api/payments/process-rental-payment`, `POST /api/payments/refund-rental-payment` — both take a leading `Idempotency-Key` header, and both carry a `client_credentials` service-account bearer token added by `PaymentClientTokenConfiguration` (see §11) | none | `BusinessException` → 422 |
| maintenance-service `CarClient` | inventory-service | `GET /api/cars/check-car-available/{carId}` | `@Retry("maintenanceToInventory")` | `BusinessException` → 422 |

Resilience4j is on the classpath via `common-package`, but there are **zero `@CircuitBreaker` annotations** and only the 2 `@Retry` above. All resilience4j YAML lives in the external config repo.

### `POST /api/rentals` — orchestration-based saga

`RentalManager.add` no longer talks to `PaymentClient`/`CarClient` directly. It calls `rules.ensureCarIsAvailable(carId)` (read-only precondition), then delegates the whole rental-creation flow to `business/saga/RentalCreationSagaOrchestrator`, which persists a `RentalCreationSaga` row (`rental_creation_sagas` table, own Postgres) as the state machine and drives it through:

```
STARTED --charge succeeds--> PAYMENT_CHARGED --rental saved + saga COMPLETED--> COMPLETED
   |                              |
   +--charge fails--> PAYMENT_FAILED
                                  +--anything after charge fails--> COMPENSATING --refund ok--> COMPENSATED
                                                                          +--refund fails--> COMPENSATION_FAILED
```

- The saga row is self-contained (full card tuple + `carId`/`dailyPrice`/`rentedForDays`/`price`) so a crash mid-flow can resume from persisted state alone — the original in-memory request is gone.
- `saga.getId()` is sent as the `Idempotency-Key` header on both `PaymentClient` calls. `payment-service` records every charge/refund in `processed_payment_operations` (unique on `idempotencyKey` + `operationType`) and replays the stored result on a repeat, so a retried charge or refund never double-executes.
- Rental-row-save + saga-status-advance is one local `TransactionTemplate` transaction in rental-service; balance-mutate + idempotency-record-save is one local `TransactionTemplate` transaction in payment-service. **No transaction spans both services** — cross-service consistency is the saga's persisted state + compensation, never a transaction manager.
- `business/saga/SagaRecoveryScheduler` (`@Scheduled(fixedDelay = 30000)`, needs `@EnableScheduling` on `RentalServiceApplication`) re-drives any saga stuck in `STARTED`/`PAYMENT_CHARGED`/`COMPENSATING` for >60s — this is what makes payment-before-save recoverable after a crash instead of silently leaking a charge.
- `rental-created` and `rental-payment-created` are **recorded into the outbox inside that same transaction**, so "rental committed but events lost" is unreachable. The old best-effort post-commit publish (and the invariant it had to protect) is gone: anything failing in the transaction now rolls back the rental too, which makes routing to `compensate` correct rather than a bug. It also closes a hole the saga alone had — the idempotent-replay branch returns early without publishing, so a crash between commit and publish used to lose the events permanently.
- Scope boundary: this saga guarantees payment ⇄ rental-row consistency. Rental-row ⇄ event-stream consistency is the outbox's job (see §12).
- `RentalCreationSaga`/`ProcessedPaymentOperation` both use `@Version` optimistic locking — a losing concurrent writer (a live request racing the recovery scheduler) gets `OptimisticLockingFailureException`, caught and treated as "someone else already handled this."

---

## 13) Data & Persistence

**Database-per-service.** Schema is created and evolved by JPA `ddl-auto`, configured in the external config repo. This repository contains no SQL files and no schema-versioning tooling.

| Service | Engine | Host port | Repository base | Notes |
|---|---|---|---|---|
| inventory-service | MySQL | 3307 | `JpaRepository` | The only service with JPA relationships: `Brand` 1-N `Model` 1-N `Car`; `Car.state` is `@Enumerated(EnumType.STRING) State` |
| rental-service | PostgreSQL | 5434 | `JpaRepository` | |
| maintenance-service | PostgreSQL | 5431 | `JpaRepository` | |
| payment-service | PostgreSQL | 5433 | `JpaRepository` | entity in the `entity` (singular) package |
| invoice-service | MongoDB | 27018 | `MongoRepository` | `@Document`, `@Id String id` |
| filter-service | MongoDB | 27017 | `MongoRepository` | `@Document`, `@Id String id` |

- The 3 event-producing JPA services each own an `outbox_messages` table (`inventory-service`, `maintenance-service`, `rental-service`). Created by `ddl-auto: update` like everything else — **if that ever becomes `create-drop`, unpublished rows are wiped on every restart and the outbox silently stops guaranteeing anything.**
- Every non-inventory service stores `UUID carId` as a plain column/field with **no foreign key**. Cross-service references are denormalized by design — never add an FK across services.
- **Consumer-side dedup** (required because outbox delivery is at-least-once, see §12): `FilterManager.add` reuses an existing document's id when the incoming `Filter` has none, turning the `car-created` insert into an upsert keyed on `carId`; `InvoiceManager.add` does the same keyed on `Invoice.rentalId`, guarded by `Objects.nonNull` because Mongo matches a null `rentalId` against every invoice written before that field existed. Both use `findFirst…` finders, never single-result ones — duplicates already in the local collections would otherwise throw `IncorrectResultSizeDataAccessException`. There is **no unique index** on either collection: `@Indexed(unique = true)` is a no-op unless `spring.data.mongodb.auto-index-creation=true` (external config, Boot 3 default is `false`), and enabling it with duplicates present stops the service booting.
- `filter-service` is a Kafka-fed CQRS read model. Never write to it over HTTP.
- Its `Filter.state` is a free-text `String` (`"Rented"` / `"Maintenance"` / `"Available"`) duplicating inventory's `State` enum. Keep the two in sync when either changes.

---

## 14) Testing

**Current state:** ~244 unit tests across `common-package` and all 6 business services, covering `*Manager`, `*BusinessRules`, `*Controller`, `*Consumer`, `*ClientFallback`, `business/outbox/*` (the 3 JPA services), and (rental-service only) `business/saga/*` and `PaymentClientTokenConfiguration`. All are plain Mockito/AssertJ unit tests or `MockMvcBuilders.standaloneSetup` controller tests — no `@SpringBootTest`, no Spring context, no config-server or database dependency. They pass with all backing infrastructure stopped. The old default `*ApplicationTests.java` `contextLoads()` stubs (one per module, Spring Initializr boilerplate) have been deleted — they required a live config-server/Keycloak to load the context and asserted nothing. `spring-security-test` is not declared anywhere; controller tests that need `@Valid` enforcement use `standaloneSetup`, which wires Bean Validation automatically without a security context.

**Mockito pitfall hit while testing the saga (worth remembering):** re-stubbing a mock method with `when(mock.method(any())).thenX(...)` a second time, when the method's *first* stub was a side-effecting `thenAnswer(...)`, re-invokes that first Answer as a side effect of setting up the second stub — because `when(...)` has to actually call the mock method to record it, and `any()` evaluates to `null` at that call site. If the Answer dereferences its argument (e.g. `((Consumer) inv.getArgument(0)).accept(...)`), this NPEs during test setup, not during the real assertion. Fix: use `doThrow(...).when(mock).method(any())` (or `doAnswer`/`doReturn`) for the *second* stub — that syntax records the stub without re-triggering the currently active one.

### Standards

- New logic requires tests. Bug fixes require a regression test.
- Test file naming: `FooManagerTest.java`, `FooControllerTest.java`.
- Test names read as sentences.

### What to test by layer

| Layer | What to test | What to mock |
|---|---|---|
| `business/concretes/*Manager` | Orchestration, mapping, event emission | Repository, `ModelMapperService`, `KafkaProducer`, `*BusinessRules` via `@Mock` |
| `business/rules/*BusinessRules` | `BusinessException` paths and messages | Repository + Feign client |
| `api/controllers/*Controller` | Status codes, `@Valid` rejection, `@Secured` | The `*Service` interface via `@MockBean`; use `@WebMvcTest` |
| `api/clients/*ClientFallback` | That it throws the documented exception type | — |
| `business/kafka/consumer/*Consumer` | Event maps to the right service call | The `*Service` interface |
| `business/saga/RentalCreationSagaOrchestrator` | Every status transition, compensation, idempotent replay, optimistic-lock skip, post-commit Kafka isolation | Both repositories, `PaymentClient`, `CarClient`, `KafkaProducer`, `TransactionTemplate` (stub `executeWithoutResult`/`execute` to actually run the lambda) |
| `business/saga/SagaRecoveryScheduler` | Correct recoverable-status query, one saga's failure doesn't block the batch | `RentalCreationSagaRepository`, `RentalCreationSagaOrchestrator` |
| `business/outbox/OutboxRecorder` | Topic + payload stored unpublished, payload round-trips back to an equal event, serialization failure throws | `OutboxMessageRepository`; use a **real** `Jackson2ObjectMapperBuilder.json().build()`, not a mock — serialization is the behaviour under test |
| `business/outbox/OutboxRelay` | Blocking send is used, mark-published on success, deserialize failure skips one row, publish failure abandons the batch, optimistic-lock skip, empty batch | `OutboxMessageRepository`, `KafkaProducer`; `@Spy` the real `ObjectMapper` |

**Atomicity tests.** Every manager whose write is now wrapped in a `TransactionTemplate` has one dedicated test proving the entity write and the outbox record happen *inside* the same transaction: override the shared stub so the lambda does **not** run, call the method, assert `verifyNoInteractions(repository, outboxRecorder)`, then capture the `Consumer<TransactionStatus>` (or `TransactionCallback<T>` for `CarManager.add`) and run it manually, asserting both writes happened. This is the strongest atomicity assertion available without a real database, and it is the reason to keep `TransactionTemplate` rather than `@Transactional` — the annotation is inert under Mockito.

### Anti-patterns

- No snapshot-only confidence for complex behavior.
- No assertions tied to implementation details when behavior-level assertions suffice.
- Avoid labyrinthine mocking. If something is hard to mock, the code is probably too coupled.
- Don't chase coverage with boilerplate tests that assert nothing.

### What not to test

Framework boilerplate, simple getters/setters, third-party internals.

---

## 15) Dependencies

`common-package/pom.xml` is the dependency hub — adding a dependency there adds it to all 6 business services. Add there only if two or more services need it; otherwise add it to the single service's pom.

Before adding a package: confirm no existing dependency solves it, check maintenance activity and license, record a one-line rationale.

Approved defaults (all declared in `common-package/pom.xml`):

- HTTP/REST: Spring MVC (`spring-boot-starter-web`)
- Validation: Bean Validation on `*Request` DTOs
- Persistence: Spring Data JPA / Spring Data MongoDB (per service) + driver
- Security: Spring Security OAuth2 Resource Server. **`spring-boot-starter-oauth2-client` is declared in `rental-service/pom.xml` only** — it is what obtains the `client_credentials` service-account token for the payment calls (§11); no other service makes an authenticated outbound call.
- Platform: Spring Cloud `2022.0.2` — config client, Eureka client, OpenFeign, gateway, circuitbreaker-resilience4j
- Messaging: `spring-kafka`
- Mapping: ModelMapper 3.1.1
- API docs: SpringDoc OpenAPI 2.1.0
- Observability: actuator, micrometer-observation, micrometer-tracing-bridge-brave, zipkin-reporter-brave
- Boilerplate: Lombok
- Tests: JUnit 5 + Mockito via `spring-boot-starter-test`

---

## 16) Known Landmines

- System JDK is 25; the project needs 17. Lombok annotation processing fails on 25.
- Turkish locale breaks gateway routing (`lower-case-service-id` produces a dotless `ı`).
- `common-package` must be installed before any service compiles; keep `<classifier>exec</classifier>`.
- `filter-service` base package has no `com.` prefix and its pom `groupId` is `kodlamaio`. A `rg 'com\.kodlamaio'` search misses it entirely.
- `spring.application.name` casing is inconsistent: `INVENTORY-SERVICE` and `INVOICE-SERVICE` are uppercase, the rest lowercase.
- Boot parent split: 3.1.0 on maintenance + payment, 3.0.6 on the other 8.
- `payment-service` uses `entity` (singular) and declares `spring-boot-starter-test` twice.
- `/api/payments/check` is permitAll with no controller behind it.
- `api-gateway`'s yml carries a stray `eureka.instance.metadata-map.serviceId: inventory-service`.
- `prometheus.yml` scrapes `/FILTER-SERVICE/...` (uppercase, broken under `lower-case-service-id`) and omits `invoice-service` entirely.
- No `micrometer-registry-prometheus` dependency is declared anywhere, so `/actuator/prometheus` likely 404s.
- All backing-service credentials are committed in plaintext in `docker-compose.yml`. Acceptable only because they are throwaway local-dev values; the pattern must not follow this project to anything real.
- Changing a database name or user in `docker-compose.yml` silently breaks the matching service until the external config repo is updated to match.
- `common-package` ships `CommonPackageApplication` (`@SpringBootApplication`) inside the scanned `ConfigurationBasePackage`, so every service component-scans a nested application class.
- `invoice-service` exposes only `GET /api/invoices`; its `getById` and Create/Update DTOs are unreachable over HTTP.
- No Dockerfile exists anywhere. `docker-compose.yml` provides backing services only.
- Adding a Kafka topic without adding it to the producing service's `OutboxRelay.TOPIC_TYPES` map means the row is written and never published — an ERROR every 5 seconds, no compile error.
- `ddl-auto` must stay `update` in the external config repo. `create-drop` would wipe unpublished `outbox_messages` rows on every restart.
- Event delivery is at-least-once and up to 5 seconds late. Anything that assumed inline publishing (a read-your-write against `filter-service` right after a `POST`) will look broken and isn't.
- `KafkaProducer.sendMessage` logs the whole event via `event.toString()`, which for `RentalPaymentCreatedEvent` includes `cardHolder` — §9 says never log PII. Pre-existing; not fixed here.
- `InvoiceManager` injects a `KafkaProducer` it never uses. Pre-existing dead field.
- Annotating `PaymentClientTokenConfiguration` with `@Configuration`/`@Component` silently widens the Feign token interceptor to every client in rental-service, including `CarClient` — the app still starts and still works while Keycloak is up, then fails with *inventory-service*'s message when it isn't. A test guards it; don't delete the test.
- `authorization-grant-type` in the external config must be `client_credentials` with an **underscore**. A hyphen passes startup validation and then makes `authorize()` return `null` on the first payment call.
- A **missing** `client-secret` produces no startup error at all: `ClientRegistration` defaults the auth method to `NONE` and Keycloak answers with 401 `invalid_client` at first use.
- Creating the `service` realm role is not enough — it must be assigned on the client's **Service accounts roles** tab. If it isn't, a valid token is still issued and payment-service returns 403, which the fallback reports as `PAYMENT DOWN`. Decode `realm_access.roles` rather than guessing from the status code.
- The service token is cached ~4 minutes (5-min Keycloak lifespan minus a 60s clock skew). An expired-token 401 is **not** retried and does not evict the cache entry.
- `COMPENSATION_FAILED` is excluded from `SagaRecoveryScheduler.RECOVERABLE`, so a refund that fails because Keycloak was momentarily down is never retried: the customer stays charged. The interceptor's ERROR log is the only signal.

---

## 17) AI Interaction Rules

- **Stop and think.** Before making changes, identify exactly how data flows and what the actual problem is.
- **Do not invent features.** Only implement what was asked for.
- **Do not invent config.** If a value lives in the external config repo, say so — never guess a datasource URL, port, or `jwk-set-uri`.
- **Verify.** Run the appropriate module's tests or CLI commands before handing the task back. Treat every edit as a real PR.
- **Be brief.** Use short sentences. Cut the fluff. "Done, tests pass" is better than "I have successfully implemented your request and verified it with the testing suite."
