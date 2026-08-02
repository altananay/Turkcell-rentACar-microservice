# Turkcell Rent-A-Car Microservices — Backend Analyst Standards

> **Supporting reference for [SKILL.md](SKILL.md).** Extends its Role, Project Context, and Constraints sections.
> This file adds output templates, syntax rules, and the File Creation Validation Checklist. Load it only when generating or validating the output files — SKILL.md's Procedure links here at the point it's needed.

Reference templates for producing `rentacar-microservices.html` and `rentacar-microservices.md`.
Replace ALL placeholder labels with actual content from analysis — templates are starting points only.

> **CRITICAL FILE RULE**: Always **overwrite** output files completely — **NEVER append**.
> If a file already exists, replace its entire contents in one write operation. Appending produces duplicate document structures that break rendering.

---

## Output Template

### `rentacar-microservices.html`

Use this scaffold. Replace all `{{PLACEHOLDER}}` values with real content found during analysis.

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Turkcell Rent-A-Car Microservices — Backend Audit</title>
  <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
  <style>
    *, *::before, *::after { box-sizing: border-box; }
    body { font-family: system-ui, sans-serif; margin: 0; background: #0d1117; color: #c9d1d9; line-height: 1.6; }
    nav { position: sticky; top: 0; background: #161b22; border-bottom: 1px solid #30363d; padding: 0.75rem 1.5rem; display: flex; gap: 1.5rem; flex-wrap: wrap; z-index: 100; }
    nav a { color: #58a6ff; text-decoration: none; font-size: 0.875rem; }
    nav a:hover { text-decoration: underline; }
    main { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }
    h1 { font-size: 1.75rem; border-bottom: 1px solid #30363d; padding-bottom: 0.5rem; }
    h2 { font-size: 1.25rem; margin-top: 2.5rem; border-left: 3px solid #58a6ff; padding-left: 0.75rem; }
    h3 { font-size: 1rem; color: #8b949e; }
    table { width: 100%; border-collapse: collapse; margin: 1rem 0; font-size: 0.875rem; }
    th { background: #161b22; color: #8b949e; text-align: left; padding: 0.5rem 0.75rem; border: 1px solid #30363d; }
    td { padding: 0.5rem 0.75rem; border: 1px solid #30363d; vertical-align: top; }
    tr:nth-child(even) { background: #161b22; }
    .badge { display: inline-block; padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: 600; }
    .high   { background: #3d1c1c; color: #f85149; }
    .medium { background: #2d2008; color: #e3b341; }
    .low    { background: #0d2216; color: #3fb950; }
    .public    { background: #0d2216; color: #3fb950; }
    .protected { background: #2d2008; color: #e3b341; }
    .admin     { background: #3d1c1c; color: #f85149; }
    .internal  { background: #1c2b3d; color: #58a6ff; }
    .external  { background: #21262d; color: #8b949e; }
    pre.mermaid { background: #161b22; border: 1px solid #30363d; border-radius: 6px; padding: 1rem; overflow-x: auto; }
    details summary { cursor: pointer; font-weight: 600; padding: 0.5rem 0; color: #58a6ff; }
    details[open] summary { margin-bottom: 0.5rem; }
    :focus-visible { outline: 2px solid #58a6ff; outline-offset: 2px; }
    @media (prefers-reduced-motion: reduce) { * { transition: none !important; } }
  </style>
</head>
<body>
  <nav aria-label="Report sections">
    <a href="#summary">Summary</a>
    <a href="#topology">Topology</a>
    <a href="#shared">Shared Kernel</a>
    <a href="#endpoints">Endpoints</a>
    <a href="#auth">Auth</a>
    <a href="#sync">Sync &amp; Resilience</a>
    <a href="#events">Events</a>
    <a href="#data">Data Layer</a>
    <a href="#config">Config &amp; Deployment</a>
    <a href="#observability">Observability</a>
    <a href="#risks">Risk Matrix</a>
    <a href="#handoff">Handoff Notes</a>
  </nav>

  <main>
    <h1>Turkcell Rent-A-Car Microservices — Backend Audit</h1>
    <p><strong>Repo:</strong> Turkcell-rentACar-microservice &nbsp;|&nbsp; <strong>Generated:</strong> {{DATE}}</p>

    <section id="summary">
      <h2>1. Executive Summary</h2>
      <p>{{One-paragraph overview of the system and overall risk posture. Call out the top 3 priority findings.}}</p>
    </section>

    <section id="topology">
      <h2>2. Service Topology</h2>
      <table>
        <thead><tr><th>Module</th><th>Type</th><th>Boot parent</th><th>Maven wrapper</th><th>Local port</th><th>Datastore</th><th>Uses common-package</th></tr></thead>
        <tbody>
          <tr>
            <td><code>{{module}}</code></td>
            <td>{{platform / shared library / business}}</td>
            <td>{{version}}</td>
            <td>{{version}}</td>
            <td>{{port or <span class="badge external">External</span>}}</td>
            <td>{{engine + host port or —}}</td>
            <td>{{yes / no}}</td>
          </tr>
        </tbody>
      </table>
      <p><em>{{State explicitly whether a root/reactor pom.xml exists and what that means for build commands.}}</em></p>
      <h3>Diagram D1 — Service Topology</h3>
      <pre class="mermaid">
graph LR
  Client([Client])
  subgraph Platform
    GW[{{api-gateway:port}}]
    DISC[{{discovery-server:port}}]
    CFG[{{config-server:port}}]
    GIT[({{external config git repo}})]
    KC[{{Keycloak:port}}]
  end
  subgraph "Business Services"
    S1[{{service-name}}]
  end
  subgraph Datastores
    DB1[({{engine host:port}})]
  end
  Client --> GW
  GW -->|/{service-id}/**| S1
  S1 -.register.-> DISC
  S1 -.spring.config.import.-> CFG
  CFG --> GIT
  S1 --> DB1
  S1 -.jwk-set-uri.-> KC
      </pre>
    </section>

    <section id="shared">
      <h2>3. Shared Kernel (<code>common-package</code>)</h2>
      <table>
        <thead><tr><th>Class</th><th>Package</th><th>Purpose</th><th>Consumed by</th></tr></thead>
        <tbody>
          <tr><td>{{ClassName}}</td><td><code>{{package}}</code></td><td>{{one sentence}}</td><td>{{services, or "all 6"}}</td></tr>
        </tbody>
      </table>
      <h3>Inherited dependency surface</h3>
      <table>
        <thead><tr><th>Dependency</th><th>Version</th><th>What it enables in every consuming service</th></tr></thead>
        <tbody>
          <tr><td>{{artifactId}}</td><td>{{version or "managed"}}</td><td>{{effect}}</td></tr>
        </tbody>
      </table>
      <p><em>{{Note which beans land in every service context via scanBasePackages, and flag anything in the shared kernel used by only one service.}}</em></p>
    </section>

    <section id="endpoints">
      <h2>4. Endpoint Inventory</h2>
      <details open>
        <summary>{{service-name}}</summary>
        <table>
          <thead><tr><th>Method</th><th>Service path</th><th>Gateway path</th><th>Handler</th><th>Auth</th><th>@Valid</th><th>Request DTO</th><th>Response DTO</th><th>Status</th></tr></thead>
          <tbody>
            <tr>
              <td>{{GET/POST/…}}</td>
              <td><code>{{/api/…}}</code></td>
              <td><code>{{/service-id/api/…}}</code></td>
              <td>{{module/Class#method}}</td>
              <td><span class="badge public">Public</span> <em>or</em> <span class="badge protected">Role:user</span> <em>or</em> <span class="badge admin">Role:admin</span> <em>or</em> <span class="badge internal">Internal</span></td>
              <td>{{yes / no}}</td>
              <td>{{DTO or —}}</td>
              <td>{{DTO or —}}</td>
              <td>{{@ResponseStatus value or default}}</td>
            </tr>
          </tbody>
        </table>
      </details>
      <p><em>Auth values come from SecurityConfig's matcher list, evaluated in declaration order. "Internal" marks endpoints reachable only in-cluster via Feign. Cross-reference SecurityConfig — do not guess.</em></p>
    </section>

    <section id="auth">
      <h2>5. Auth &amp; Authorisation Model</h2>
      <h3>Diagram D4 — Token validation</h3>
      <pre class="mermaid">
sequenceDiagram
  participant Client
  participant Keycloak
  participant Gateway as {{api-gateway}}
  participant Chain as SecurityConfig filterChain
  participant Converter as KeycloakJwtRoleConverter
  participant Controller as {{module/Controller}}
  Client->>Keycloak: POST /realms/{{realm}}/protocol/openid-connect/token
  Keycloak-->>Client: access_token (JWT)
  Client->>Gateway: Request + Authorization: Bearer JWT
  Note over Gateway: {{state whether the gateway validates the token}}
  Gateway->>Chain: forwarded request
  Chain->>Chain: oauth2ResourceServer().jwt() — jwk-set-uri ({{External}})
  Chain->>Converter: convert(Jwt)
  Converter-->>Chain: {{claim}} -> ROLE_* authorities
  Chain->>Chain: matcher list, first match wins
  Chain->>Controller: {{method-level security check, if any}}
      </pre>
      <h3>Matcher order (as declared)</h3>
      <table>
        <thead><tr><th>#</th><th>Matcher</th><th>Rule</th><th>Notes / shadowing</th></tr></thead>
        <tbody>
          <tr><td>1</td><td><code>{{path pattern}}</code></td><td>{{permitAll / hasAnyRole / authenticated}}</td><td>{{what this shadows, or "—"}}</td></tr>
        </tbody>
      </table>
      <h3>Role model</h3>
      <table>
        <thead><tr><th>Role</th><th>Reachable paths</th><th>Notes</th></tr></thead>
        <tbody>
          <tr><td>Anonymous</td><td>{{permitAll paths}}</td><td>{{notes}}</td></tr>
          <tr><td>{{realm role}}</td><td>{{paths}}</td><td>{{notes}}</td></tr>
        </tbody>
      </table>
    </section>

    <section id="sync">
      <h2>6. Synchronous Call Graph &amp; Resilience</h2>
      <table>
        <thead><tr><th>Client</th><th>Declared in</th><th>Target</th><th>Endpoint(s)</th><th>Retry / CB</th><th>Fallback</th><th>Fallback throws</th><th>HTTP status</th></tr></thead>
        <tbody>
          <tr><td>{{ClientName}}</td><td>{{module}}</td><td>{{target service}}</td><td><code>{{path}}</code></td><td>{{annotation + instance name, or —}}</td><td>{{FallbackClass}}</td><td>{{ExceptionType}}</td><td>{{status via RestExceptionHandler}}</td></tr>
        </tbody>
      </table>
      <p><em>{{Report the actual @CircuitBreaker count found, and state where resilience4j configuration lives.}}</em></p>
      <h3>Diagram D3 — {{heaviest flow}} end to end</h3>
      <pre class="mermaid">
sequenceDiagram
  participant Client
  participant GW as {{api-gateway}}
  participant C as {{module/Controller}}
  participant M as {{module/Manager}}
  participant R as {{module/BusinessRules}}
  participant FC as {{FeignClient}}
  participant Target as {{target service}}
  participant DB as {{datastore}}
  participant K as KafkaProducer
  participant Cons as {{consumer service}}
  Client->>GW: {{METHOD /gateway/path}}
  GW->>C: forwarded
  C->>M: {{method}}
  M->>R: {{rule check}}
  R->>FC: {{feign call}}
  FC->>Target: {{HTTP path}}
  Target-->>FC: {{response DTO}}
  M->>DB: save
  M->>K: sendMessage(event, "{{topic}}")
  K-->>Cons: {{topic}}
  Note over M: {{flag the save-vs-publish ordering and the absence of a transaction/outbox}}
      </pre>
    </section>

    <section id="events">
      <h2>7. Event Choreography (Kafka)</h2>
      <table>
        <thead><tr><th>Topic</th><th>Event class</th><th>Producer</th><th>Consumers (groupId)</th><th>Payload fields</th></tr></thead>
        <tbody>
          <tr><td><code>{{topic}}</code></td><td>{{EventClass}}</td><td>{{module/Manager}}</td><td>{{module/Consumer (groupId)}}</td><td>{{fields}}</td></tr>
        </tbody>
      </table>
      <h3>Diagram D2 — Event choreography</h3>
      <pre class="mermaid">
graph LR
  P1[{{producer service}}]
  T1(({{topic-literal}}))
  C1[{{consumer service}}]
  P1 --> T1
  T1 -->|{{groupId}}| C1
      </pre>
      <p><em>{{Flag orphan topics, duplicate groupIds, and whether topic names are centralised or repeated as literals.}}</em></p>
    </section>

    <section id="data">
      <h2>8. Data Layer &amp; Ownership</h2>
      <table>
        <thead><tr><th>Service</th><th>Engine</th><th>Host port</th><th>Repository base</th><th>Entities / documents</th></tr></thead>
        <tbody>
          <tr><td>{{service}}</td><td>{{MySQL / PostgreSQL / MongoDB}}</td><td>{{port}}</td><td>{{JpaRepository / MongoRepository}}</td><td>{{classes}}</td></tr>
        </tbody>
      </table>
      <details>
        <summary>{{service-name}} — schema detail</summary>
        <table>
          <thead><tr><th>Table / collection</th><th>Field</th><th>Type</th><th>Constraints / annotations</th></tr></thead>
          <tbody>
            <tr><td>{{name}}</td><td>{{field}}</td><td>{{type}}</td><td>{{@Id / @ManyToOne / enum / —}}</td></tr>
          </tbody>
        </table>
      </details>
      <p><em>{{State how schema is managed and where that configuration lives. Name the source of truth for each shared concept.}}</em></p>
      <h3>Diagram D5 (optional) — Shared identifier fan-out</h3>
      <pre class="mermaid">
graph TD
  SRC[{{owning entity}} — {{owning service}}/{{engine}}]
  REF1[{{referencing field}} — {{service}}/{{engine}}]
  SRC -.no FK - eventually consistent via Kafka.-> REF1
      </pre>
    </section>

    <section id="config">
      <h2>9. Configuration &amp; Deployment</h2>
      <h3>Local vs external configuration</h3>
      <table>
        <thead><tr><th>Key</th><th>Where it lives</th><th>Value</th></tr></thead>
        <tbody>
          <tr><td><code>{{key}}</code></td><td>{{local file path or config-server}}</td><td>{{value or <span class="badge external">External</span>}}</td></tr>
        </tbody>
      </table>
      <h3>Backing services (<code>docker-compose.yml</code>)</h3>
      <table>
        <thead><tr><th>Service</th><th>Image</th><th>Host port</th><th>Consumed by</th></tr></thead>
        <tbody>
          <tr><td>{{name}}</td><td>{{image}}</td><td>{{host:container}}</td><td>{{service}}</td></tr>
        </tbody>
      </table>
      <p><em>{{State whether any Spring Boot app is containerized and whether a Dockerfile exists anywhere.}}</em></p>
    </section>

    <section id="observability">
      <h2>10. Observability</h2>
      <table>
        <thead><tr><th>Scrape job</th><th>Configured metrics_path</th><th>Actual spring.application.name</th><th>Reachable?</th></tr></thead>
        <tbody>
          <tr><td>{{job}}</td><td><code>{{path}}</code></td><td>{{name}}</td><td>{{yes / no + reason}}</td></tr>
        </tbody>
      </table>
      <p><em>{{Trace the tracing chain end to end and list gaps — services with no scrape job, missing registry dependencies, absent dashboard provisioning.}}</em></p>
    </section>

    <section id="risks">
      <h2>11. Risk Matrix</h2>
      <table>
        <thead><tr><th>#</th><th>Category</th><th>Finding</th><th>Evidence</th><th>Tag</th><th>Impact</th><th>Effort</th></tr></thead>
        <tbody>
          <tr>
            <td>1</td>
            <td>{{Security / Resilience / Data Consistency / Build / Observability / Testing / API Contract}}</td>
            <td>{{Finding — concise noun phrase}}</td>
            <td><code>{{path/to/file.java:line}}</code></td>
            <td>Confirmed</td>
            <td><span class="badge high">High</span></td>
            <td><span class="badge high">High</span></td>
          </tr>
        </tbody>
      </table>
    </section>

    <section id="handoff">
      <h2>12. Handoff Notes</h2>
      <p>{{None. | Out-of-scope findings and where they belong — e.g. the external config repo, the Keycloak realm export.}}</p>
    </section>
  </main>

  <script>mermaid.initialize({ startOnLoad: true, theme: 'dark' });</script>
</body>
</html>
```

**Rules for HTML output:**
- `<!DOCTYPE html>` must appear exactly once.
- All Mermaid diagrams must use `<pre class="mermaid">` blocks — never `<div>` or `<script>` tags.
- No inline `style=""` attributes on content elements; use the stylesheet classes above.
- Every `<section>` must have a matching `<a href>` in `<nav>`.
- Severity badges must use exactly the CSS classes `high`, `medium`, or `low`.
- Endpoint auth badges must use `public`, `protected`, `admin`, or `internal`. Externalized configuration values use `external`.
- **Every service-scoped table must be wrapped in `<details>` with the service name in the `<summary>`.** Ten modules produce far too much content for flat tables.
- `lang` attribute on `<html>` is required.

---

## Syntax Rules

### Rule 1 — Endpoint Path Accuracy
Always combine the class-level `@RequestMapping` prefix with the method-level annotation, **and** record the gateway-exposed path separately as `/{spring.application.name lower-cased}{service path}`.

Example: a service whose `spring.application.name` is `INVENTORY-SERVICE` and whose controller declares `@RequestMapping("/api/cars")` with a bare `@GetMapping` yields service path `/api/cars` and gateway path `/inventory-service/api/cars`. Never record only the method-level fragment, and never omit the gateway path.

### Rule 2 — DTO Names
Use the simple class name only (no package prefix) in tables. If the endpoint takes no body, write `—` (not `null` or `void`). If the return type is `void` or `ResponseEntity<Void>`, write `—`.

### Rule 3 — Evidence Citations
All `Confirmed` findings must cite `ClassName.java:lineNumber`. All `Inferred` findings must state the basis: `Inferred from the absence of an explicit rule in SecurityConfig.java:34`. Never cite a file you have not read.

### Rule 4 — Mermaid Node Names
Use real module names and real Java class simple names. For Kafka topics use the **literal topic string** as the node label and put the `groupId` on the edge label. Do not invent node names that don't exist in the codebase. If a relationship doesn't exist, omit the arrow rather than adding a placeholder node.

### Rule 5 — Externalized Configuration
Any datasource URL, Kafka bootstrap server, Eureka URL, Keycloak `jwk-set-uri`, resilience4j setting, or business-service `server.port` must be rendered as:

`External — <service>-dev.yml in github.com/altananay/config-server`

Only ports declared in a local `application.yml` may be stated as fact. A port that is a framework default rather than a declared value must be tagged `Inferred`. Never invent a value to fill a cell.

### Rule 6 — Module Qualification
Class names that collide across modules must be written `<module>/<ClassName>` everywhere — tables, prose, and diagram node labels. Verify collisions during the run rather than trusting a remembered list; `RentalConsumer`, `MaintenanceConsumer`, `CarClient` and `CarClientFallback` are known repeat offenders.

### Rule 7 — Package Literalism
Reproduce packages and directory names exactly as they are; do not silently normalize them. Some base packages omit the `com.` prefix, some entity packages are singular, and the Kafka consumer package is singular. If a deviation is worth reporting, it belongs in the Risk Matrix — not in a quietly corrected path string.

### Rule 8 — Secret Redaction
Never copy a credential value into an output file. Cite the location only:

> credentials committed in `` `docker-compose.yml:86` ``

This applies to `docker-compose.yml` (Keycloak admin, Postgres, MySQL and Mongo passwords), any credential-bearing file found during the scan, and anything discovered about the external config repo. A finding that says "plaintext password at `docker-compose.yml:86`" is complete; adding the password itself makes the report a second leak.

---

## File Creation Validation Checklist

After generating each output file, verify every item before marking the step complete:

1. **Files exist** — confirm the writes succeeded at `backend-audit/rentacar-microservices.md` and `backend-audit/rentacar-microservices.html`
2. **Single document root** — `<!DOCTYPE html>` appears exactly once in the HTML file; the `# Turkcell Rent-A-Car Microservices — Backend Audit` heading appears exactly once in the MD file
3. **All 12 sections present** — Executive Summary, Service Topology, Shared Kernel, Endpoint Inventory, Auth & Authorisation Model, Synchronous Call Graph & Resilience, Event Choreography, Data Layer & Ownership, Configuration & Deployment, Observability, Risk Matrix, Handoff Notes
4. **No placeholder text** — no `{{PLACEHOLDER}}` strings remain in either output file
5. **No empty sections and no empty tables** — every section has substantive content; every table has at least one data row
6. **Topology completeness** — every Maven module found in Step 1 appears in the topology table with its Boot parent version and datastore
7. **Endpoint coverage** — every `@RestController` scanned in Step 3 contributes at least one row, and every row carries **both** a service path and a gateway path
8. **Event coverage** — every Kafka topic found in Step 6 appears with its producer, consumer(s), `groupId`, and event class
9. **Feign coverage** — every `@FeignClient` found in Step 5 appears with its fallback class, thrown exception type, and resulting HTTP status
10. **Auth ordering** — the auth section lists SecurityConfig matchers in **declaration order** and names every method-level security annotation found
11. **No invented configuration** — every externalized value is rendered per Rule 5; no port, URL, or credential appears that was not read from a local file
12. **Minimum findings** — Risk Matrix contains at least 10 rows, each with a `Confirmed` or `Inferred` tag and a `file:line` citation; every Impact and Effort value is exactly `High`, `Medium`, or `Low`
13. **Mermaid correctness** — all 4 required diagrams are present (D1 topology, D2 choreography, D3 heaviest-flow sequence, D4 token validation), each in a `<pre class="mermaid">` block, all node names matching real modules/classes/topics
14. **Nav completeness** — every `<section id="…">` in the HTML has a matching `<a href="#…">` in `<nav>`
15. **No secret values** — grep both output files for any credential string found during the scan. Only `file:line` citations are acceptable; a match on an actual value is a failing check

If any check fails, correct the file via targeted edits or full regeneration, then rerun all checklist items. The file is valid only when every check passes.

---

## Output Document Structure

### `rentacar-microservices.md`

```markdown
# Turkcell Rent-A-Car Microservices — Backend Audit

**Generated:** {{DATE}}

---

## 1. Executive Summary
[One-paragraph overview. State the overall risk posture and name the top 3 priority findings.]

---

## 2. Service Topology
| Module | Type | Boot parent | Maven wrapper | Local port | Datastore | Uses common-package |
|---|---|---|---|---|---|---|
| {{module}} | {{platform/library/business}} | {{version}} | {{version}} | {{port or External}} | {{engine + port}} | {{yes/no}} |

[State whether a root/reactor pom.xml exists and what that implies for build commands.]

[Mermaid D1 — service topology]

---

## 3. Shared Kernel (`common-package`)
| Class | Package | Purpose | Consumed by |
|---|---|---|---|
| {{ClassName}} | `{{package}}` | {{one sentence}} | {{services}} |

[Inherited dependency surface table. Note which beans land in every service via scanBasePackages.]

---

## 4. Endpoint Inventory

### {{service-name}}
| Method | Service path | Gateway path | Handler | Auth | @Valid | Request DTO | Response DTO | Status |
|---|---|---|---|---|---|---|---|---|
| {{GET}} | {{/api/…}} | {{/service-id/api/…}} | {{module/Class#method}} | {{Public/Role:user/Role:admin/Internal}} | {{yes/no}} | {{DTO or —}} | {{DTO or —}} | {{status}} |

> Auth values come from SecurityConfig's matcher list, evaluated in declaration order.

---

## 5. Auth & Authorisation Model

[Mermaid D4 — token validation sequence]

### Matcher order (as declared)
| # | Matcher | Rule | Notes / shadowing |
|---|---|---|---|

### Role model
| Role | Reachable paths | Notes |
|---|---|---|

---

## 6. Synchronous Call Graph & Resilience
| Client | Declared in | Target | Endpoint(s) | Retry / CB | Fallback | Fallback throws | HTTP status |
|---|---|---|---|---|---|---|---|

[Report the actual @CircuitBreaker count. State where resilience4j config lives.]

[Mermaid D3 — heaviest flow, end to end]

---

## 7. Event Choreography (Kafka)
| Topic | Event class | Producer | Consumers (groupId) | Payload fields |
|---|---|---|---|---|

[Mermaid D2 — event choreography]

---

## 8. Data Layer & Ownership
| Service | Engine | Host port | Repository base | Entities / documents |
|---|---|---|---|---|

[Per-service schema detail. State how schema is managed and where that configuration lives. Name the source of truth for each shared concept.]

---

## 9. Configuration & Deployment
| Key | Where it lives | Value |
|---|---|---|

[docker-compose backing services table. State whether any Spring Boot app is containerized and whether a Dockerfile exists.]

---

## 10. Observability
| Scrape job | Configured metrics_path | Actual spring.application.name | Reachable? |
|---|---|---|---|

[Tracing chain and gaps.]

---

## 11. Risk Matrix

| # | Category | Finding | Evidence (file:line) | Tag | Impact | Remediation Effort |
|---|---|---|---|---|---|---|
| 1 | {{Category}} | {{Finding}} | `{{path:line}}` | Confirmed | High | High |

---

## 12. Handoff Notes
[None. | Out-of-scope findings and where they belong.]
```

---

## Finding Table Template

Minimum **10 rows** required. Use `High / Medium / Low` for all Impact and Remediation Effort values. The `Category` column must be one of: Security · Resilience · Data Consistency · Build · Observability · Testing · API Contract.

| # | Category | Finding | Evidence (file:line) | Tag | Impact | Remediation Effort |
|---|---|---|---|---|---|---|
| 1 | Security | {{Finding — concise noun phrase}} | `{{path/to/file.java:line}}` | Confirmed | High | High |
| 2 | Data Consistency | {{Finding — concise noun phrase}} | `{{path/to/file.java:line}}` | Confirmed | Medium | Medium |
| 3 | Observability | {{Finding — concise noun phrase}} | `{{path/to/file.yml:line}}` | Inferred | Low | Low |
