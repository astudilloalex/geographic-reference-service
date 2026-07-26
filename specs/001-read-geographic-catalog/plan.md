# Implementation Plan: Read Geographic Catalog API

**Git Branch**: `1-ft-1` | **Feature Context**: `001-read-geographic-catalog` | **Date**: 2026-07-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from
`/specs/001-read-geographic-catalog/spec.md`

## Summary

Build one independently deployable Java 25 service that exposes the activated geographic
catalog through the 11 approved read-only catalog paths and five approved operational paths.
Consumers resolve 249 ISO-aligned countries and the bounded Ecuador catalog of 1,293 divisions
without database coupling. The runtime uses Quarkus REST, Mutiny, OIDC, and fixed prepared SQL
through the Vert.x Reactive PostgreSQL Client. Every database-backed catalog response uses at
most one PostgreSQL statement and one active catalog revision; liveness, metrics, and
pre-database failures use none. No runtime component can mutate data.

PostgreSQL 18 stores immutable revision-scoped snapshots, source evidence, deterministic
validation, and one atomic active-revision pointer. A separate non-root Flyway 12.0.0 image
applies immutable SQL with a migration identity; a privileged, fail-atomic finalization one-shot
then removes temporary role authority and installs the runtime login before the rootless Quadlet
runtime can start with a SELECT-only identity. Contract, architecture, reactive, migration,
privilege, security, container, and deployment tests provide the evidence required by the
constitution.

## Technical Context

**Language/Version**: Java 25; Kotlin only in Gradle build scripts

**Build**: Gradle Wrapper 9.3.1 with Kotlin DSL and Quarkus platform BOM `3.33.2.1` exist now.
Implementation removes `mavenLocal()`, enables strict dependency locking and SHA-256
verification metadata, and pins CI actions by commit before dependency implementation.

**Primary Dependencies**: `quarkus-arc`, `quarkus-rest-jackson`,
`quarkus-reactive-pg-client`, `quarkus-oidc`, `quarkus-micrometer`,
`micrometer-registry-prometheus`, `quarkus-logging-json`, and Mutiny supplied by Quarkus.
Do not add Hibernate ORM, Hibernate Reactive Panache, runtime JDBC, runtime Flyway,
SmallRye Health, Quarkus Info, or runtime OpenAPI endpoint extensions.
An application CDI producer owns the Prometheus registry and the custom `/q/metrics` resource
scrapes it as OpenMetrics; no extension-owned metrics route is enabled.

**Storage**: PostgreSQL 18 with native UUIDv7, `daterange`, supplied `btree_gist`, immutable
revision-scoped snapshots, append-only stable-identity/code registries, active-only runtime
views, and versioned Flyway SQL

**Runtime Database Identity**: Secret-managed login that belongs only to the NOLOGIN
`geographic_runtime` role. It receives database `CONNECT`, `USAGE` on `geographic_api`, and
explicit `SELECT` on approved active-only views. It receives no internal schema, table,
sequence, application-routine, Flyway history, temporary-object, ownership, grant, DML, or DDL
privilege. PostgreSQL built-ins needed by approved SELECT expressions remain usable.

**Migration Execution**: Pinned external Flyway 12.0.0 OCI image with an initial
`geographic_migrator` login that has temporary `CREATEROLE`. V001 creates the NOLOGIN object
owner and runtime privilege role, accepts PostgreSQL's temporary unchangeable creator-admin rows,
adds only the owner SET path needed for object work, and creates all initial object grants. After
grouped V001-V003 succeeds, a privileged fail-atomic finalization one-shot creates the exact
non-admin memberships, removes migration role-admin authority, revokes migrator `CREATEROLE`,
transfers required ownership, and minimizes recurring privileges; future migrations use
`SET ROLE geographic_owner`. Runtime startup requires successful finalization, never runs
Flyway, and has no JDBC datasource.

**Testing**: JUnit 5 domain and application tests; Quarkus REST and OIDC tests; Vert.x
`UniAsserter` reactive tests with a fail-on-Vert.x-blocked-thread extension and expected-to-fail
blocking canary; PostgreSQL 18 Testcontainers integration, migration, query-plan,
and privilege tests; OpenAPI 3.1.1 and production route inventory tests; ArchUnit dependency
and prohibition rules; Spotless and SpotBugs; container and Quadlet validation; Trivy,
Gitleaks, and Trivy-generated CycloneDX SBOM gates

**Supply-Chain Scanner**: Pin
`docker.io/aquasec/trivy:0.72.0@sha256:cffe3f5161a47a6823fbd23d985795b3ed72a4c806da4c4df16266c02accdd6f`
for filesystem/image scanning and CycloneDX generation/consumption, and pin
`docker.io/zricethezav/gitleaks:v8.30.1@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f`
for Git secret scanning

**Pinned Verification Images**:
`docker.io/library/postgres:18@sha256:d93de42662696f278fb34354b06fdaa90ad7ca3106d6f72fbd01d16da006d2cf`
and
`registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24@sha256:bd10e66fb1d472705b1596f6ada00a8e7a8142f8d5c3b8480d745f5640e3207e`.
Other architectures require separately reviewed digests.

**Target Platform**: Initial `linux/amd64` Java 25 JVM fast-jar in a non-root OCI container,
using the digest-pinned UBI 9 OpenJDK 25 runtime and deployed through a version-controlled
rootless Podman Quadlet runtime unit. Separate non-root migration and privileged-finalization
one-shot units run first; runtime requires successful finalization.

**OIDC Availability**: Startup/readiness require initial issuer metadata and at least one RS256
JWK within 30 seconds. Keys refresh every five minutes; temporary refresh failure retains the
last usable set and emits telemetry, unknown keys trigger a one-minute rate-limited refresh,
and an empty usable set makes readiness down. ADR 0001 is normative.

**Project Type**: One Gradle module and one independently deployable reactive read-only
reference-data service, with package-level Clean Architecture boundaries

**Performance Goals**: No latency, throughput, concurrency, or resource target is approved by
the specification. Do not invent one. Prove fixed bounds, inspect PostgreSQL 18 plans, publish
the required metrics, and collect workload evidence before adding performance commitments,
resource tuning, or caching.

**Constraints**: Production exposes exactly `GET` and `HEAD` for 16 declared paths and no
application `OPTIONS`; all runtime I/O is non-blocking; each database-backed observation uses
at most one fixed prepared SELECT; page size defaults to 50 and is capped at 100; hierarchy
depth is three and ancestors
are capped at two; access is JWT-authenticated and permission-scoped; SQL catalog maintenance
is external; OpenAPI is contract-first; JVM delivery only; no messaging, distributed or local
cache, search engine, geospatial extension, native build, or multi-module split

**Scale/Scope**: 249 countries, 433 English country names, 24 provinces, 222 cantons, 222
cantonal-seat areas, 825 rural parishes, 1,293 total divisions, 1,293 Spanish division names,
1,293 DPA identifiers, 24 ISO 3166-2 identifiers, four division type codes over three levels,
two languages across their declared coverage, current and explicit historical date dimensions,
11 catalog query patterns, and five operational observations. Request volume and read
concurrency are not yet approved workload inputs.

**Catalog Revision/Provenance**: Country source is Debian `iso-codes` 4.20.1 with archive
SHA-256 `5d551f3ddb32548c4321e9011720fd97751af0107592f79ebffc939bd32f2268`.
Division source is INEC Clasificador Geografico Estadistico 2026 with archive SHA-256
`9a2962bcccd88745dba4d61627e27945714049d97571d088e6c6b294be668a2c`.
The public revision is `sha256:` followed by the approved SHA-256 of the RFC 8785 JCS derived
manifest. The concrete digest and independently reproducible relational-projection digest are
approval artifacts defined by `catalog-source-manifest.md` and `data-model.md`. V003 verifies
both, pinned source evidence, exact counts, legal approval, and schema compatibility before
switching the active pointer. Readiness requires the configured expected revision.
The projection digest uses version-2 logical framing over lineage keys and approved values; it
excludes public manifest identity, UUIDs, migration/schema versions, principals, timestamps,
generated storage values, and physical order.
OpenAPI models catalog metadata as revision-bound fields; exact initial hashes, dates, counts,
and exclusions remain source/migration assertions and examples rather than schema constants that
would block a later approved revision within the fixed v1 type and identifier contract.

**Query Consistency**: Every database-backed catalog query and database-dependent readiness,
startup, or info observation uses at most one prepared PostgreSQL statement. Active-only views
expose the active revision; each statement compares it with the configured expected-revision
bind value. Lists use `LIMIT pageSize + 1`, not a second count query. Items use lateral selected-
name and ordered identifier aggregates. Ancestors use a recursive CTE whose recursion predicate
stops at depth two. Liveness and metrics use zero database statements. No capability justifies a
multi-query read-only transaction.

## Constitution Check

*GATE: Every item is evaluated from the approved specification before research and again from
the completed Phase 1 design. Any future design change that turns a row to FAIL blocks task
generation and requires compliance or a constitutional amendment.*

| Gate | Pre-Research | Post-Design | Evidence |
|------|--------------|-------------|----------|
| The capability is a geographic query inside the bounded context | PASS | PASS | `spec.md` Scope; `data-model.md` sections 1 and 13 |
| Only `GET`, `HEAD`, or required `OPTIONS` endpoints are introduced | PASS | PASS | `spec.md` RO-001; `contracts/openapi.yaml` has exactly 32 GET/HEAD operations and no OPTIONS |
| No `POST`, `PUT`, `PATCH`, `DELETE`, mutation job, or message consumer is introduced | PASS | PASS | `spec.md` Non-Goals and RO-002/003; `research.md` decisions 1 and 15; route and architecture tests in `quickstart.md` |
| Runtime PostgreSQL access is reactive and non-blocking | PASS | PASS | `spec.md` QC; `research.md` decisions 1 and 10; Vert.x reactive adapter design |
| Runtime PostgreSQL credentials have SELECT-only privileges | PASS | PASS | `spec.md` RO-004/DR-011; `data-model.md` section 18; privilege probes in `quickstart.md` |
| Flyway and catalog SQL execute outside the runtime application identity | PASS | PASS | `spec.md` RO-005; `research.md` decisions 11, 13, and 14; separate image and Quadlet design |
| Clean Architecture dependency direction is preserved | PASS | PASS | Constitution principle III; package tree below; ArchUnit plan in `research.md` decision 15 |
| Application ports and repositories expose only query operations | PASS | PASS | Explicit query ports below; no generic CRUD, mutation, persistence type, or SQL leakage |
| OpenAPI is updated before implementation | PASS | PASS | `spec.md` CR-001; completed canonical design contract `contracts/openapi.yaml` |
| Pagination, depth, and result sizes are bounded | PASS | PASS | `spec.md` QR-001/003; OpenAPI bounds; `data-model.md` sections 15 and 16 |
| Lifecycle and temporal visibility are defined | PASS | PASS | `spec.md` LR-001 through LR-008; `data-model.md` sections 7 and 12 |
| Localization and fallback behavior are defined where applicable | PASS | PASS | `spec.md` QR-004; `data-model.md` section 9.3; language parameters and schemas in OpenAPI |
| RFC 9457 query errors are defined | PASS | PASS | `spec.md` ER; OpenAPI `Problem` and 23 stable problem codes/type URNs |
| HTTP caching behavior is defined or explicitly not required | PASS | PASS | `spec.md` HC; `research.md` decision 6; contract ETag/304 headers and tests |
| PostgreSQL query and migration tests use PostgreSQL 18 | PASS | PASS | `spec.md` DR-009; `research.md` decision 15; PostgreSQL 18/Testcontainers guide |
| SQL catalog changes are immutable, reviewed, traceable, and recoverable | PASS | PASS | `catalog-source-manifest.md`; `research.md` decisions 13 and 17; `data-model.md` sections 4, 17, and 20 |
| Database constraints continue to enforce reference-data integrity | PASS | PASS | `spec.md` DR-003 through DR-009; `data-model.md` sections 5 through 8 and validation matrix |
| Architecture tests prohibit write endpoints and mutation use cases | PASS | PASS | `spec.md` Read-Only Evidence; `research.md` decision 15; `quickstart.md` quality gates |
| Reactive tests prohibit blocking and manual subscriptions | PASS | PASS | `research.md` decision 15; `UniAsserter`, fail-on-Vert.x-blocked-thread extension, expected-to-fail blocking canary, subscription instrumentation, and static prohibitions |
| Deployment separates migration and runtime database identities | PASS | PASS | `spec.md` OR-009; `research.md` decisions 11 and 14; planned Quadlet units and separate secrets |
| Observability, security, documentation, and operational changes are covered | PASS | PASS | `spec.md` SR/OR/Documentation; decisions 3-6 and 11-15; ADRs 0001/0002; documentation deliverables below; contract and quickstart operations |
| No speculative messaging, cache, native build, or other infrastructure is added | PASS | PASS | `spec.md` Non-Goals; `research.md` decisions 1, 6, 9, and 16; dependency allowlist above |

**Gate result**: PASS before research and PASS after Phase 1 design. No constitutional
exception or amendment is required. The gateway/JWT trust boundary is recorded in accepted
ADR 0001. The database role-administration/finalization trust boundary and migration strategy are
recorded in accepted ADR 0002. Both implement, rather than bypass, constitutional requirements.

## Read-Only and Migration Design

### HTTP Surface

The canonical contract contains exactly the following paths. Every row has explicit `GET` and
matching bodyless `HEAD`. Recognized `POST`, `PUT`, `PATCH`, `DELETE`, and application `OPTIONS`
receive `405` with `Allow: GET, HEAD`; no write or OPTIONS operation exists in OpenAPI or route
registration.

| Permission | Path | Capability |
|------------|------|------------|
| `geographic-reference.read` | `/v1/catalog` | Activated revision and coverage metadata |
| `geographic-reference.read` | `/v1/countries` | Bounded country list |
| `geographic-reference.read` | `/v1/countries/{countryCode}` | Country resolution |
| `geographic-reference.read` | `/v1/countries/{countryCode}/names` | Bounded country names |
| `geographic-reference.read` | `/v1/countries/{countryCode}/division-types` | Ecuador division types |
| `geographic-reference.read` | `/v1/countries/{countryCode}/divisions` | Ecuador root divisions |
| `geographic-reference.read` | `/v1/countries/{countryCode}/divisions/{canonicalCode}` | Canonical division resolution |
| `geographic-reference.read` | `/v1/countries/{countryCode}/division-identifiers/{schemeCode}/{identifierValue}` | External identifier resolution |
| `geographic-reference.read` | `/v1/countries/{countryCode}/divisions/{canonicalCode}/names` | Bounded division names |
| `geographic-reference.read` | `/v1/countries/{countryCode}/divisions/{canonicalCode}/children` | Direct children |
| `geographic-reference.read` | `/v1/countries/{countryCode}/divisions/{canonicalCode}/ancestors` | At most two ancestors |
| `geographic-reference.observe` | `/q/health/live` | Process liveness |
| `geographic-reference.observe` | `/q/health/ready` | Database and expected-revision readiness |
| `geographic-reference.observe` | `/q/health/started` | Configuration/schema/revision startup |
| `geographic-reference.observe` | `/q/metrics` | OpenMetrics 1.0 exposition |
| `geographic-reference.observe` | `/q/info` | Safe build, catalog, and coverage information |

Application-owned operational resources are deliberate: stock health, info, metrics, OpenAPI,
and UI extensions would expose undeclared paths or incompatible methods. Production route
inventory is a release gate. A non-blocking Vert.x `@RouteFilter` runs before HTTP security and
REST routing for the exact known path templates. It passes only GET/HEAD and directly emits the
canonical method problem for all other recognized methods, suppressing automatic Jakarta REST
OPTIONS and proving `405 Allow: GET, HEAD` before authentication.

### Query Ports and Repositories

Application input ports expose only approved query intentions:

- `CatalogQueries`: `getCatalog(GetCatalogQuery)`.
- `CountryQueries`: `listCountries(ListCountriesQuery)`,
  `resolveCountry(ResolveCountryQuery)`, and
  `listCountryNames(ListCountryNamesQuery)`.
- `DivisionQueries`: `listDivisionTypes(ListDivisionTypesQuery)`,
  `listRootDivisions(ListRootDivisionsQuery)`,
  `resolveDivision(ResolveDivisionQuery)`,
  `resolveDivisionIdentifier(ResolveDivisionIdentifierQuery)`,
  `listDivisionNames(ListDivisionNamesQuery)`, `listChildren(ListChildrenQuery)`, and
  `listAncestors(ListAncestorsQuery)`.

Single-item I/O returns `Uni<T>`; bounded collections return `Uni<PageResult<T>>`; ancestors
return `Uni<List<DivisionSummary>>` with an invariant maximum of two. Application query values
contain normalized raw inputs, effective UTC date, query mode, authorization context, and
pagination but no REST, Vert.x, SQL, or persistence type.

Output ports mirror those explicit intentions as `CatalogQueryRepository`,
`CountryQueryRepository`, and `DivisionQueryRepository`. The PostgreSQL adapter implements each
method with one named prepared SQL statement and maps rows/outcome discriminators to application
results. Ports expose no generic `findAll`, mutation method, SQL, row, pool, or persistence
entity. REST resources call only input ports; they never inject the PostgreSQL adapter or pool.

Operational checks live in infrastructure because they describe process/database/catalog
viability rather than geographic domain behavior. Database-dependent readiness, startup, and
info observations use at most one bounded reactive statement; liveness and metrics use none.

### Runtime Role

Initial platform bootstrap creates only the database, Flyway history schema, and secret-managed
migration login with temporary `CREATEROLE`; it does not create catalog roles or the runtime
login. Flyway V001 creates the NOLOGIN owner/runtime roles and exact object grants without
embedding a password:

- Revoke database and schema privileges from `PUBLIC`, including `TEMPORARY`.
- Grant runtime database `CONNECT` only.
- Grant `USAGE` on `geographic_api` only.
- Grant `SELECT` explicitly on reviewed active revision, metadata, country, name, type,
  division, scheme, identifier, and provenance views.
- Grant no sequence privilege. Revoke EXECUTE on application-schema and mutation-capable
  routines while retaining PostgreSQL built-ins needed by approved SELECT expressions.
- Revoke all access to internal `geographic`, Flyway history, inactive data, and future views.
- Set `default_transaction_read_only=on` as defense in depth.
- Apply `default_transaction_read_only=on`, `statement_timeout=5s`, and
  `search_path=geographic_api,pg_catalog` directly to the secret runtime login during
  finalization; group-role settings are not inherited at connection time.
- Verify runtime owns no database, schema, relation, sequence, routine, or type; has no grant
  option or privileged membership; and cannot `SET ROLE geographic_owner`.

Positive tests execute every approved prepared SELECT. Negative tests execute real DML, DDL,
temporary object, routine, ownership, role, and grant probes in addition to privilege-catalog
inspection. Both are required because privilege predicates alone are insufficient.

### Migration Role and Ordering

The platform supplies the initial elevated migration login. Flyway V001 creates
`geographic_owner` and `geographic_runtime`. PostgreSQL's automatic creator rows temporarily
retain `admin_option=true`, `inherit_option=false`, and `set_option=false` because the
non-superuser creator cannot change them. V001 adds a separate non-admin, non-inherited owner SET
path and creates all initial object grants; it adds no runtime SET/INHERIT path. V001-V003 run
externally as one grouped release. After success, a privileged finalization one-shot removes all
temporary creator grants and installs exactly the migrator-owner and runtime-login/runtime rows,
revokes migrator `CREATEROLE`, transfers database and Flyway history ownership to
`geographic_owner`, and retains only exact Flyway history privileges plus owner SET membership
for the migrator. All changes and final assertions share one advisory-locked transaction; failure
rolls back, a corrected rerun is safe, and runtime requires its successful systemd result. Future
Flyway runs use the hardened migrator and `SET ROLE geographic_owner`.

Deployment order is:

1. Verify source hashes, derived manifest, legal approval, image digests, SQL naming, and
   migration checksums.
2. Establish and test the approved PostgreSQL 18 recovery point.
3. Run the one-shot migration unit with only the migration secret.
4. Verify Flyway history, constraints, counts, projection digest, grants, and active revision.
5. Run the required finalization one-shot to create/rotate the exact runtime membership, remove
   all migration role-admin authority, revoke one-time migrator `CREATEROLE`, transfer required
   ownership, apply runtime-login session defaults, and atomically prove/commit the final roles,
   memberships, ownership, and privileges.
6. Start the JVM runtime unit with only the runtime database secret and expected revision.
7. Execute authenticated startup, readiness, privilege, contract, and catalog smoke checks.
8. Promote catalog and management ingress traffic.

A failure at any step prevents subsequent startup or promotion. Transactional migration failure
retains the previous active pointer; a finalization failure rolls back all role/ownership/grant
changes and is rerun only after correction. Failure-injection tests cover every finalization
stage. The first release remains unready. Catalog recovery uses the approved restore point or a
new reviewed forward migration, never an edited applied migration, ad hoc pointer update, or
automated Flyway `repair`.

### SQL Catalog Impact

Planned versioned migration files are:

- `database/migration/V001__create_geographic_catalog.sql`: NOLOGIN owner/runtime roles,
  temporary owner SET path pending privileged creator-grant cleanup, `btree_gist`, schemas,
  types, stable identity and append-only registry tables, control and snapshot tables,
  declarative constraints, indexes, active-only views, PUBLIC revocations, and exact object
  grants.
- `database/migration/V002__load_initial_catalog_candidate.sql`: fixed UUIDv7 source evidence,
  coverage, provenance, stable identities and registry mappings, and complete revision-scoped
  candidate snapshot referencing those mappings.
- `database/migration/V003__validate_activate_initial_catalog.sql`: exact source/count checks,
  independently reproducible projection digest, legal approval validation, passing validation
  rows, deferred constraint evaluation, and final singleton pointer switch.

V002 and V003 are assembled and committed only after the source/legal gate passes. Source archives are
never fetched by PostgreSQL and are not included in the runtime image. The migration image
contains only pinned Flyway and approved SQL. Every future correction uses a new versioned
migration.

### Query Bounds and Indexes

- Page defaults to 1 with size 50; maximum size is 100; lists fetch at most 101 rows and return
  no total count.
- Ecuador hierarchy has three levels. Root and direct-child queries never recurse. Ancestor
  recursion stops within the recursive predicate at depth two and retains `CYCLE` defense.
- Fixed order is alpha-2 for countries; level then type code; canonical DPA code for divisions;
  exact language/name-type/preference/Unicode fold/original NFC for names; and immediate parent
  to root for ancestors.
- Fixed filters are the capability matrix in `spec.md` and OpenAPI; unknown, repeated, or
  dynamic sort/filter expressions are rejected.
- Unique code indexes support country and division resolution. Parent, name, external
  identifier, source/provenance, coverage, and active pointer indexes map directly to the 11
  catalog paths as documented in `data-model.md` section 15.
- Query-plan tests verify bounded work, no N+1 pattern, and index availability. A sequential
  scan is acceptable when PostgreSQL 18 cost evidence favors it for the fixed small dataset;
  tests do not require brittle plan shapes.
- No text-search, default-name search, closure-table, materialized-path, partitioning,
  geospatial, cache, or speculative index is introduced.

## Project Structure

### Documentation (this feature)

```text
specs/001-read-geographic-catalog/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── catalog-source-manifest.md
├── requirements-review.md
├── quickstart.md
├── checklists/
│   └── requirements.md
├── contracts/
│   └── openapi.yaml
└── tasks.md                         # Phase 2 output, not created by this command
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/alexastudillo/geographicreference/
│   │   ├── domain/
│   │   │   ├── catalog/
│   │   │   ├── country/
│   │   │   ├── division/
│   │   │   └── shared/
│   │   ├── application/
│   │   │   ├── port/in/
│   │   │   ├── port/out/
│   │   │   ├── query/
│   │   │   └── result/
│   │   ├── adapter/in/rest/
│   │   │   ├── resource/
│   │   │   ├── dto/
│   │   │   ├── mapper/
│   │   │   ├── problem/
│   │   │   └── validation/
│   │   ├── adapter/out/postgresql/
│   │   │   ├── repository/
│   │   │   ├── mapper/
│   │   │   └── sql/
│   │   └── infrastructure/
│   │       ├── configuration/
│   │       ├── security/
│   │       ├── http/
│   │       ├── observability/
│   │       └── operations/
│   ├── resources/
│   │   ├── META-INF/openapi.yaml
│   │   └── application.properties
│   └── docker/
│       ├── Dockerfile.jvm
│       ├── Dockerfile.flyway
│       └── Dockerfile.role-management
├── test/java/com/alexastudillo/geographicreference/
│   ├── domain/
│   ├── application/
│   ├── adapter/
│   ├── contract/
│   ├── architecture/
│   └── support/
└── test/resources/
    ├── application.properties
    └── fixtures/                    # synthetic only until legal approval

database/
├── bootstrap/
│   ├── prepare-initial-migrator.sql
│   └── finalize-runtime-login.sql
└── migration/
    ├── V001__create_geographic_catalog.sql
    ├── V002__load_initial_catalog_candidate.sql
    └── V003__validate_activate_initial_catalog.sql

deploy/quadlet/
├── geographic-reference-migration.container
├── geographic-reference-finalization.container
├── geographic-reference-runtime.container
└── README.md

docs/
├── adr/0001-internal-jwt-trust-boundary.md
├── adr/0002-transactional-database-role-finalization.md
├── architecture/
├── database/
├── deployment/
├── operations/
├── security/
└── testing.md
```

**Structure Decision**: Keep one deployable Gradle module and enforce Clean Architecture with
packages and ArchUnit, not compile-time submodules. Domain is synchronous and framework-free;
application depends only on domain and may expose `Uni` at I/O boundaries; inbound REST maps
transport to query ports; outbound PostgreSQL implements output ports; infrastructure wires
security and operations. SQL migrations stay under top-level `database/` and are copied only
into the migration image, so they cannot enter the runtime artifact. The design OpenAPI is
copied byte-for-byte to `src/main/resources/META-INF/openapi.yaml` before endpoint code and is
validated against runtime routes.

### Documentation Deliverables

Implementation is not complete until the same change updates every affected current-behavior
document:

- `README.md`: distinguish current scaffold from implemented query-only behavior, external
  migration order, local validation, and supported coverage.
- `docs/architecture/geographic-reference-service-v1.drawio`: show catalog and management
  ingress, runtime, migration/finalization one-shots, PostgreSQL, separate identities, revision
  readiness, and promotion ordering; mark proposed versus implemented state accurately.
- `docs/database/v1-schema.dbml`: align with the normative Phase 1 model, including revision
  scope, nullable independence, strict temporal ends, multiple level-three types, schemes,
  provenance, active pointer, and removal of optimistic runtime mutation semantics.
- `docs/database/roles-and-privileges.md` and `docs/database/migration-strategy.md`: initial
  temporary role elevation and automatic creator grants, V001 role/grant creation, atomic
  finalization, hardening, active views, source evidence, activation, recovery, and accepted ADR
  0002.
- `docs/security/read-access.md`: accepted ADR 0001, OIDC/JWT profile, permissions, ingress,
  error precedence, secret handling, and log exclusions.
- `docs/deployment/rootless-quadlet.md`: images, secrets, one-shot ordering, ports, startup,
  shutdown, and promotion.
- `docs/operations/runbook.md`: revision mismatch, dependency failure/recovery, migration
  failure, restore point, metrics, logs, and smoke checks.
- `docs/local-development.md` and `docs/testing.md`: synthetic-fixture path, legal gate, external
  migration, PostgreSQL 18, contract, reactive, privilege, and supply-chain verification.

`data-model.md` is the normative proposed design during planning; it does not silently replace
the existing DBML. The DBML and architecture become current only when implementation and their
required updates are delivered together.

## Phase 0 and Phase 1 Artifacts

- [research.md](research.md) records the resolved persistence, security, contract, cache,
  database, migration, delivery, testing, legal, and performance decisions.
- [data-model.md](data-model.md) defines the normative revision-scoped relational and public
  read models, constraints, indexes, activation lifecycle, grants, and validation matrix.
- [contracts/openapi.yaml](contracts/openapi.yaml) is the validated OpenAPI 3.1.1 design with
  exactly 16 paths and 32 GET/HEAD operations.
- [quickstart.md](quickstart.md) is the post-implementation validation guide and distinguishes
  currently available scaffold commands from required implementation outputs.

No unresolved technical clarification remains. The implementation phase must not generate or
commit source-derived INEC examples or SQL until the legal/data-governance approval required by
the source manifest is recorded.

## Complexity Tracking

No constitutional SHOULD or SHOULD NOT deviation is requested. The explicit Vert.x SQL adapter
is an approved baseline option justified by recursive and projection-heavy read queries. The
application-owned operational endpoints are required to satisfy the exact approved route and
method contract. `btree_gist` is a supplied PostgreSQL constraint extension used for a confirmed
temporal-integrity requirement, and hierarchy persistence remains the approved adjacency list.
