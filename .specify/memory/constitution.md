<!--
Sync Impact Report
- Version change: 1.0.0 -> 2.0.0
- Bump rationale: MAJOR because runtime mutation, administration, import, publication,
  lifecycle-command, event-publication, and application-managed migration responsibilities
  are removed and the runtime becomes permanently read-only.
- Modified principles:
  - I. Global Geographic System of Record -> I. Read-Only Global Geographic Reference Service
  - II. Approved Technology Baseline -> II. Runtime Technology Baseline
  - III. Pure Clean Architecture -> III. Pure Clean Architecture for Queries
  - IV. End-to-End Reactive Execution -> IV. Reactive Read Execution
  - V. Domain and Data Integrity -> V. Reference-Data Integrity and Temporal Semantics
  - VI. Migration-Controlled PostgreSQL -> VI. Controlled SQL Catalog Maintenance
  - VII. Contract-First Integration -> VII. Query-Focused Contract-First API
  - VIII. Security, Auditability, and Provenance -> VIII. Read Access, Audit, and Provenance
  - IX. Test-First Verification -> IX. Test-First Query and Migration Verification
  - X. Observable, Measured Operations -> X. Observable and Bounded Read Operations
  - XI. Reproducible JVM Delivery -> XI. Separated Migration and JVM Delivery
  - XII. Simplicity and Explicit Decisions -> XII. Simplicity and Explicit Decisions
- Added sections:
  - Permanent runtime read-only boundary with GET, HEAD, and required OPTIONS only
  - SQL-only catalog maintenance through immutable Flyway migrations
  - Separate migration and SELECT-only runtime PostgreSQL identities
  - Query lifecycle, temporal, caching, pagination, hierarchy, and error semantics
  - Deployment ordering and catalog revision readiness requirements
- Removed sections and concepts:
  - Write-oriented Data and Transactions guidance
  - Import-, publication-, and event-oriented Contracts and Releases guidance
  - Runtime state-changing commands and write transactions
  - Administrative, catalog-management, import, publication, and bulk-upload endpoints
  - Application-managed lifecycle transitions and caller-populated audit identities
  - Optimistic write concurrency, If-Match mutation semantics, and command idempotency
  - Transactional outbox, integration-event publication, and message consumers
  - Runtime Flyway execution and runtime JDBC migration datasource
- Templates and guidance requiring updates:
  - ✅ updated .specify/templates/plan-template.md
  - ✅ updated .specify/templates/spec-template.md
  - ✅ updated .specify/templates/tasks-template.md
  - ✅ updated affected .agents/skills/speckit-*/SKILL.md guidance
  - ✅ updated README.md
  - ✅ reviewed remaining installed Spec Kit skills; no conflicting project guidance found
- Migration requirements for existing implementation or documentation:
  - Repository scan found no application Java sources, HTTP routes, persistence adapters,
    Flyway startup configuration, or JDBC datasource to migrate in this amendment.
  - Any implementation added after ratification MUST exclude runtime write routes,
    mutation use cases or repositories, scheduled catalog writers, message consumers,
    startup Flyway execution, and runtime JDBC datasources.
  - Provision separate migration and runtime database identities and prove runtime
    INSERT, UPDATE, and DELETE rejection before the first runtime data feature is promoted.
  - Move every catalog change to a reviewed, immutable Flyway SQL migration and align
    OpenAPI, tests, deployment documentation, and operations with query-only behavior.
  - README.md now documents the read-only runtime and external migration boundary.
- Follow-up TODOs: none
-->
# Geographic Reference Service Constitution

**Status**: active

## Core Principles

### I. Read-Only Global Geographic Reference Service

Geographic Reference Service MUST be the independently deployable system of record for
global geographic reference data and MUST expose that data exclusively through read-only
HTTP endpoints. The runtime application MUST support only safe and idempotent query
operations. Application HTTP methods MUST be limited to `GET`, `HEAD`, and `OPTIONS` when
infrastructure or protocol behavior requires it.

The application MUST NOT expose `POST`, `PUT`, `PATCH`, or `DELETE` endpoints;
administrative or catalog-management endpoints; lifecycle-transition commands; import,
publication, or bulk-upload endpoints; database-maintenance endpoints; generic CRUD
endpoints; or hidden or undocumented mutation endpoints. No endpoint, scheduled
application job, message consumer, or application use case MAY insert, update, delete,
activate, deprecate, retire, or otherwise modify geographic catalog records.

The absence of runtime writes is a permanent architectural boundary, not a version-one
scope decision. A future proposal for runtime mutation requires a constitutional MAJOR
amendment, an approved ADR, a new threat model, a new database-permission model, explicit
transactional and audit requirements, and review of bounded-context ownership.

The service MUST own countries and ISO-recognized territories; ISO 3166-1 alpha-2,
alpha-3, and numeric codes; political-administrative division types and hierarchies;
stable canonical division codes; external identifier schemes; localized, official,
common, alternative, and historical names; lifecycle status; temporal validity; and
source authority, provenance, and revision.

The service MUST NOT own postal or physical addresses, contact information, customer or
organization coordinates, geocoding, reverse geocoding, postal delivery rules, tax
jurisdictions, commercial territories, sales regions, service coverage, branches,
warehouses, tenants, subscriptions, users, organizations, organization-specific
configuration or permissions, or customer-specific geographic information. Postal codes
MUST NOT enter this bounded context without an approved specification and constitutional
review.

Geographic data MUST remain global and shared. Catalog tables MUST NOT contain
`tenant_id`. Other services MUST reference published logical identifiers, such as ISO
country codes or canonical division codes, and MUST NOT create database foreign keys to
this service's database. This service MUST own its database schema exclusively.

### II. Runtime Technology Baseline

Application source MUST use Java 25. Builds MUST use the committed Gradle Wrapper,
`build.gradle.kts`, and `settings.gradle.kts`; developers and CI MUST invoke
`./gradlew`. Kotlin MUST remain limited to Gradle build scripts.

The project MUST use an approved Quarkus LTS release at the latest approved patch,
Quarkus REST, Jackson, Mutiny, Hibernate Reactive with the Panache repository pattern or
justified Vert.x Reactive PostgreSQL Client queries, PostgreSQL 18, Flyway,
repository-stored OpenAPI, JVM deployment, OCI containers, and rootless Podman Quadlet.

Runtime database access MUST remain fully reactive and non-blocking. JDBC MAY be used
only by the external Flyway migration process. The runtime application MUST NOT configure
or use a JDBC datasource.

A framework, database, broker, cache, protocol, or infrastructure product MUST NOT be
added without a confirmed requirement and documented architectural justification.
Blocking Hibernate ORM, runtime JDBC, Redis, Kafka, RabbitMQ, Elasticsearch, OpenSearch,
GraphQL, PostGIS, event-sourcing infrastructure, CQRS infrastructure, distributed
transactions, XA, and native compilation MUST NOT be introduced speculatively.

### III. Pure Clean Architecture for Queries

Dependencies MUST point inward through:

1. Domain.
2. Application.
3. Inbound adapters.
4. Outbound adapters.
5. Infrastructure.

The Domain layer MUST contain immutable or behavior-rich read-domain concepts, value
objects, validation rules, hierarchy semantics, lifecycle and temporal interpretation,
and domain exceptions. It MUST be synchronous, deterministic, and independent of
infrastructure. Public construction MUST NOT create invalid domain values.

The Domain layer MUST NOT depend on Quarkus, Jakarta REST, Jakarta Persistence,
Hibernate, Panache, Vert.x, PostgreSQL, Flyway, Jackson, OpenAPI, transport DTOs,
persistence entities, configuration or logging frameworks, or Mutiny inside domain
entities or value objects. Business-significant primitives SHOULD be value objects,
including `Alpha2Code`, `Alpha3Code`, `NumericCountryCode`,
`CanonicalDivisionCode`, `LanguageTag`, `IdentifierSchemeCode`, `ValidityPeriod`,
`SourceAuthority`, and `SourceRevision`, because their normalization and validation are
domain rules.

The Application layer MUST contain query use cases, query input ports, repository output
ports, result models, authorization decisions for reads, application validation, and
query orchestration. It MUST NOT contain commands that mutate geographic data. Its
vocabulary SHOULD use `Query`, `Find`, `Get`, `List`, `Search`, `Resolve`, `Browse`, and
`Navigate` because these names reveal read intent.

Application vocabulary MUST NOT introduce mutation-oriented use cases named `Create`,
`Update`, `Delete`, `Activate`, `Deprecate`, `Retire`, `Import`, `Publish`, `Apply`, or
`Approve`. The Application layer MAY depend on Mutiny, but MUST NOT depend on Quarkus
runtime APIs, REST resources, Hibernate sessions, Panache repositories, Vert.x clients,
PostgreSQL-specific classes, Flyway, transport DTOs, or persistence entities.

Application query use cases performing I/O MUST return Mutiny `Uni<T>`. Ordinary bounded
lists MUST return `Uni<PageResult<T>>`. `Multi<T>` MUST be limited to a confirmed
streaming requirement and MUST NOT be used for ordinary relational pagination.

Inbound REST adapters MUST invoke application query ports and MUST NOT access Hibernate
Reactive repositories or PostgreSQL clients directly. Outbound persistence adapters MUST
implement domain-oriented query repository ports. Persistence entities and REST DTOs
MUST remain separate from domain and application models. Repository ports MUST NOT
expose generic CRUD, mutation methods, unbounded `findAll()`, HQL, SQL, Panache queries,
or persistence entities.

Architecture tests MUST enforce dependency direction and all read-only rules. They MUST
fail for domain framework imports, REST-to-persistence access, persistence entities
exposed through REST, Panache domain entities, mutation use cases or repositories, write
REST methods, runtime JDBC, startup Flyway execution, or direct resource-to-persistence
dependencies. The service SHOULD remain one deployable Gradle module with package-level
boundaries unless independent compilation has a demonstrated benefit.

### IV. Reactive Read Execution

All runtime I/O MUST be non-blocking. Runtime database access MUST use Hibernate Reactive
or the Vert.x Reactive PostgreSQL Client.

The application MUST NOT use blocking Hibernate ORM, runtime JDBC,
`await().indefinitely()`, `Future.get()`, `CompletionStage.get()`, `Thread.sleep(...)`,
`Thread.join()`, manual `subscribe().with(...)` in resources, use cases, or repositories,
or blocking file or network operations on event-loop threads. Reactive pipelines MUST be
returned to Quarkus for subscription, and failures MUST propagate through those
pipelines.

Queries using one Hibernate Reactive session MUST sequence session operations safely.
Concurrent work MUST NOT share one reactive session. Ordinary queries SHOULD use
explicit reactive sessions when required by the persistence adapter.

Write transactions are not part of the runtime application. A read-only transaction MAY
be used only when a query requires a consistent multi-query snapshot and the plan
documents why one statement is insufficient. The application MUST NOT maintain database
transactions across external calls or long-running work.

Reactive errors MUST retain the original cause, a stable application error code, trace
correlation, a safe external response, and non-confidential diagnostic context.

### V. Reference-Data Integrity and Temporal Semantics

PostgreSQL constraints MUST remain the final integrity boundary even though writes occur
only through controlled migrations. SQL migrations MUST preserve valid ISO country-code
formats, country-code uniqueness, canonical division-code uniqueness, same-country
parent relationships, valid hierarchy-level transitions, valid root levels, cycle
prohibition, external identifier uniqueness, preferred-name uniqueness, temporal-range
validity, source provenance, and historical identifier non-reuse.

The fact that data is loaded through scripts MUST NOT justify removing database
constraints. Migration tests MUST prove invalid reference data is rejected. The full
hierarchy MUST NOT be loaded as one aggregate or read model merely to enforce integrity.
A country MUST NOT load all divisions for ordinary query behavior.

Lifecycle and temporal fields MAY remain because they describe reference-data state and
history even though the application cannot modify them:

- `DRAFT` records MUST be treated as migration-stage data and MUST NOT be returned by
  normal runtime queries.
- `ACTIVE` records MUST be returned by current-catalog queries.
- `DEPRECATED` records MAY be returned only according to explicit endpoint and temporal
  semantics.
- `RETIRED` records MUST be excluded from ordinary current-catalog listings but MAY be
  resolved by approved historical queries.

Temporal validity MUST use half-open intervals: `valid_from` is inclusive,
`valid_until` is exclusive, and null `valid_until` means no known end. Periods MUST
satisfy `valid_until IS NULL OR valid_from IS NULL OR valid_until > valid_from`.
Current-data queries MUST apply lifecycle and temporal rules consistently. Historical
queries MUST use an explicit `asOf` date or an explicitly historical endpoint.

Internal database identifiers MUST be UUIDs; UUID version 7 SHOULD be used when
PostgreSQL 18 support and project tooling make it practical. Internal UUIDs MUST NOT
automatically become preferred public identifiers. Public references SHOULD use stable
domain codes. Every public code's normalization, character set, semantics, and
uniqueness scope MUST be specified. Retired codes MUST NOT be silently reused, and
historical resolution MUST follow approved temporal rules.

### VI. Controlled SQL Catalog Maintenance

PostgreSQL MUST be the persisted source of truth for geographic reference data.
Countries, administrative divisions, localized names, identifiers, lifecycle statuses,
temporal validity, and source provenance MUST be loaded and modified exclusively through
reviewed, version-controlled SQL scripts. All schema and reference-data changes MUST use
immutable Flyway migrations. Previously applied migrations MUST NOT be edited. Every
correction or catalog revision MUST use a new migration.

Migration names SHOULD reveal their immutable sequence and purpose, for example:

- `V001__create_geographic_schema.sql`
- `V002__load_iso_3166_country_catalog.sql`
- `V003__load_ecuador_administrative_divisions.sql`
- `V004__apply_iso_3166_revision_2026_01.sql`
- `V005__apply_ecuador_dpa_revision_2026_06.sql`

Large datasets MAY be stored in separate repository-controlled SQL or CSV resources when
Flyway executes and validates them deterministically.

Catalog scripts MUST be deterministic, repeatable from a clean database, reviewed
through source control, traceable to an official or approved internal source, validated
before production execution, tested against PostgreSQL 18, executed atomically when the
revision requires all-or-nothing behavior, accompanied by an explicit recovery strategy,
and protected against partial catalog activation. Scripts MUST be idempotent only when
explicitly designed as repeatable migrations.

The project MUST NOT rely on manually edited production rows. Direct ad hoc SQL against
production MUST NOT be the normal catalog-maintenance process. Emergency corrections
MUST still produce a repository migration immediately and MUST follow the approved
incident and change-management process.

The runtime application database identity MUST have read-only access. It MUST receive
only privileges required for approved queries, normally `CONNECT` on the geographic
database, `USAGE` on the geographic schema, `SELECT` on approved tables or views, and
`USAGE` or `SELECT` on a sequence only when genuinely necessary for a read.

The runtime role MUST NOT have `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, `CREATE`,
`ALTER`, `DROP`, `REFERENCES`, `TRIGGER`, `EXECUTE` on mutation-capable
procedures, ownership of schemas, tables, sequences, or functions, or membership in
privileged PostgreSQL roles.

Flyway and catalog migrations MUST execute with a separate migration identity available
only to the controlled deployment or migration process. The application runtime MUST
NOT receive the migration credential and MUST NOT run Flyway automatically at startup
using its runtime identity.

Migrations SHOULD execute as a dedicated CI/CD stage or controlled one-shot deployment
unit before the new application version becomes ready because this isolates elevated
credentials from the long-running service. A Podman Quadlet deployment MAY run
migrations through a dedicated one-shot systemd or container unit using the migration
identity. The application service MUST start with the read-only runtime identity only
after migrations complete successfully.

### VII. Query-Focused Contract-First API

The canonical OpenAPI contract MUST be committed and updated before endpoint
implementation. The API MUST contain only read operations. Expected capabilities MAY
include:

- Listing active countries.
- Resolving a country by alpha-2, alpha-3, or numeric ISO code.
- Retrieving localized and historical country names.
- Listing administrative-division types for a country.
- Listing root administrative divisions or direct children of a division.
- Retrieving a division by canonical code.
- Resolving a division by an external identifier.
- Retrieving ancestors or bounded descendants.
- Searching divisions with approved name-search semantics.
- Querying historical validity through an approved `asOf` parameter.

The contract MUST define paths, query parameters, validation, bounded pagination,
maximum page size, sorting, filtering, language negotiation or explicit language
parameters, temporal query semantics, HTTP caching behavior, error responses, versioning,
access requirements, and examples.

The API MUST NOT contain write request schemas, define idempotency keys for read
operations, or define optimistic write concurrency using `If-Match`. ETag MAY be used
only for HTTP cache validation of read responses. When ETag is used, `GET` MAY accept
`If-None-Match`, an unchanged resource SHOULD return `304 Not Modified` to avoid
transferring an identical representation, and ETag MUST NOT imply caller update rights.
`Last-Modified` and `If-Modified-Since` MAY be used when reliable catalog revision dates
exist.

HTTP errors MUST use RFC 9457 `application/problem+json` with a stable problem type,
title, status, safe detail, machine-readable application error code, request or instance
reference when appropriate, and trace or correlation identifier. Responses MUST NOT
expose stack traces, SQL, credentials, or confidential configuration.

The contract MUST define stable errors for invalid country code format, country not
found, administrative division not found, identifier not found, invalid language tag,
unsupported identifier scheme, invalid temporal query date, invalid pagination, a page
size above the configured maximum, invalid hierarchy depth, temporary database
unavailability, and unauthorized or forbidden access when authentication applies.

Write-specific errors MUST NOT appear, including optimistic concurrency conflict, stale
update, duplicate creation, invalid lifecycle transition, idempotency-key conflict,
import failure, or publication conflict.

### VIII. Read Access, Audit, and Provenance

Read-only MUST NOT be interpreted as automatically public. The first API specification
MUST explicitly decide the access model. The service SHOULD be internal by default to
minimize its network and consumer trust boundary unless an approved architecture exposes
it externally. External exposure MUST pass through the approved F5, reverse proxy, or
gateway.

No application administrative permission exists because there are no administrative
endpoints. When authentication is required, the application SHOULD use a read permission
such as `geographic-reference.read` because authorization vocabulary MUST match the
runtime capability.

The service MUST NOT define runtime permissions such as
`geographic-reference.create`, `geographic-reference.update`,
`geographic-reference.delete`, `geographic-reference.import`,
`geographic-reference.publish`, or `geographic-reference.manage`.

Database credentials MUST be supplied through approved secret-management mechanisms.
Secrets MUST NOT be committed. Logs MUST NOT expose credentials, tokens, credential-
bearing connection strings, secret keys, confidential infrastructure information, or
full confidential payloads.

Runtime HTTP queries MUST NOT update audit columns. When retained, `created_by` and
`updated_by` MUST identify the controlled migration, deployment principal, or catalog
maintenance process that produced the row version and MUST NOT represent an HTTP caller.
Controlled values MAY include `flyway`, `catalog-migration`,
`deployment:<pipeline-id>`, or `source:<authority>:<revision>`.

Source provenance MUST remain persisted with catalog records. Every catalog data
migration MUST identify, directly or through associated metadata, the source authority,
source reference, source revision, effective date when applicable, repository migration
version, and checksum or source digest when available.

A runtime audit trail for successful `GET` requests is not required unless an approved
security or regulatory requirement demands it. Operational access logs MAY record route,
status, duration, caller identity, and trace identifier, but MUST NOT duplicate complete
response payloads.

### IX. Test-First Query and Migration Verification

The project MUST implement test-first verification for query behavior. Required tests
MUST initially demonstrate missing behavior when practical and MUST pass before a feature
is complete.

Domain tests MUST cover code normalization, language-tag validation, temporal validity
interpretation, lifecycle visibility, hierarchy-navigation rules, and identifier
resolution semantics.

Application tests MUST cover query orchestration, not-found behavior, filter and
pagination validation, language fallback, temporal query behavior, reactive failure
propagation, and security checks when applicable.

Persistence integration tests MUST use real PostgreSQL 18 or an approved ephemeral
PostgreSQL 18 instance. They MUST cover reactive reads, recursive hierarchy queries,
identifier and name resolution, bounded pagination, query indexes and plans when
performance-sensitive, database-role read-only enforcement, and rejection of `INSERT`,
`UPDATE`, and `DELETE` with the runtime credential.

Migration tests MUST cover clean database creation, initial catalog loading, upgrade from
the previous supported schema and dataset revision, constraint enforcement, atomic
failure behavior, recovery behavior, catalog provenance, and deterministic data counts
or checksums when appropriate.

API contract tests MUST prove only `GET`, `HEAD`, and required `OPTIONS` operations are
exposed; `POST`, `PUT`, `PATCH`, and `DELETE` are unavailable; runtime behavior matches
OpenAPI; pagination is bounded; RFC 9457 errors are returned; cache validation works when
enabled; and security behavior matches the contract.

Architecture tests MUST prove Clean Architecture dependency direction, the absence of
mutation use cases and mutation repository methods, the absence of write REST methods,
the absence of runtime JDBC and application-startup Flyway execution, and the absence of
direct resource-to-persistence dependencies.

Reactive tests MUST detect event-loop blocking, manual subscription, blocking database
access, reactive session misuse, and unhandled failures. Native tests MUST NOT be
required unless an approved constitutional amendment and ADR change the runtime.

### X. Observable and Bounded Read Operations

The service MUST expose liveness, readiness, startup checks when required, structured
JSON logs, trace correlation, metrics, graceful shutdown, version metadata, application
build revision, and catalog dataset revision when available. Health endpoints MUST NOT
expose secrets. Readiness MUST fail when approved queries cannot be served safely or the
expected schema or catalog revision is unavailable. Liveness MUST NOT fail solely
because a temporary dependency is unavailable.

Metrics SHOULD include request count, request duration, error count, database pool
utilization, reactive connection acquisition duration, query count by route category,
not-found count, and current catalog revision because these signals describe the
runtime's actual responsibilities. Import, publication, command, and write-transaction
metrics MUST NOT exist in the runtime.

Performance requirements MUST derive from approved workloads and MUST NOT be invented.
All collection endpoints MUST use bounded pagination, and maximum page sizes MUST be
contractually defined. Recursive hierarchy queries MUST have explicit depth and result
bounds. The application MUST NOT expose unbounded `findAll()` behavior.

Indexes MUST correspond to actual query patterns. Caching MUST NOT be introduced without
measurement. HTTP cache validation SHOULD be evaluated before Redis or another
distributed cache because the API is read-only and protocol caching is simpler.

If a local in-memory cache is proposed, it MUST define invalidation based on catalog
revision or application restart, maximum size, expiration, consistency implications,
resource limits, benchmark evidence, and an ADR when materially architectural.
Denormalized hierarchy structures, closure tables, materialized paths, or `ltree` MUST
NOT be introduced without evidence and an ADR.

### XI. Separated Migration and JVM Delivery

The deployment pipeline MUST follow this order:

1. Validate SQL migrations.
2. Back up or establish the approved recovery point when required.
3. Execute Flyway with the migration identity.
4. Verify schema and catalog integrity.
5. Start or restart the application with the read-only runtime identity.
6. Execute readiness and smoke tests.
7. Promote traffic through the approved F5, proxy, or gateway.

Migration failure MUST prevent application promotion. The application MUST NOT become
ready when its expected schema or catalog revision is unavailable.

Production MUST run the Java 25 JVM artifact as a long-running service. Native
compilation MUST NOT be adopted by preference. A future native-runtime proposal requires
a constitutional amendment, an ADR, JVM/native benchmarks, compatibility checks, native
integration tests, and operational acceptance.

The Gradle Wrapper MUST be committed. Dependencies SHOULD use the Quarkus platform BOM
where supported to keep the approved stack coherent. Versions MUST NOT use `+`,
`latest.release`, or unbounded ranges. Upgrades MUST include compatibility, test,
migration when applicable, and security review. The build MUST be reproducible, MUST
retain or generate an approved software bill of materials, and MUST remove unused
dependencies.

The application MUST remain independently deployable as an OCI-compatible, non-root JVM
container through version-controlled rootless Podman Quadlet. The container MUST persist
no business data locally, MUST receive only runtime secrets through approved mechanisms,
MUST expose only required ports, MUST terminate gracefully, and MUST include build
revision metadata. Ports MUST NOT be published to untrusted networks without approval.
Database ports MUST NOT be public.

### XII. Simplicity and Explicit Decisions

The simplest architecture satisfying approved query requirements MUST be preferred. The
project MUST NOT introduce speculative interfaces, generic abstractions without multiple
concrete uses, generic CRUD layers, premature microservices, messaging, caching,
denormalization, native compilation, multi-module decomposition, or framework wrappers
without domain value. Possible future needs are insufficient justification.

The repository MUST maintain accurate documentation for its README, architecture, C4
context and container diagrams, API contract, database model, SQL catalog migration
strategy, database identities, security model, deployment instructions, operational
runbook, ADR directory, local-development guide, and testing guide. A change that
invalidates documentation MUST update it in the same change.

English MUST remain the canonical language for source code, database objects, API
contracts, configuration, logs, migrations, and technical documentation. Current
behavior and proposed intent MUST be distinguished; future designs MUST be labeled
proposed.

An ADR MUST document a constitutional-default change, major infrastructure, new
datastore, caching, native runtime, hierarchy persistence change, API versioning change,
new security trust boundary, material migration strategy, or accepted architectural
risk. It MUST include context, decision, alternatives, consequences, risks, validation
criteria, and a reversal strategy when applicable. An ADR MUST NOT override a
constitutional MUST or MUST NOT without a constitutional amendment.

## Engineering Standards

### Specification and Query Design

Specifications MUST focus on consuming-system query stories and observable read
behavior. They MUST declare scope, non-goals, actors, read-access policy, scenarios,
testable requirements, measurable success criteria, query errors, pagination, filtering,
localization, temporal behavior, query consistency or snapshot semantics when needed,
HTTP cache validation, contract impact, migration impact, performance, deployment, and
documentation impact.

Every specification MUST contain an explicit Read-Only Enforcement section. It MUST state
that only `GET`, `HEAD`, and required `OPTIONS` are exposed; `POST`, `PUT`, `PATCH`, and
`DELETE` are not exposed; and catalog mutation occurs only through controlled SQL
migrations. SQL catalog changes MUST be described as migration impact, not application
functionality.

Plans MUST translate approved query requirements without changing scope. Every
`plan.md` MUST contain the mechanical Constitution Check below before research and again
after design. Tasks MUST be dependency-ordered, test-first, and traceable to query
requirements and consuming-system scenarios. Implementation MUST NOT add capability
absent from the approved specification.

### Catalog Change Control

Schema or catalog changes MUST be delivered only as immutable Flyway migrations.
Migration tests MUST precede migration implementation. Plans and tasks MUST cover source
provenance, atomicity, recovery, deterministic validation, and separate database
identities whenever a feature changes schema or reference data.

Application features MUST NOT model SQL catalog maintenance as an HTTP command,
application use case, scheduled job, message consumer, import workflow, or publication
workflow.

### Query Consistency and Caching

One query statement SHOULD provide a response snapshot when practical because it avoids
unnecessary session and transaction complexity. A multi-query read-only transaction MAY
be planned only for a documented consistent-snapshot requirement.

Pagination, hierarchy depth, and result counts MUST remain bounded. Cache semantics MUST
be explicit even when caching is not required. HTTP validation SHOULD be evaluated
before application or distributed caching.

### Language and Documentation

English MUST be canonical for every source, database, contract, configuration, log,
migration, and technical-documentation artifact. Documentation MUST describe the current
runtime as query-only and MUST NOT imply administrative, import, publication, or mutation
capabilities.

## Quality Gates and Workflow

### Specification Readiness

Implementation planning MUST NOT proceed while material ambiguity remains. Before task
generation, a feature MUST have query-focused scenarios, explicit scope and non-goals,
testable functional requirements, measurable evidence-based success criteria, defined
read access, error behavior, read-only enforcement, bounded query behavior, lifecycle
and temporal visibility, applicable localization and caching semantics, contract and
migration impact, and no unresolved high-severity contradiction.

`tasks.md` MUST NOT be generated while blocking requirements remain or while any
Constitution Check item is `FAIL`. A `FAIL` requires constitutional compliance or an
explicit constitutional amendment; an ADR or local exception is insufficient.

### Required Verification

Before merge, the project MUST pass compilation; domain and application query tests;
PostgreSQL 18 persistence integration tests; database-role privilege tests; OpenAPI
contract and HTTP-method exclusion tests; architecture tests; migration tests when
schema or catalog data changes; reactive tests; static analysis; dependency
vulnerability scanning; secret scanning; formatting checks; container build; and
deployment-manifest validation. A green build MUST NOT substitute for compliance with
the approved specification and this constitution.

### Constitution Check

Every plan MUST copy this checklist and record `PASS`, `FAIL`, or `N/A` with concrete
evidence before research and after design:

- [ ] The capability is a geographic query inside the bounded context.
- [ ] Only `GET`, `HEAD`, or required `OPTIONS` endpoints are introduced.
- [ ] No `POST`, `PUT`, `PATCH`, `DELETE`, mutation job, or message consumer is
      introduced.
- [ ] Runtime PostgreSQL access is reactive and non-blocking.
- [ ] Runtime PostgreSQL credentials have SELECT-only privileges.
- [ ] Flyway and catalog SQL execute outside the runtime application identity.
- [ ] Clean Architecture dependency direction is preserved.
- [ ] Application ports and repositories expose only query operations.
- [ ] OpenAPI is updated before implementation.
- [ ] Pagination, depth, and result sizes are bounded.
- [ ] Lifecycle and temporal visibility are defined.
- [ ] Localization and fallback behavior are defined where applicable.
- [ ] RFC 9457 query errors are defined.
- [ ] HTTP caching behavior is defined or explicitly not required.
- [ ] PostgreSQL query and migration tests use PostgreSQL 18.
- [ ] SQL catalog changes are immutable, reviewed, traceable, and recoverable.
- [ ] Database constraints continue to enforce reference-data integrity.
- [ ] Architecture tests prohibit write endpoints and mutation use cases.
- [ ] Reactive tests prohibit blocking and manual subscriptions.
- [ ] Deployment separates migration and runtime database identities.
- [ ] Observability, security, documentation, and operational changes are covered.
- [ ] No speculative messaging, cache, native build, or other infrastructure is added.

Any `FAIL` MUST block task generation unless this constitution is amended.

## Governance

This constitution MUST govern every specification, plan, task, implementation, review,
database migration, API contract, test, deployment artifact, and architectural decision
in this repository. It supersedes conflicting local practice. Reviews MUST verify
constitutional compliance, and non-compliance with a MUST or MUST NOT MUST block
approval. A SHOULD or SHOULD NOT MAY be bypassed only with documented justification.

An exception to a SHOULD or SHOULD NOT MUST identify the principle, reason, alternatives,
risk, compensating controls, required approval, and a removal or review date when
temporary. A MUST or MUST NOT conflict requires a constitutional amendment. An ADR alone
MUST NOT override it.

Amendments MUST be explicit and reviewable and MUST include the version change,
ratification and amendment dates, change summary, affected templates or documentation,
and compliance or migration actions for existing work. A specification, implementation,
or ADR MUST NOT silently amend this constitution.

Constitution versions MUST use semantic versioning:

- **MAJOR** MUST be used for incompatible governance or architectural-principle changes.
- **MINOR** MUST be used for a new mandatory principle or materially expanded guidance.
- **PATCH** MUST be used for clarification that does not change intent.

Each amendment MUST update the Sync Impact Report and all affected templates, Spec Kit
skills, and guidance in the same change. Compliance MUST be checked during specification
readiness, planning, task generation, implementation, review, migration review, and
release approval.

**Project**: Geographic Reference Service | **Status**: active

**Version**: 2.0.0 | **Ratified**: 2026-07-23 | **Last Amended**: 2026-07-24
