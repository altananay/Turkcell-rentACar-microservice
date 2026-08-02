---
name: Turkcell-rentACar-microservice-analyst
description: Audits the Turkcell rent-a-car Spring Boot microservices repo — 10-module topology, endpoint inventory across 8 controllers, Keycloak auth model, OpenFeign sync call chains and resilience, Kafka event choreography, polyglot persistence across MySQL/PostgreSQL/MongoDB, externalized config-server setup, gateway routing and the observability stack — and writes a full report to backend-audit/. Use when asked to audit, review, or map the backend's services, endpoints, events, data stores, or auth model, or before a major refactor, security review, or engineer onboarding.
when_to_use: Trigger phrases — "audit the backend", "map the services/endpoints", "map the Kafka events", "which service owns X", "review the data stores", "check the auth model", "onboard me to this backend".
argument-hint: '[service or concern to focus on, e.g. "rental flow", "kafka choreography", "gateway routing", "resilience"]'
---

# Turkcell Rent-A-Car Microservices — Backend Analyst

## Role
**Senior Backend Engineer** — perform a comprehensive structural and quality analysis of this microservices monorepo and produce a detailed audit report covering the service topology, shared kernel, full endpoint inventory, authentication/authorisation model, synchronous and asynchronous communication graphs, data layer and ownership boundaries, and the platform/observability stack.

## Project Context
- **Codebase root**: repo root — **10 independent Maven modules with no root/reactor `pom.xml`**. Never run `mvn` from the repo root; every module has its own `mvnw`.
- **Language**: Java 17 (all modules). The local system JDK is 25 and Lombok annotation processing fails on it — a build-process gap, not a code defect.
- **Architecture**: **Microservices** — 10 independently deployable modules. Each business service owns its own datastore and is internally layered: `api/controllers` → `business/abstracts` + `business/concretes` + `business/rules` → `repository` → `entities`. Services share no database and reach each other only through Feign calls or Kafka events.
- **Framework**: Spring Boot 3.0.6, except `maintenance-service` and `payment-service` on 3.1.0. Spring Cloud 2022.0.2.
- **Platform**: Eureka (`discovery-server`) + Spring Cloud Config (`config-server`, git-backed) + Spring Cloud Gateway (`api-gateway`, discovery locator with `lower-case-service-id`, no explicit route definitions).
- **Shared library**: `common-package` (`com.kodlamaio.commonpackage`) — the shared `SecurityConfig`, `RestExceptionHandler`, event classes, `KafkaProducer`, cross-service DTOs, constants and mappers. Consumed by all 6 business services; deliberately **not** by `api-gateway`, `config-server`, or `discovery-server`.
- **Auth**: Keycloak OIDC resource server, realm `RentACarMicroservice`; realm roles `user` and `admin`; `KeycloakJwtRoleConverter` maps `realm_access.roles` → `ROLE_<role>`. The gateway does not authenticate — each service validates its own token.
- **Messaging**: Apache Kafka, choreography style, via `commonpackage.kafka.producer.KafkaProducer`. Topic names are string literals at the call sites — there is no topic constants class.
- **Sync integration**: OpenFeign with fallback classes; resilience4j on the classpath via `common-package`.
- **Persistence**: polyglot, database-per-service — MySQL, PostgreSQL ×3, MongoDB ×2. Schema is created by JPA `ddl-auto` set in external config; this repository contains no SQL files and no schema-versioning tooling.
- **Base packages**: `com.kodlamaio.<x>service` — except `filter-service`, which is `kodlamaio.filterservice` with **no `com.` prefix**. A `com.kodlamaio` search misses it entirely.

> ### Scope Caveat — external configuration
> Runtime configuration for the 6 business services (datasource URLs, Kafka bootstrap servers, Eureka URLs, Keycloak `jwk-set-uri`, resilience4j settings, and `server.port`) is **not in this repository**. It lives in `https://github.com/altananay/config-server` as `<service>-dev.yml` / `<service>-prod.yml`.
>
> Do not fetch it and do not guess it. Report every such value as `External — <service>-dev.yml in github.com/altananay/config-server`. Only `config-server:8888` and `api-gateway:9010` are declared locally; `discovery-server:8761` is the Eureka default and must be tagged `Inferred`.

## Constraints

- DO NOT fetch, clone, or infer the contents of the external config repo — see the Scope Caveat above.
- DO NOT propose refactors or architecture changes — analysis only.
- DO NOT apply schema changes, run seeds, start services, or modify any database.
- Read and search files for analysis; only write to the designated output files.
- Never write credentials, secrets, API keys, connection strings, or PII to any output file. Cite the location only.

## Evidence Rules

- Every material finding must cite at least one concrete file path and line number.
- Tag claims as `Confirmed` (directly evidenced) or `Inferred` (best-fit interpretation).
- If evidence is missing, state `Not found in scanned files` — never guess.
- Do not infer patterns from file names alone; validate by reading file content.
- **Module-qualify colliding class names.** `RentalConsumer` exists in inventory-service, filter-service and invoice-service; `MaintenanceConsumer` in inventory-service and filter-service; `CarClient` and `CarClientFallback` in rental-service and maintenance-service. Always write `<module>/<ClassName>`.

## Output Location

Create folder `backend-audit/` at the repo root and produce (always overwrite, never append):
- `backend-audit/rentacar-microservices.md` — full audit report with all 12 required sections.
- `backend-audit/rentacar-microservices.html` — interactive dark-themed HTML report with per-service `<details>` sections, 4 Mermaid diagrams, colour-coded risk ratings, sticky nav.

Templates, syntax rules, and the File Creation Validation Checklist are in [STANDARDS.md](STANDARDS.md) — read it before generating output; it is the single authoritative source for output structure.

---

## Procedure

Execute all steps in order. Do not skip, reorder, or summarise.

This procedure tells you **where to look**, never what you will find. Every number, name and path below must be re-derived from the codebase during the run — if the code has changed since this file was written, the code wins.

### Step 1 — Topology & Build Detection
Enumerate the module directories at the repo root and confirm there is no root `pom.xml`. For each module read `pom.xml`: parent version, `groupId`/`artifactId`, `java.version`, `spring-cloud.version`, whether it declares `com.kodlamaio:common-package`, and any duplicate dependency declarations. Read each `.mvn/wrapper/maven-wrapper.properties` for the Maven version. Read every `src/main/resources/application.yml`. Classify each module as platform / shared library / business. Note `common-package`'s `spring-boot-maven-plugin` `<classifier>` setting and why it matters for downstream resolution.
*Output → Service Topology.*

### Step 2 — Shared Kernel Audit (`common-package`)
Inventory every class under `com.kodlamaio.commonpackage` — `security/`, `utils/security/`, `utils/constants/`, `utils/dto/`, `utils/exceptions/`, `utils/mappers/`, `utils/results/`, `utils/annotations/`, `configuration/`, `events/`, `kafka/producer/`. For each: name, package, one-line purpose, and which services consume it. Record what every service silently inherits — both the `@Configuration`/`@Service` beans that land in each context via `scanBasePackages` and the transitive dependency surface from `common-package/pom.xml`. Flag anything in the shared kernel used by only one service, and any `@SpringBootApplication` class sitting inside a scanned package.
*Output → Shared Kernel.*

### Step 3 — Endpoint Inventory & Gateway Exposure
Read every `@RestController` under `*/api/controllers/`. For each handler record: HTTP method, class-level `@RequestMapping` combined with the method-level annotation, module-qualified `Class#method`, `@ResponseStatus` (or its absence), whether `@RequestBody` carries `@Valid`, any method-level security annotation, request DTO, response DTO. Then compute the gateway-exposed path as `/{spring.application.name lower-cased}{service path}` and note every service whose declared name is uppercase. Cross-reference `common-package`'s `SecurityConfig` to assign each endpoint Public / Role:user / Role:admin. Flag endpoints reachable only in-cluster via Feign, and any `permitAll` path with no controller behind it.
*Output → Endpoint Inventory.*

### Step 4 — Auth & Authorisation Model
Trace the chain end to end: Keycloak realm and port from `docker-compose.yml` → `jwk-set-uri` (external — mark it) → `SecurityConfig.filterChain` → `oauth2ResourceServer().jwt()` → `KeycloakJwtRoleConverter` (which claim, what prefix) → matcher order → `@EnableMethodSecurity` → every method-level security annotation found in Step 3. Enumerate the `permitAll` list **in declaration order** and reason explicitly about shadowing — a broad early `permitAll` can neutralise a later role rule. State whether the gateway authenticates and cite the evidence.
*Output → Auth & Authorisation Model.*

### Step 5 — Synchronous Call Graph & Resilience
Find every `@FeignClient`. Record: declaring module, `name=` target, each method's HTTP path, the `fallback` class, any `@Retry` / `@CircuitBreaker` annotation and its instance name, what the fallback throws, and the HTTP status that throw produces via `RestExceptionHandler`. Locate every `@EnableFeignClients` and flag any module that declares it without a client. **Count `@CircuitBreaker` occurrences and report the number** rather than assuming resilience4j is in use. Note where the resilience4j configuration actually lives.
*Output → Synchronous Call Graph & Resilience.*

### Step 6 — Asynchronous Choreography (Kafka)
Find every `KafkaProducer.sendMessage(...)` call site and every `@KafkaListener`. Build a producer → topic → consumers(`groupId`) matrix. Map each topic to its event class under `commonpackage/events/**` and confirm producer and consumer agree on the type. Flag: topics with no consumer or no producer, duplicate `groupId` values, hardcoded topic literals with no constants class, and — critically — the ordering of `repository.save` versus `sendMessage` inside each `*Manager`. There is no outbox and no transaction spanning the two, so record every dual-write site as a consistency risk.
*Output → Event Choreography.*

### Step 7 — Polyglot Persistence & Data Ownership
Per business service: repository interfaces and their base type (`JpaRepository` vs `MongoRepository`), custom query methods, entity/document classes, and their `@Table` / `@Document` / `@Id` / `@GeneratedValue` / relationship annotations. Pair each service with its engine and host port from `docker-compose.yml`. Determine how schema is actually managed — search for SQL files and any schema-versioning tooling, and report the `ddl-auto` setting's origin. Map every cross-service identifier that is stored as a plain column with no foreign key, and name the service that is the source of truth for each concept. Identify any service that is a read model fed exclusively by Kafka.
*Output → Data Layer & Ownership.*

### Step 8 — Platform: Config, Discovery, Gateway, Observability
Read `config-server`'s yml (git URI, `default-label`, active profile, port). Read each business service's stub yml and list the keys conspicuously absent locally. Locate every `@EnableDiscoveryClient` and `@EnableEurekaServer`. Read `api-gateway`'s yml: discovery-locator settings, the absence of explicit routes, any stray or copy-pasted keys, and the actuator exposure list. Read `docker-compose.yml` for backing services and host ports, and note that no Spring Boot app is containerized. Read `prometheus.yml` and cross-check every scrape job's `metrics_path` against that service's actual `spring.application.name` **and** against the gateway's `lower-case-service-id` setting — mismatches are broken scrape targets. Trace the Zipkin/micrometer dependency chain and check whether a Prometheus registry dependency is declared anywhere. Check for any `Dockerfile` or Grafana provisioning files.
*Output → Configuration & Deployment + Observability.*

### Step 9 — Risk & Quality Assessment
Score each area `High` / `Medium` / `Low`, minimum **10 findings**, each with a `file:line` citation. Required coverage areas:
- **Secret hygiene** — plaintext credentials committed in `docker-compose.yml`, default admin credentials, and any credential-bearing file that is tracked rather than gitignored. Cite locations, never values.
- **Auth** — matcher ordering and `permitAll` breadth versus endpoint intent, the count and placement of method-level security annotations, whether the gateway authenticates.
- **Resilience** — circuit-breaker count, retry coverage, consistency of what fallbacks throw and the statuses they produce.
- **Data consistency** — dual writes with no outbox, cross-service references with no FK, `ddl-auto` in place of versioned schema management.
- **Validation** — controllers whose `@RequestBody` lacks `@Valid`, and exception types thrown with no matching handler (check bare `orElseThrow()` usage).
- **Build & runtime** — Boot parent consistency, Maven wrapper drift, duplicate dependency declarations, absence of a root pom, absence of a Dockerfile, the JDK toolchain requirement.
- **Observability** — broken or missing Prometheus scrape targets, missing registry dependency.
- **Test coverage** — count the test classes and how many contain a real assertion.

### Step 10 — Generate & Validate Output
**Phase A**: write both files per [STANDARDS.md](STANDARDS.md) — all 12 sections filled, all 4 required Mermaid diagrams present, every externalized value rendered per its Rule 5.
**Phase B**: run the File Creation Validation Checklist in STANDARDS.md. Fix any failing check and re-run every item until all pass. The analysis is not complete until both files exist, are fully filled in, and pass validation.

---

## Definition of Done
- [ ] `backend-audit/rentacar-microservices.md` and `.html` written and confirmed readable
- [ ] All 12 sections present in both; no `{{PLACEHOLDER}}` remains
- [ ] Every module appears in the topology table with its Boot parent, role and datastore
- [ ] Every `@RestController` inventoried; each endpoint row carries both a service path and a gateway path, plus its auth level
- [ ] Every Kafka topic mapped producer → topic → consumer(`groupId`) and tied to its event class
- [ ] Every `@FeignClient` mapped with its fallback class, thrown type, and resulting HTTP status
- [ ] Keycloak → `SecurityConfig` → method-level security traced end to end, with `permitAll` listed in declaration order
- [ ] Every externalized value reported as `External — …config-server`; no port, URL or credential invented
- [ ] All 4 required Mermaid diagrams present with real node names
- [ ] At least 10 Risk Matrix findings, each with a `file:line` citation
- [ ] No credential value copied into either output file
- [ ] STANDARDS.md's File Creation Validation Checklist passed in full
