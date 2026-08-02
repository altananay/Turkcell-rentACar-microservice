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

---

## 4) Module Boundaries

1. `common-package` is the leaf. It imports no service package. Never add one.
2. Every business service depends on `common-package`. `config-server`, `discovery-server` and `api-gateway` must **not** — `common-package` pulls `spring-boot-starter-web` (MVC), which would flip the WebFlux gateway to a servlet stack and break Spring Cloud Gateway.
3. Services never import one another. Cross-service data crosses only via `commonpackage.utils.dto.*` (Feign) or `commonpackage.events.*` (Kafka).
4. A type shared by two services goes in `common-package`, never duplicated. Adding one requires `cd common-package && ./mvnw install` before the consumer compiles.
5. Inside a service: the controller depends on `business/abstracts/*Service`, never on `*Manager` directly. A `*Manager` owns exactly one repository. A `*BusinessRules` throws only `BusinessException`.
6. Feign clients live in `api/clients` and are invoked from `business/rules` (precedent: `RentalBusinessRules.ensureCarIsAvailable`, `ensurePaymentIsProcessed`) or `business/concretes` — never from a controller.
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
#   kafka 9092 · keycloak 8081 · zipkin 9411 · prometheus 9090 · grafana 3000
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

- Realm `RentACarMicroservice` at `localhost:8081` (the container maps host 8081 → container 8080). Realm roles: `user`, `admin`.
- One shared filter chain for every service: `common-package/src/main/java/com/kodlamaio/commonpackage/security/SecurityConfig.java`, annotated `@EnableMethodSecurity(securedEnabled = true)`. The `jwk-set-uri` comes from the external config repo.
- `utils/security/KeycloakJwtRoleConverter` maps the `realm_access.roles` claim to `ROLE_<role>` authorities.
- Matcher order — **first match wins**:
  1. `permitAll()`: `/api/filters`, `/api/cars/check-car-available/**`, `/api/payments/check`, `/api/cars`, `/api/cars/**`, `/actuator/**`
  2. `/api/**` → `hasAnyRole("user")`
  3. `anyRequest().authenticated()`
  `cors()` is enabled, `csrf()` is disabled.
- Consequence to keep in mind: because `/api/cars/**` is `permitAll`, the `@Secured("ROLE_admin")` on `inventory-service/CarsController#getAll` — the only method-level security annotation in the repo — is the sole gate on that path.
- `/api/payments/check` is in the permitAll list but **no controller declares it**. Dead rule.
- **The gateway does not authenticate.** Every service validates its own token.
- Mandatory: validate all inputs at boundaries, authorize every protected operation, parameterize all queries, never log credentials or PII.

---

## 12) Inter-Service Communication

### Kafka (async, choreography)

7 topics. **Topic names are hardcoded string literals at every call site — there is no topic constants class.** The producer is `commonpackage.kafka.producer.KafkaProducer#sendMessage(T extends Event, String topic)`.

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

Adding a topic requires: the event class in `common-package/events/<domain>/`, a `sendMessage(event, "topic")` call, a `@KafkaListener(topics = …, groupId = …)`, and a `common-package` reinstall.

### OpenFeign (sync)

| Client | Target | Endpoint | Resilience | Fallback throws |
|---|---|---|---|---|
| rental-service `CarClient` | inventory-service | `GET /api/cars/check-car-available/{carId}`, `GET /api/cars/get-car-for-invoice/{carId}` | `@Retry("rentalToInventory")` on `checkIfCarAvailable` only | `BusinessException` → 422 |
| rental-service `PaymentClient` | payment-service | `POST /api/payments/process-rental-payment` | none | `BusinessException` → 422 |
| maintenance-service `CarClient` | inventory-service | `GET /api/cars/check-car-available/{carId}` | `@Retry("maintenanceToInventory")` | `RuntimeException` → 500 (inconsistent — see §10) |

Resilience4j is on the classpath via `common-package`, but there are **zero `@CircuitBreaker` annotations** and only the 2 `@Retry` above. All resilience4j YAML lives in the external config repo.

`POST /api/rentals` is the heaviest flow: 3 synchronous hops (2× inventory, 1× payment) plus 2 Kafka publishes fanning out to 3 consumer services. Note that payment is debited *before* the rental row is saved and there is no compensating action — be careful when touching `RentalManager.add`.

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

- Every non-inventory service stores `UUID carId` as a plain column/field with **no foreign key**. Cross-service references are denormalized by design — never add an FK across services.
- `filter-service` is a Kafka-fed CQRS read model. Never write to it over HTTP.
- Its `Filter.state` is a free-text `String` (`"Rented"` / `"Maintenance"` / `"Available"`) duplicating inventory's `State` enum. Keep the two in sync when either changes.

---

## 14) Testing

**Current state:** ~162 unit tests across `common-package` and all 6 business services, covering `*Manager`, `*BusinessRules`, `*Controller`, `*Consumer`, and `*ClientFallback` classes. All are plain Mockito/AssertJ unit tests or `MockMvcBuilders.standaloneSetup` controller tests — no `@SpringBootTest`, no Spring context, no config-server or database dependency. They pass with all backing infrastructure stopped. The old default `*ApplicationTests.java` `contextLoads()` stubs (one per module, Spring Initializr boilerplate) have been deleted — they required a live config-server/Keycloak to load the context and asserted nothing. `spring-security-test` is not declared anywhere; controller tests that need `@Valid` enforcement use `standaloneSetup`, which wires Bean Validation automatically without a security context.

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
- Security: Spring Security OAuth2 Resource Server
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

---

## 17) AI Interaction Rules

- **Stop and think.** Before making changes, identify exactly how data flows and what the actual problem is.
- **Do not invent features.** Only implement what was asked for.
- **Do not invent config.** If a value lives in the external config repo, say so — never guess a datasource URL, port, or `jwk-set-uri`.
- **Verify.** Run the appropriate module's tests or CLI commands before handing the task back. Treat every edit as a real PR.
- **Be brief.** Use short sentences. Cut the fluff. "Done, tests pass" is better than "I have successfully implemented your request and verified it with the testing suite."
