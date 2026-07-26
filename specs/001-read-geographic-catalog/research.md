# Phase 0 Research: Read Geographic Catalog API

**Date**: 2026-07-25
**Feature**: [spec.md](spec.md)
**Constitution**: `.specify/memory/constitution.md` version 2.0.0

All technical decisions required for Phase 1 are resolved below.

## Decision 1: Reactive Persistence

**Decision**: Use the Vert.x Reactive PostgreSQL Client through
`io.quarkus:quarkus-reactive-pg-client`. Each application output port is implemented by an
explicit repository adapter with fixed prepared SQL. Do not add Hibernate ORM, Hibernate
Reactive Panache, JDBC, generic CRUD repositories, or runtime Flyway.

**Rationale**: The approved workload is projection-heavy and requires recursive ancestors,
JSON aggregation, deterministic PostgreSQL ordering, outcome discrimination, and at most one
SQL statement per database-backed observation. Explicit reactive SQL makes those guarantees visible and testable
without persistence entities or session lifecycle complexity.

**Alternatives considered**:

- Hibernate Reactive Panache: approved by the constitution but rejected because these queries
  do not benefit from aggregate persistence or entity mutation and recursive CTEs would still
  require database-specific queries.
- Runtime JDBC: rejected by constitutional prohibition.

## Decision 2: Runtime Dependencies

**Decision**: Keep the runtime dependency set minimal:

- `io.quarkus:quarkus-arc`
- `io.quarkus:quarkus-rest-jackson`
- `io.quarkus:quarkus-reactive-pg-client`
- `io.quarkus:quarkus-oidc`
- `io.quarkus:quarkus-micrometer`
- `io.micrometer:micrometer-registry-prometheus`
- `io.quarkus:quarkus-logging-json`

Use the repository-pinned Quarkus platform `3.33.2.1`, Java 25, and Gradle Wrapper 9.3.1.
OpenAPI parsing, Flyway, JDBC, PostgreSQL Testcontainers, and architecture tooling remain
test, build, or migration-image dependencies and are absent from `runtimeClasspath`.
An application CDI producer owns the `PrometheusMeterRegistry`; Quarkus Micrometer binds
meters to that registry, and the custom metrics resource calls its OpenMetrics scrape API.
No extension-owned metrics endpoint is enabled.

The initial `linux/amd64` verification images are pinned as
`docker.io/library/postgres:18@sha256:d93de42662696f278fb34354b06fdaa90ad7ca3106d6f72fbd01d16da006d2cf`
and
`registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24@sha256:bd10e66fb1d472705b1596f6ada00a8e7a8142f8d5c3b8480d745f5640e3207e`.
Another CPU architecture requires deliberately reviewed per-platform digests.

**Rationale**: These dependencies provide the approved transport, serialization, reactive
database, security, metrics, and logging behavior without registering undeclared health,
info, metrics, OpenAPI, or management routes.

**Alternatives considered**:

- Stock SmallRye Health, Quarkus Info, SmallRye OpenAPI, and the Prometheus endpoint extension:
  rejected for runtime because they register additional or method-unconstrained `/q` routes
  that conflict with the exact production route allowlist.
- SmallRye JWT alongside OIDC: rejected because one bearer-token mechanism is sufficient.

## Decision 3: Exact Operational Surface

**Decision**: Implement the five approved `/q` paths as application-owned Quarkus REST
resources, each with explicit `GET` and `HEAD`. Put non-application framework endpoints under
an unused internal root and disable Dev UI and undeclared extension endpoints in production.
The approved gateway exposes `/v1/*` to catalog consumers and `/q/*` only to the management
ingress. Runtime route inventory tests fail for any other `/v1` or `/q` route.

An application-owned, non-blocking Vert.x `@RouteFilter` executes before HTTP security and REST
routing. For the exact known path templates it passes only GET and HEAD and directly returns the
canonical `405` problem with `Allow: GET, HEAD` for every other recognized method, including
automatic OPTIONS. GET and HEAD then continue to OIDC and the REST pipeline. This prevents
Jakarta REST automatic OPTIONS generation and proves method rejection precedes authentication.

Health resources are custom and reactive:

- Liveness checks process viability only.
- Readiness checks PostgreSQL and the configured expected catalog revision.
- Startup remains down until configuration, schema compatibility, and expected revision pass.
- A down readiness/startup check returns the applicable RFC 9457 problem; successful checks
  return the contract health representation.
- Metrics are scraped from the application-owned Prometheus registry and returned only as
  OpenMetrics 1.0.
- Info is assembled per request so active catalog revision is not stale.

**Rationale**: This preserves the specification's exact five operational routes, permission
model, HEAD parity, RFC 9457 failures, and `405 Allow: GET, HEAD` behavior. It avoids silently
expanding scope to aggregate health, health groups, OpenAPI, UI, or default info routes.

**Alternatives considered**:

- Permit stock `/q/health` and extension routes: rejected because that changes approved scope.
- Secure extension routes only at the gateway: rejected because the runtime contract itself
  must prove undeclared routes are absent.

## Decision 4: Security Trust Profile

**Decision**: Use bearer JWT access tokens validated by Quarkus OIDC. The approved gateway is
the only ingress but the application independently validates signature, `iss`, service
audience `geographic-reference-service`, `exp`, `nbf` when present, and `sub`. V1 accepts
RS256 only. Permissions are space-delimited values in the `scope` claim:

- Catalog routes require `geographic-reference.read`.
- Operational routes require `geographic-reference.observe`.

OIDC issuer/discovery and JWKS location are deployment configuration. Opaque tokens, token
introspection fallback, trusted identity headers, and permission implication are disabled.
Missing or invalid tokens return `401`; valid tokens missing the exact route permission return
`403`. HTTP-layer path authorization executes before parameter conversion. Application-owned
authentication and authorization failure handlers serialize the canonical RFC 9457 response;
hermetic tests use the in-process Quarkus OIDC test server and generated RSA keys rather than a
live identity platform.

Startup has 30 seconds to load issuer metadata and a non-empty usable RS256 JWKS; startup and
readiness remain down until it succeeds. JWKS refresh runs every five minutes. Refresh failure
retains the last validated keys, records a metric/log, and keeps readiness up while that set is
usable. Unknown `kid` triggers one refresh per minute and returns invalid-token `401` if still
unknown. A successful refresh replaces keys atomically; no usable key set makes readiness down.

**Rationale**: Signed JWTs retain application-level validation while allowing gateway
authentication and avoid trusting spoofable forwarded identity headers.

**Alternatives considered**:

- Gateway-only trusted headers: rejected because direct-header spoofing and precedence cannot
  be proven safely.
- Opaque tokens: rejected because runtime introspection adds an external call to every trust
  decision and is not required.
- ES256 or multiple algorithms: deferred until the approved identity platform requires one;
  an allowlist of one algorithm minimizes downgrade and key-confusion risk.

**Governance**: The decision is recorded in
`docs/adr/0001-internal-jwt-trust-boundary.md` before implementation.

## Decision 5: OpenAPI and Problems

**Decision**: Use one canonical OpenAPI 3.1.1 document at
`specs/001-read-geographic-catalog/contracts/openapi.yaml` during design and promote the same
contract to `src/main/resources/META-INF/openapi.yaml` before endpoint implementation. The
contract contains exactly 16 paths and 32 operations: explicit `GET` and bodyless `HEAD` for
11 catalog and 5 operational paths. It declares no request bodies, writes, `OPTIONS`, `TRACE`,
callbacks, or webhooks.

Use environment-independent RFC 9457 type URNs:

`urn:problem-type:geographic-reference:<lowercase-error-suffix>`

Every problem includes `type`, `title`, `status`, safe `detail`, `instance`, stable `code`, and
`traceId`. Problems use `Cache-Control: no-store`. Recognized unsupported methods return the
specified problem and `Allow: GET, HEAD`; unknown paths and unrecognized method tokens remain
standard server behavior outside the resource contract.

**Rationale**: A URN is a stable absolute URI without inventing an organization-owned host.
Explicit HEAD operations avoid OpenAPI ambiguity about response bodies, and one contract lets
route-inventory tests prove the complete surface.

**Alternatives considered**:

- Annotation-generated OpenAPI: rejected because implementation could silently become the
  contract source.
- A placeholder HTTPS problem host: rejected because ownership is unknown.
- Multiple catalog and operations documents: rejected because a single route allowlist is
  easier to validate.

## Decision 6: HTTP Validation and HEAD

**Decision**: Use a weak representation validator:

`W/"grs-v1-<base64url-sha256>"`

The digest covers API representation version, operation, normalized path and query inputs,
effective UTC date, query mode, pagination, media type, authorization representation variant,
and catalog revision. Evaluate `If-None-Match` only after authentication, authorization,
validation, dependency checks, and resource resolution. Support wildcard and entity-tag
lists using weak comparison. A matching response returns `304` with ETag, cache control,
catalog revision, date, and trace headers and no body.

GET and HEAD execute the same reactive pipeline. HEAD returns the same status and headers and
no bytes; `Content-Length` is omitted unless it equals the GET payload length exactly.

**Rationale**: The validator represents semantic equality without falsely promising byte
identity across compression or serializer changes. Running the full query preserves required
error precedence and dependency visibility.

**Alternatives considered**:

- Strong ETag derived only from request dimensions: rejected because it would claim byte
  identity without hashing the final bytes.
- Evaluating cache preconditions before querying: rejected because it could hide `400`, `404`,
  `503`, and revision mismatch outcomes.
- Application or distributed cache: rejected because no measured need exists.

## Decision 7: Input Normalization

**Decision**:

- Parse request inputs as raw strings so framework conversion cannot bypass stable errors.
- Country alphabetic codes use locale-neutral uppercase; numeric codes remain strings.
- Canonical DPA values remain exact 2, 4, or 6 digit strings.
- Identifier scheme and ISO 3166-2 values use locale-neutral uppercase.
- Language tags are limited to 35 characters and validated/canonicalized by Java 25
  `Locale.Builder#setLanguageTag`; no ICU dependency is added.
- `nameType` remains uppercase-only.
- `asOf` uses strict ISO `YYYY-MM-DD`. Any syntactically valid explicit date, including a future
  date, applies the approved interval, lifecycle, dependency, and coverage rules.
- Unknown parameters are rejected before duplicate supported parameters; supported values are
  then validated in the contract's fixed order.

**Rationale**: These rules are deterministic, require no new library, and preserve the
specification's authentication, country, coverage, downstream-input, and resource precedence.

**Alternatives considered**:

- Automatic REST integer/date conversion: rejected because it produces framework-specific
  errors before application precedence.
- ICU4J: rejected because Java 25 supplies the approved v1 behavior and no broader locale
  requirement exists.

## Decision 8: Catalog Revision and Active Views

**Decision**: Persist immutable, revision-scoped snapshot rows. Every catalog row carries
`catalog_revision_id` and provenance. A validated revision becomes visible through one
singleton active-revision pointer switched as the final statement of the catalog migration.
Runtime queries read only `geographic_api` views that join the active pointer; inactive
candidates and Flyway history are invisible. Every database-backed statement receives the
configured expected revision as a bind value and compares it with the view revision.

The public catalog revision is `sha256:` followed by the approved digest of the RFC 8785 JCS
derived manifest defined in `catalog-source-manifest.md`. The concrete digest is generated,
reviewed, and recorded in the approval artifact before migration assembly. A separate
versioned relational-projection framing defined in `data-model.md` is hashed independently.
Source artifacts, mapping rules, row/object references, counts, legal approval reference,
migration version, projection digest, validation result, and exclusions are persisted.
Catalog metadata schemas remain revision-bound: initial hashes, dates, counts, and exclusions are
examples and activation assertions rather than OpenAPI `const` values, so a later approved
immutable revision within the fixed v1 type and identifier contract does not require a breaking
schema change.

**Rationale**: One atomic pointer switch prevents mixed catalogs. One PostgreSQL SELECT sees
one MVCC snapshot even if a new revision is activated concurrently.

**Alternatives considered**:

- Mutable `is_active` on every catalog row: rejected because multi-row activation can expose
  partial state.
- Choosing arbitrary archived catalog revisions through the API: rejected because `asOf`
  applies within the active approved snapshot and the contract exposes no revision selector.

## Decision 9: SQL Integrity Model

**Decision**:

- Use native PostgreSQL 18 UUIDv7; approved catalog migrations contain deterministic fixed
  UUIDv7 literals.
- Use generated half-open `daterange` values with strict `valid_until > valid_from` checks.
- Use append-only stable country/division identities plus global country-code,
  country-scoped division-code, and country/scheme identifier registries. Snapshot composite
  FKs include the complete registry mapping, so a code or identifier cannot be reassigned to a
  different logical identity in any retained revision.
- Use the trusted supplied `btree_gist` extension and partial exclusion constraints to prevent
  overlapping preferred names while permitting adjacent historical periods.
- Use an adjacency list with revision, country, and hierarchy level in composite keys. Parent
  level is exactly child level minus one; this makes cycles impossible without a trigger.
- Permit both `CANTONAL_SEAT_AREA` and `RURAL_PARISH` at level three by making type uniqueness
  country-and-code based.
- Normalize source display text to NFC and use PostgreSQL 18 Unicode case folding plus
  code-point tie breaking for deterministic name order.
- Persist approved identifier scheme definitions and enforce scheme-specific checks.
- Hash the relational projection with the version-2 logical framing in `data-model.md`. Stable
  lineage keys participate, while public manifest identity, UUIDs, migration/schema versions,
  principals, timestamps, generated storage values, and physical order do not.

**Rationale**: Declarative constraints remain concurrency-safe, reviewable, and the final
integrity boundary. They avoid custom triggers for rules PostgreSQL can prove directly.

**Alternatives considered**:

- Trigger-based preferred-name overlap checks: rejected because correct concurrent behavior
  requires extra locking and security-sensitive functions.
- Closure tables, `ltree`, materialized paths, or denormalized trees: rejected because the
  approved three-level adjacency model and bounded recursive query are sufficient.

**Governance**: No ADR is needed for `btree_gist` or the unchanged adjacency model. An ADR is
required only if the platform rejects the supplied extension or hierarchy persistence changes.

## Decision 10: Query Shape and Bounds

**Decision**: Every database-backed catalog query and database-dependent readiness, startup,
or info observation uses at most one fixed prepared SQL statement. Liveness, metrics, and
responses completed before database access use zero statements:

- Start from active expected revision and coverage.
- Return an outcome discriminator for country absent, coverage unavailable, resource absent,
  or revision mismatch when needed.
- Use page-number offset pagination with `LIMIT pageSize + 1`, default 50 and maximum 100; do
  not run a count query.
- Use lateral subqueries or ordered JSON aggregates for selected names and identifiers.
- Bound the recursive ancestor predicate itself to depth two and retain PostgreSQL `CYCLE`
  defense.
- Set a five-second login-and-database-specific statement timeout on the concrete runtime login;
  map timeout separately from dependency unavailability.

No capability currently justifies a multi-query read-only transaction.

**Rationale**: The catalog has 249 countries and 1,293 divisions, so bounded offset pagination
is simple and adequate. One statement guarantees response revision consistency and prevents
N+1 queries.

**Alternatives considered**:

- Total counts: rejected because the contract does not require them and a second query would
  complicate snapshots.
- Keyset pagination: rejected because the approved contract is page-number based and scale is
  bounded.
- Outer `LIMIT 2` on ancestors: rejected because recursion itself must be bounded.

## Decision 11: Roles, Flyway, and Activation

**Decision**: Before the initial migration, the platform pre-provisions only the target
database, Flyway history schema, and secret-managed `geographic_migrator` login with temporary
`CREATEROLE`. Initial Flyway V001 creates the NOLOGIN `geographic_owner` and
`geographic_runtime` roles. PostgreSQL's bootstrap-superuser-granted automatic creator rows have
`admin_option=true`, `inherit_option=false`, and `set_option=false`; a non-superuser creator
cannot remove or change them. V001 therefore leaves those temporary rows for privileged
finalization and uses the temporary authority only to add a separate non-admin, non-inherited
owner SET path required for object work. It adds no SET or INHERIT path to
`geographic_runtime`. V001 creates catalog objects as the owner and creates every initial object
grant. Immutable migrations contain no login role, password, or password hash.

After grouped V001-V003 succeeds, a privileged finalization one-shot executes all role,
ownership, grant, login, and login-default changes in one transaction under a deployment
advisory lock. It removes every temporary creator grant, creates the secret-managed runtime
login, installs only the exact final owner/runtime memberships, revokes `CREATEROLE`, transfers
the database and Flyway history ownership to `geographic_owner`, and retains for the migrator
only exact history-table privileges and non-admin owner SET membership. Assertions run before
commit. Failure at any injected stage rolls everything back; rerunning from the pre-finalization
or already validated final state is safe, while any other state aborts for investigation. Future
Flyway runs use the hardened migrator and `SET ROLE geographic_owner`. This makes recurring
identity provisioning least-privileged while satisfying the requirement that initial Flyway
migrations create catalog roles and grants.

Use pinned image
`docker.io/flyway/flyway:12.0.0@sha256:f14df737a680875d5e549e31b6dd0d980869be31501519037115796f37c6670f`
with these settings: versioned migrations only,
`validateOnMigrate=true`, `cleanDisabled=true`, `baselineOnMigrate=false`, `outOfOrder=false`,
`mixed=false`, transactional execution, and grouped initial migrations. Runtime contains no
Flyway, JDBC driver, migration SQL, or migration secret.

**Rationale**: Flyway creates the required non-login roles and object grants, while the platform
alone handles secret-bearing logins and removes one-time elevation. Password rotation remains
outside immutable SQL and recurring migration privilege is minimized.

**Alternatives considered**:

- Runtime Flyway: constitutionally prohibited.
- Gradle Flyway plugin: rejected because the migration image is closer to deployment and avoids
  Gradle 9 compatibility risk.
- Leaving the recurring migrator with `CREATEROLE`: rejected as excessive privilege.

## Decision 12: Runtime Grants

**Decision**: Revoke database and schema defaults from PUBLIC. Grant runtime only database
`CONNECT`, `USAGE` on `geographic_api`, and explicit `SELECT` on approved active-only views.
Revoke `TEMPORARY`, internal schema access, sequences, all internal tables, Flyway history,
DML, DDL, ownership, grant options, and privileged role membership. Revoke EXECUTE on
application-schema and mutation-capable routines, but not PostgreSQL built-ins required by
approved SELECT expressions. Set
`default_transaction_read_only=on` as defense in depth, not as the privilege boundary.
After the secret runtime login is created, finalization applies login-and-database session
defaults directly to that login: `default_transaction_read_only=on`, `statement_timeout=5s`,
and `search_path=geographic_api,pg_catalog`. Group-role settings are not relied on because
PostgreSQL does not inherit them at login.

**Rationale**: Views prevent accidental reads of staged revisions and explicit grants ensure
new objects are not exposed automatically.

**Alternatives considered**:

- `SELECT ON ALL TABLES` or default future grants: rejected because new data would bypass
  review.
- Read-only transaction setting alone: rejected because it can be changed by privileged users
  and does not replace grants.

## Decision 13: Catalog Migrations and Recovery

**Decision**: Plan these immutable migrations:

1. `V001__create_geographic_catalog.sql`: NOLOGIN owner/runtime roles, a temporary owner SET
   path pending privileged creator-grant cleanup, extension, schemas, types, stable-identity/registry,
   revision/provenance and snapshot tables, constraints, indexes, active-only views, PUBLIC
   revocations, and exact object grants.
2. `V002__load_initial_catalog_candidate.sql`: approved fixed source evidence, coverage,
   provenance, stable identities and append-only registry mappings, then the revision-scoped
   candidate snapshot that references those mappings.
3. `V003__validate_activate_initial_catalog.sql`: exact source/count checks, independently
   reproducible projection digest, legal approval validation, validation record, and final
   active-pointer switch.

Source archive hashes are verified before image assembly; the database never fetches network
data. V003 verifies all source and projection constants and raises exceptions on mismatch.
Transactional failure leaves the prior pointer untouched. Before production migration, create
and test the approved PITR/provider snapshot or PostgreSQL 18 restore point. Never edit applied
migrations or use automated Flyway `repair` as recovery.

**Rationale**: Schema, grants, and catalog activation are independently reviewable while the
grouped initial release remains atomic. Future catalog corrections use new migrations.

**Alternatives considered**:

- Repeatable catalog migrations: rejected because catalog revisions must remain immutable.
- Down migrations or manual pointer edits: rejected because recovery must be reviewed and
  deterministic.

## Decision 14: Delivery and Quadlet

**Decision**: Build three non-root OCI images:

- JVM runtime image with only the application and runtime configuration.
- Flyway migration image with only Flyway and `database/migration` SQL.
- Database role-management image based on the digest-pinned PostgreSQL 18 image, configured with
  an explicit non-root user and containing only `psql` plus reviewed prepare/finalize scripts.

Deploy three rootless Quadlet units. The migration unit is `Type=oneshot`, receives only the
migration secret, and remains successful after completion. A privileged finalization
`Type=oneshot` unit receives the administrator and new runtime-login secrets, `Requires` and
starts `After` migration, and commits the fail-atomic role hardening. The runtime unit `Requires`
and starts `After` successful finalization, receives only the runtime secret, uses
`StopTimeout=30`, and is promoted only after authenticated startup/readiness and catalog smoke
tests. The initial platform preparation uses the role-management image as a controlled stage
before migration. Migration never receives the runtime credential. Finalization transiently
receives administrator and runtime-login bootstrap secrets; runtime later receives that same
runtime credential but never receives administrator or migration secrets. No privileged unit
shares a process, image lifecycle, or elevated credential with runtime. ADR 0002 is normative.

**Rationale**: Systemd ordering makes migration failure block runtime startup and proves that
the long-running service never possesses migration credentials.

**Alternatives considered**:

- Migration in runtime `ExecStartPre`: rejected because it couples privileged and runtime
  lifecycles.
- Native image: constitutionally excluded.

## Decision 15: Verification and Supply Chain

**Decision**:

- Plain JUnit domain tests for value objects, lifecycle, temporal, and fallback rules.
- Application tests with fake query repositories and fixed error precedence.
- PostgreSQL 18 Testcontainers for migrations, prepared queries, ordering, hierarchy, query
  plans, runtime privileges, timeouts, and recovery.
- `@RunOnVertxContext` and `UniAsserter` reactive tests; subscription instrumentation verifies
  framework-owned subscription. A mandatory JUnit extension captures Vert.x
  `BlockedThreadChecker` warnings for event-loop threads and fails the test on the JUnit thread.
  Test-profile thresholds are 200 ms maximum event-loop execution and 50 ms warning/sampling;
  a separate expected-to-fail Gradle canary blocks an event loop for one second. Its JUnit XML
  is deleted before execution and must contain a current `<failure>` whose message begins
  `EXPECTED_BLOCKED_EVENT_LOOP_CANARY: BlockedThreadChecker`; an unrelated compile, startup,
  assertion, or infrastructure failure does not satisfy the canary gate.
- ArchUnit and source rules prohibit JDBC, ORM, runtime Flyway, blocking calls, manual
  subscription, mutation vocabulary, write annotations, and REST-to-database dependencies.
- OpenAPI validation, exact production route inventory, all-method exclusion, GET/HEAD parity,
  problem, security, ETag, and OpenMetrics contract tests.
- Spotless, SpotBugs, dependency locking, Gradle checksum verification, Trivy, Gitleaks,
  Trivy-generated CycloneDX SBOMs, non-root image inspection, and Quadlet validation in CI.

CI pins Trivy as
`docker.io/aquasec/trivy:0.72.0@sha256:cffe3f5161a47a6823fbd23d985795b3ed72a4c806da4c4df16266c02accdd6f`.
The same image generates filesystem/image CycloneDX documents and consumes them with
`trivy sbom`; no separate SBOM plugin or validator is required.

CI pins Gitleaks as
`docker.io/zricethezav/gitleaks:v8.30.1@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f`
and runs it without a host-installed scanner.

BlockHound is not selected because Java 25 and Quarkus 3.33 compatibility is not established;
the supported Vert.x blocked-thread checker plus failing canary provides the runtime detector.
Remove `mavenLocal()` so dependency resolution is reproducible.

**Rationale**: This test matrix directly proves every constitutional gate and avoids relying
on a green unit build as a substitute for architecture, privilege, contract, and deployment
evidence.

**Alternatives considered**:

- H2 or mocked persistence: rejected because PostgreSQL-specific constraints and SQL are core
  behavior.
- Native tests: constitutionally unnecessary.

## Decision 16: Performance and Scale

**Decision**: Record no latency, throughput, concurrency, or resource target in this plan.
Validate all bounds, inspect representative PostgreSQL plans, and collect production-ready
metrics, but do not add caches or resource tuning without an approved consumer workload.

**Rationale**: The specification explicitly provides no approved workload and the constitution
prohibits invented performance commitments. Data volume is fixed and small enough for the
approved bounded queries.

**Alternatives considered**:

- Invent a percentile latency target or add Redis/in-memory caching: rejected for lack of
  evidence.

## Decision 17: Legal and Source Gates

**Decision**: Planning documents may retain source titles, URLs, hashes, aggregate counts,
classification rules, and minimal anomaly/exclusion references needed for deterministic review.
Schema work and clearly synthetic fixture tests may proceed. Bulk extracted INEC rows, generated
catalog SQL, source-derived response examples, migration image content, and production promotion
remain blocked until written legal or data-governance approval is recorded. Debian LGPL notices,
source attribution, artifacts, hashes, extraction evidence, derived manifest, and approval
reference become migration inputs.

The approval is an explicit manual release-control gate. Its governed record identifies use and
scope, decision, approver, and date; an authorized release reviewer verifies it. Automation can
verify that the approved reference is present and bound to generated evidence, but a non-empty
operator string is never treated as proof of legal approval.

**Rationale**: This preserves the approved source scope without committing or distributing
data before its legal gate is satisfied.

**Alternatives considered**:

- Silently substitute another source or omit legal evidence: rejected by the specification.
