# Turkcell Rent-A-Car Microservices — Skill Notes

> Supporting reference for [SKILL.md](SKILL.md). Not required reading to run the skill — this is for maintaining/extending the skill itself.

## How this skill is invoked

This is a standard Claude Code project skill living at `.claude/skills/Turkcell-rentACar-microservice/`. Claude Code handles discovery and invocation natively — there is no separate orchestration layer to maintain:

- **Automatic**: Claude loads it when a prompt matches `SKILL.md`'s `description`/`when_to_use` (e.g. "audit the backend", "map the services", "map the Kafka events").
- **Manual**: type `/Turkcell-rentACar-microservice` (the command name comes from the directory name, not the frontmatter `name` field). Because the directory wins, `SKILL.md`'s frontmatter `name` is deliberately set to match it — a mismatched `name` is silently ignored and only misleads whoever reads the file next.
- Only `SKILL.md` loads automatically on invocation. `STANDARDS.md` (and this file) are supporting files Claude reads on demand, when `SKILL.md` points to them — this keeps the always-in-context cost to just the `description` line until the skill actually runs.

## File layout

| File | Purpose |
|---|---|
| `SKILL.md` | Entry point — role, project context, the external-config scope caveat, constraints, evidence rules, and the 10-step audit procedure. Required. |
| `STANDARDS.md` | Output templates (HTML/MD scaffolds for a 12-section report), the 4 required Mermaid diagrams, 8 syntax rules, and the File Creation Validation Checklist. Loaded when generating/validating output. |
| `INSTRUCTIONS.md` (this file) | Maintenance notes only — not part of the audit procedure. |

## Adding another skill to this project

Create `.claude/skills/<new-skill-name>/SKILL.md` with `name`/`description` frontmatter (description drives auto-invocation — put the trigger phrase first). Add supporting files in the same directory and reference them from `SKILL.md` with a relative markdown link, e.g. `[REFERENCE.md](REFERENCE.md)`. Keep `SKILL.md` itself under ~500 lines; move long reference material to supporting files. No shared registry file is needed — each skill directory is self-contained and Claude Code discovers it automatically.

## Repo-specific caveats to keep in sync with SKILL.md

- **There is no companion frontend repo.** This is a backend-only monorepo; do not add handoff notes addressed to a frontend agent that does not exist.
- **Runtime configuration is out of audit scope.** Datasource URLs, Kafka bootstrap servers, Eureka URLs, Keycloak `jwk-set-uri`, resilience4j settings and the business services' `server.port` all live in `https://github.com/altananay/config-server`, not here. `SKILL.md` carries this as a Scope Caveat block and `STANDARDS.md` Rule 5 enforces how such values are rendered. If that external repo is ever vendored into this one, both must be updated together.
- **The procedure stores *where to look*, never *what will be found*.** This is deliberate. An earlier version of this skill hardcoded a fixed file inventory and rotted the moment the codebase moved. Step text should name directories, annotations and file patterns; counts and names belong in the generated report, not in `SKILL.md`. The only exceptions are the genuinely structural facts in Project Context — microservices with database-per-service, no root pom, `filter-service`'s missing `com.` prefix — which are cheap to re-verify and expensive to get wrong.
- **Known-true state at the time of writing** — re-verify each run and update this list when it changes: no `Dockerfile` exists anywhere and no Spring Boot app is containerized; ~162 unit tests exist across `common-package` and the 6 business services (Manager/BusinessRules/Controller/Consumer/ClientFallback layers), all Mockito/AssertJ or `standaloneSetup` MockMvc — no `@SpringBootTest`, no config-server dependency; there are zero `@CircuitBreaker` annotations despite resilience4j being on the classpath; all backing-service credentials are committed in plaintext in `docker-compose.yml` (there is no `.env` file — it was removed deliberately, so do not report its absence as a finding); the Spring Boot parent is split 3.0.6 / 3.1.0 across modules; `filter-service` uses the base package `kodlamaio.filterservice` with no `com.` prefix, so a `com.kodlamaio` search silently misses it.
- **`backend-audit/` is intentionally committed.** The generated reports are meant to be pushed, so do not suggest gitignoring the directory.
