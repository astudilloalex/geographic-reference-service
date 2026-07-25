<!--
Sync Impact Report
- Version change: unratified template -> 1.0.0
- Modified principles:
  - Placeholder Principle 1 -> I. Global Geographic System of Record
  - Placeholder Principle 2 -> II. Approved Technology Baseline
  - Placeholder Principle 3 -> III. Pure Clean Architecture
  - Placeholder Principle 4 -> IV. End-to-End Reactive Execution
  - Placeholder Principle 5 -> V. Domain and Data Integrity
- Added principles:
  - VI. Migration-Controlled PostgreSQL
  - VII. Contract-First Integration
  - VIII. Security, Auditability, and Provenance
  - IX. Test-First Verification
  - X. Observable, Measured Operations
  - XI. Reproducible JVM Delivery
  - XII. Simplicity and Explicit Decisions
- Added sections:
  - Engineering Standards
  - Quality Gates and Workflow
  - Constitution Check
  - Governance metadata and amendment rules
- Removed sections:
  - Unfilled template placeholders and example comments
- Propagation:
  - ✅ updated .specify/templates/plan-template.md
  - ✅ updated .specify/templates/spec-template.md
  - ✅ updated .specify/templates/tasks-template.md
  - ✅ updated .agents/skills/speckit-specify/SKILL.md
  - ✅ updated .agents/skills/speckit-tasks/SKILL.md
  - ✅ updated .agents/skills/speckit-implement/SKILL.md
  - ✅ updated README.md
  - ✅ reviewed remaining installed Spec Kit skill definitions; no conflicting
    agent-specific or constitutional guidance found
- Follow-up TODOs: none
-->
# Geographic Reference Service Constitution

**Status**: active

## Core Principles

### I. Global Geographic System of Record

Geographic Reference Service MUST be the independently deployable system of record for
global geographic reference data. It MUST own countries and ISO-recognized territories;
ISO 3166-1 alpha-2, alpha-3, and numeric codes; political-administrative division types
and hierarchies; stable canonical division codes; external identifier schemes; localized,
official, common, alternative, and historical names; lifecycle status; temporal validity;
source authority, provenance, and revision; and, when approved, catalog releases and
controlled imports.

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

### II. Approved Technology Baseline

Application source MUST use Java 25. Builds MUST use the committed Gradle Wrapper,
`build.gradle.kts`, and `settings.gradle.kts`; developers and CI MUST invoke
`./gradlew`. Kotlin MUST be limited to Gradle build DSL unless this constitution is
amended.

The runtime baseline MUST use the approved Quarkus LTS release and latest approved patch,
Quarkus REST, Jackson, Mutiny, Hibernate Reactive with Panache repositories, the Vert.x
Reactive PostgreSQL Client, PostgreSQL 18 or the explicitly approved project baseline,
Flyway, repository-stored OpenAPI contracts, Podman-compatible OCI images, and rootless
Podman Quadlet artifacts.

A framework, database, broker, cache, protocol, or infrastructure product MUST NOT be
added without a confirmed requirement and documented architectural justification.
Blocking Hibernate ORM, runtime JDBC, Redis, Kafka, RabbitMQ, Elasticsearch, OpenSearch,
GraphQL, PostGIS, event-sourcing infrastructure, CQRS infrastructure, distributed
transactions, XA, and native compilation MUST NOT be introduced speculatively. Flyway
MAY use an isolated JDBC datasource only for migrations; business use cases and runtime
persistence adapters MUST NOT use JDBC.

### III. Pure Clean Architecture

Dependencies MUST point inward through Domain, Application, Inbound Adapters, Outbound
Adapters, and Infrastructure or framework configuration.

The Domain layer MUST contain behavior-rich aggregates, entities, value objects, domain
services, policies, invariants, exceptions, and only required domain events. It MUST be
synchronous, deterministic, and independent of infrastructure. It MUST NOT depend on
Quarkus, Jakarta REST or Persistence, Hibernate, Panache, Vert.x, PostgreSQL, Flyway,
Jackson, OpenAPI, security or logging frameworks, transport DTOs, persistence entities,
configuration classes, or Mutiny types in entities or value objects. Public constructors
MUST NOT create invalid domain objects. Business-significant primitives SHOULD be value
objects, including CountryCode, Alpha2Code, Alpha3Code, NumericCountryCode,
CanonicalDivisionCode, LanguageTag, IdentifierSchemeCode, ValidityPeriod,
SourceAuthority, and SourceRevision.

The Application layer MUST own use cases, commands, queries, services, input and output
ports, transaction orchestration, authorization decisions, application validation, and
application-to-domain mapping. It MAY depend on Mutiny. It MUST NOT depend on Quarkus
runtime APIs, REST resources, Hibernate sessions, Panache repositories, Vert.x clients,
PostgreSQL-specific classes, Flyway, transport DTOs, or persistence entities.

Inbound adapters MUST translate external inputs to application commands or queries.
Outbound adapters MUST implement application ports. Framework concerns MUST remain in
adapters. REST resources MUST NOT contain business rules or access persistence directly.
Persistence adapters MUST NOT decide domain policy. Transport DTOs and persistence
entities MUST NOT be domain models.

Architecture tests MUST enforce package and dependency rules and MUST fail the build for
inward-dependency violations, domain framework imports, REST-to-persistence access,
persistence entities exposed through REST, Panache domain entities, direct repository or
session injection into use cases, or other prohibited coupling. The service SHOULD begin
as one deployable Gradle module with package-level boundaries. A multi-module build MUST
NOT be introduced solely to mirror layers.

### IV. End-to-End Reactive Execution

All runtime I/O in the request path MUST be non-blocking end to end and MUST use Mutiny
consistently. Asynchronous application use cases and output ports MUST return `Uni<T>` or
`Uni<Void>`. `Multi<T>` MUST be used only for genuine streaming. Ordinary listings MUST
use bounded pagination and `Uni<PageResult<T>>`.

Event-loop threads MUST NOT execute blocking JDBC, blocking file operations, synchronous
network or SDK calls, thread sleeps, or unbounded CPU-intensive computation. Application
and adapter code MUST NOT use `await().indefinitely()`, `CompletionStage.get()`,
`Future.get()`, `Thread.sleep(...)`, `Thread.join()`, manual blocking locks around
reactive flows, or manual `subscribe().with(...)` in resources, use cases, or
repositories. Fire-and-forget work MUST NOT be used without an approved reliability
mechanism. Pipelines MUST be returned to Quarkus for subscription, and failures MUST
propagate through them.

Reactive errors MUST retain the original cause, a stable application error code, trace
correlation, a safe external response, and non-confidential diagnostic context.
Operations sharing a Hibernate Reactive session or transaction MUST be sequenced.
Concurrent operations MUST NOT share a session. Parallel persistence composition in one
session MUST NOT be used unless isolation and session behavior are proven safe.

### V. Domain and Data Integrity

Aggregate boundaries MUST follow transactional consistency, not foreign-key shape. The
full hierarchy MUST NOT be one aggregate, and a country MUST NOT load all divisions for
ordinary invariants. Expected aggregate candidates are Country,
AdministrativeDivisionType, AdministrativeDivision, and, when introduced,
CatalogRelease and ImportRun. A division MAY reference its parent by identity.

Domain validation, application orchestration, database constraints, and deferred
constraint triggers when unavoidable MUST collectively prevent self-parenting,
cross-country parenting, invalid level transitions or root levels, cycles, duplicate
canonical codes within scope, duplicate external identifiers within scheme and scope,
multiple active preferred names for one entity and language, invalid temporal ranges, and
negative optimistic-lock versions. Database constraints MUST be the final integrity
boundary; application validation MUST NOT substitute for them.

Lifecycle semantics MUST explicitly define allowed and forbidden transitions, visibility,
update permission, historical queries, and referential consequences for `DRAFT`,
`ACTIVE`, `DEPRECATED`, and `RETIRED`. Temporal validity MUST use half-open intervals:
`valid_from` inclusive and `valid_until` exclusive, with null `valid_until` meaning no
known end. Periods MUST satisfy
`valid_until IS NULL OR valid_from IS NULL OR valid_until > valid_from`. Lifecycle and
temporal validity MUST NOT be interchangeable, and specifications MUST define legal
combinations. A record MAY be operationally deprecated while remaining historically
valid.

Activated records MUST NOT be physically deleted, and historical data MUST remain
traceable. Physical deletion MAY apply only to never-activated records when an approved
specification defines it.

Internal database identifiers MUST be UUIDs; UUID version 7 SHOULD be used when supported
by the approved PostgreSQL baseline. Internal UUIDs MUST NOT automatically become the
preferred public identifiers. Public references SHOULD use stable domain codes. Each
public code's semantics, normalization, character set, and uniqueness scope MUST be
specified. Retired codes MUST NOT be silently reused, and historical resolution MUST be
preserved under approved temporal rules.

### VI. Migration-Controlled PostgreSQL

PostgreSQL MUST be the persisted source of truth. Runtime access MUST use Hibernate
Reactive with Panache repository pattern or justified SQL through the Vert.x Reactive
PostgreSQL Client. Active Record MUST NOT be used. Application repository ports MUST
express domain intent and MUST NOT expose generic CRUD, `save(Object)`, unbounded
`findAll()`, HQL, SQL, Panache queries, or persistence entities.

State-changing commands MUST run in explicit reactive transactions. Queries SHOULD use an
explicitly managed reactive session when required. Transactions MUST NOT remain open
across calls to other services or brokers, user interaction, unbounded streams, or
long-running file or network work. Distributed transactions and XA MUST NOT be used.
Atomic database change and event publication MUST use a transactional outbox or another
approved local-transaction pattern; an outbox for this context MUST belong to this
service.

Flyway MUST be the only schema-evolution mechanism. Every schema change MUST be an
immutable versioned migration. Applied migrations MUST NOT be edited; correction MUST use
a new migration. Production MUST validate the schema and MUST NOT use `drop-and-create`,
`update`, or uncontrolled automatic DDL.

Migrations MUST be deterministic, reproducible on clean environments, data-preserving,
explicitly name indexes and constraints, include required indexes and constraints, avoid
destructive changes without an approved strategy, and document rollback or recovery when
rollback is unsafe. Every migration MUST be tested on the approved PostgreSQL version
from both an empty database and the previous supported schema with representative data,
including failure and recovery behavior. Flyway JDBC configuration MUST remain isolated
from reactive runtime persistence.

### VII. Contract-First Integration

Every externally accessible HTTP capability MUST be contract-first. The canonical OpenAPI
contract MUST be committed and updated before implementation; generated runtime
documentation MUST NOT be the sole contract. It MUST define paths, methods, authentication,
authorization scopes, request and response schemas, required fields, validation, status
codes, bounded pagination, filtering, sorting, errors, examples, idempotency, concurrency,
and versioning.

REST APIs MUST use resource nouns and explicit lifecycle commands when CRUD semantics are
insufficient. Generic CRUD MUST NOT replace domain behavior. Breaking changes MUST use an
approved versioning strategy and MUST NOT silently break consumers.

HTTP behavior MUST follow protocol semantics. Errors MUST use
`application/problem+json` conforming to RFC 9457 and include a stable problem type,
title, status, safe detail, machine-readable application error code, request or instance
reference when appropriate, and trace or correlation identifier. Responses MUST NOT
expose stack traces, SQL, credentials, or confidential configuration.

Mutable resources MUST use optimistic concurrency. Persistence versions MUST be exposed
as ETags or an approved equivalent, `If-Match` MUST be required where lost updates are
possible, and stale versions MUST produce `412 Precondition Failed` or another explicitly
documented protocol-compliant response. Retriable commands MUST specify idempotency.
Idempotency keys MUST define scope, retention, payload consistency validation,
deterministic replay, and reuse protection.

Messaging MUST NOT be introduced without a confirmed consumer or reliability
requirement. Row-by-row integration events MUST NOT be emitted without a concrete use
case; catalog-release publication SHOULD be the bulk-update event. Event contracts MUST
be machine-readable, using AsyncAPI or an approved equivalent, before implementation and
MUST include identifier, type, version, occurrence time, producer, catalog revision,
affected scope, and applicable correlation metadata. Consumers MUST assume at-least-once
delivery unless stated otherwise, and handlers MUST be idempotent.

### VIII. Security, Auditability, and Provenance

Security MUST apply least privilege. Administrative endpoints MUST authenticate and
authorize principals through explicit scopes or permissions for geographic reads,
catalog management, import execution, and release publication as applicable. Every
specification MUST explicitly decide public or internal read access. Tokens MUST be
validated under the approved identity architecture.

Audit identities MUST come from the validated principal. `created_by`, `updated_by`, and
equivalent fields MUST NOT be accepted from request bodies, untrusted headers, query
parameters, or client metadata. Audit records MUST preserve opaque immutable subject
identifiers; display names MAY be auxiliary.

Secrets MUST NOT be committed. They MUST be supplied through Podman secrets, environment
references, or an approved secret manager. Logs MUST NOT contain passwords, access or
refresh tokens, credential-bearing connection strings, secret keys, or full confidential
payloads.

Every externally sourced geographic record MUST persist source authority, reference,
revision, and applicable import or publication context; provenance MUST NOT exist only in
logs. Controlled imports MUST separate validation, comparison or diff, application, and
publication. A partially failed import MUST NOT activate a catalog release. Publication
MUST identify one coherent revision, and validation results and reports MUST be
deterministic and traceable. Source files SHOULD use checksums, and applicable licensing
or usage restrictions SHOULD be recorded.

### IX. Test-First Verification

Business-critical behavior MUST be implemented test-first. Required tests MUST exist,
MUST initially demonstrate the missing behavior when practical, and MUST pass before a
feature is complete.

Domain unit tests MUST cover value objects, aggregate invariants, lifecycle, temporal
validity, hierarchy policy, identifier normalization, and domain errors without Quarkus
or PostgreSQL. Application tests MUST cover orchestration, ports, authorization, error
mapping, reactive failure propagation, idempotency, and transaction boundaries.

Persistence integration tests MUST use the approved real PostgreSQL engine or an
ephemeral compatible PostgreSQL instance, never an in-memory substitute. They MUST cover
Flyway, constraints, partial indexes, deferred triggers, recursive queries, optimistic
concurrency, rollback, uniqueness, temporal constraints, and reactive session behavior.
API contract tests MUST prove runtime conformance to committed OpenAPI for success,
validation, authentication, authorization, not-found, conflict, concurrency, idempotent
retry, and Problem Details behavior.

Architecture tests MUST enforce dependency rules. Migration tests MUST cover empty and
upgrade paths, representative data, repeatability, failure, and recovery. Reactive tests
MUST detect event-loop blocking, synchronous database access, manual subscriptions,
unhandled failures, session misuse, and invalid concurrent transaction use. Native tests
MUST NOT be required unless an approved ADR changes the runtime to native.

### X. Observable, Measured Operations

The service MUST provide liveness, readiness, startup checks when needed, structured JSON
logs, request correlation, distributed tracing, metrics, graceful shutdown, actionable
diagnostics, and version and build metadata. Health endpoints MUST NOT expose secrets.
Readiness MUST fail when requests cannot be processed safely. Liveness MUST NOT fail only
because a temporary downstream dependency is unavailable.

Every request SHOULD carry a correlation or trace identifier. Metrics SHOULD cover
request duration, volume and errors; database pool use and acquisition time; and, when
applicable, import duration, validation failures, and publication results.

Performance requirements MUST derive from measured or approved workloads and MUST NOT be
invented. Performance-sensitive specifications MUST identify expected catalog size,
request volume, page limit, hierarchy depth, access patterns, import size, concurrency,
response objectives, and resource limits. Pagination and result sets MUST be bounded;
unbounded `findAll` and JSON responses MUST NOT be used. Indexes MUST follow query
patterns. Caches MUST require measured evidence. Recursive queries SHOULD precede
denormalized hierarchy structures. Closure tables, materialized paths, or `ltree` MUST
NOT be introduced without evidence and an ADR.

### XI. Reproducible JVM Delivery

Production MUST run the Java 25 JVM artifact as a long-running service. Native compilation
MUST NOT be adopted by preference. A native-runtime amendment requires a documented
problem, an ADR, JVM/native benchmarks for startup, steady-state throughput, memory, and
build time, compatibility checks, native integration tests, and operational acceptance.

The Gradle Wrapper MUST be committed. Dependencies SHOULD use the Quarkus platform BOM
where supported. Versions MUST NOT be duplicated arbitrarily or use `+`,
`latest.release`, or unbounded ranges. Upgrades MUST include compatibility, test,
migration when applicable, and security review. The build MUST be reproducible, MUST
retain or generate an approved software bill of materials, and MUST remove unused
dependencies.

The service MUST produce an OCI-compatible, non-root JVM container. It MUST use a
read-only filesystem where practical, persist no business data locally, receive secrets
through approved mechanisms, expose only required ports, terminate gracefully, expose
compatible health behavior, use deterministic production tags, and include build
revision metadata.

Deployment MUST support version-controlled rootless Podman Quadlet. Ports MUST NOT be
published to untrusted networks without approval. External access MUST pass through the
approved reverse proxy, load balancer, or F5 architecture. Database ports MUST NOT be
public. Deployment configuration and source MUST remain separate from secrets.

### XII. Simplicity and Explicit Decisions

The simplest architecture satisfying approved requirements MUST be preferred. The
project MUST NOT introduce speculative interfaces, generic abstractions without multiple
concrete uses, generic CRUD layers, premature microservices, messaging, caching,
denormalization, native compilation, multi-module decomposition, or framework wrappers
without domain value. Abstractions MUST be justified by current requirements,
testability, or a real boundary; possible future needs are insufficient.

The repository MUST maintain an accurate README, architecture overview, C4 context and
container diagrams, API contract, database model, migration strategy, security model,
deployment instructions, operational runbook, ADR directory, local-development guide,
and testing guide. Current behavior and proposed intent MUST be distinguished. A change
that invalidates documentation MUST update it in the same change.

An ADR MUST document any constitutional-default change, major infrastructure, new
datastore, messaging, caching, native runtime, hierarchy persistence change, API
versioning change, eventual consistency, new security trust boundary, or accepted
architectural risk. It MUST include context, decision, alternatives, consequences, risks,
validation criteria, and a reversal strategy when applicable. An ADR MUST NOT override a
constitutional MUST or MUST NOT without a constitutional amendment.

## Engineering Standards

### Specification and Design

Specifications MUST be written in English and MUST describe business capability and
observable behavior before implementation detail, except where a constitutional
constraint or correctness requires the detail. They MUST declare scope, non-goals,
actors, scenarios, testable requirements, measurable success criteria, errors, security,
transaction boundaries, and applicable temporal, contract, event, migration, performance,
and deployment impact. They MUST NOT invent arbitrary targets or functionality.

Plans MUST translate approved requirements without changing scope. Every `plan.md` MUST
contain the mechanical Constitution Check below before research and again after design.
Tasks MUST be dependency-ordered and traceable to requirements and user scenarios.
Implementation MUST NOT add capability absent from the approved specification.

### Data and Transactions

Hierarchy integrity MUST use the minimum sufficient combination of domain rules,
application orchestration, constraints, and deferred triggers. External calls, broker
publication, and long work MUST occur outside database transactions. When required, an
outbox record and its domain database change MUST commit in the same local PostgreSQL
transaction.

### Contracts and Releases

OpenAPI or AsyncAPI contracts MUST precede their implementation. Release and import
features MUST prevent partial publication and MUST preserve a coherent, identifiable
catalog revision. Existing consumers MUST receive an explicit compatibility or versioning
strategy before a breaking change.

### Language and Documentation

English MUST be the canonical language for source code, database objects, API and event
contracts, configuration, logs, and technical documentation. Documentation MUST describe
the current implementation; future designs MUST be labeled proposed.

## Quality Gates and Workflow

### Specification Readiness

Implementation planning MUST NOT proceed while material ambiguity remains. Before task
generation, a feature MUST have clear scenarios, explicit scope and non-goals, testable
functional requirements, measurable success criteria based on approved evidence, defined
error and security behavior, transaction boundaries, applicable temporal behavior,
contracts, migration impact, and no unresolved high-severity contradiction.
`tasks.md` MUST NOT be generated while blocking requirements or unjustified exceptions
remain.

### Required Verification

Before merge, the project MUST pass compilation; domain and application unit tests;
PostgreSQL persistence integration tests; API contract tests; architecture tests;
migration tests; reactive tests; static analysis; dependency vulnerability scanning;
secret scanning; formatting checks; container build; and deployment-manifest validation.
A green build is necessary but MUST NOT substitute for compliance with the specification
and this constitution.

### Constitution Check

Every plan MUST copy this checklist, record `PASS`, `FAIL`, or `N/A` with evidence for each
item, and repeat it after Phase 1 design. Any `FAIL` MUST block progress unless a compliant
exception is documented and approved.

- [ ] **Scope and ownership**: The capability is inside the bounded context; excluded
      domains remain excluded; no `tenant_id`, shared schema, or cross-service database
      foreign key is introduced.
- [ ] **Technology baseline**: Java 25, Gradle Wrapper/Kotlin DSL, approved Quarkus LTS,
      reactive persistence, PostgreSQL/Flyway, OpenAPI, JVM OCI, and Quadlet defaults are
      preserved; every addition or deviation is justified.
- [ ] **Architecture**: Domain, Application, Adapters, and Infrastructure dependencies
      point inward; DTOs and persistence entities remain outside the domain; architecture
      tests cover the boundaries.
- [ ] **Reactive model**: Runtime I/O is non-blocking, uses returned Mutiny pipelines,
      avoids prohibited blocking or manual subscription, and sequences session work.
- [ ] **Persistence and transactions**: Repository ports are domain-oriented; reactive
      transaction boundaries are explicit; no transaction spans external or long-running
      work; any outbox is locally atomic.
- [ ] **Domain integrity**: Aggregate boundaries and database constraints cover hierarchy,
      uniqueness, lifecycle, temporal, and concurrency invariants without loading the
      complete hierarchy.
- [ ] **Identifiers and provenance**: UUID internals, stable public-code semantics,
      non-reuse, historical resolution, and source provenance are defined where relevant.
- [ ] **Migrations**: Every schema change has a new immutable Flyway migration, named
      constraints and indexes, data-preserving/recovery strategy, and empty plus upgrade
      PostgreSQL tests.
- [ ] **HTTP contract**: Repository OpenAPI is updated first and covers security, schemas,
      validation, bounded pagination, errors, examples, concurrency, idempotency, and
      versioning.
- [ ] **Errors and concurrency**: RFC 9457 Problem Details, stable error codes, safe
      diagnostics, ETag/`If-Match`, and stale-update behavior are specified where relevant.
- [ ] **Security and audit**: Least privilege, explicit access policy, validated-principal
      audit identity, secret handling, and confidential-log exclusions are addressed.
- [ ] **Imports, releases, and events**: Provenance, coherent publication, failure
      isolation, machine-readable event contracts, idempotency, and local outbox atomicity
      are addressed where relevant.
- [ ] **Test-first coverage**: Domain, application, PostgreSQL persistence, contract,
      architecture, migration, and reactive tests are planned before implementation.
- [ ] **Performance**: Workload and limits are evidenced, pagination is bounded, indexes
      follow access patterns, and caching or denormalization is supported by measurements
      and an ADR where required.
- [ ] **Operability**: Health semantics, structured logs, correlation, tracing, metrics,
      graceful shutdown, and build metadata are planned.
- [ ] **Build and delivery**: Wrapper/BOM/version rules, SBOM, JVM container hardening,
      deterministic tags, rootless Quadlet, network boundaries, and manifest validation
      are covered.
- [ ] **Documentation and decisions**: Required documentation changes and mandatory ADRs
      are included in scope.
- [ ] **Simplicity and exceptions**: No speculative abstraction or infrastructure is
      introduced; each SHOULD deviation and constitutional exception records principle,
      reason, alternatives, risk, controls, approval, and review or removal date.

## Governance

This constitution MUST govern every specification, plan, task, implementation, review,
database migration, API or event contract, test, deployment artifact, and architectural
decision in this repository. It supersedes conflicting local practice. Reviews MUST verify
constitutional compliance, and non-compliance with a MUST or MUST NOT MUST block approval.
A SHOULD or SHOULD NOT MAY be bypassed only with documented justification.

An exception MUST identify the violated principle, reason, alternatives, risk,
compensating controls, required approval, and a removal or review date when temporary.
Unjustified exceptions MUST block task generation. An ADR alone MUST NOT override a MUST
or MUST NOT.

Amendments MUST be explicit and reviewable and MUST include the version change,
ratification and amendment dates, change summary, affected templates or documentation,
and compliance or migration actions for existing code. A specification, implementation,
or ADR MUST NOT silently amend this constitution.

Constitution versions MUST use semantic versioning:

- **MAJOR** MUST be used for incompatible governance or architectural-principle changes.
- **MINOR** MUST be used for a new mandatory principle or materially expanded guidance.
- **PATCH** MUST be used for clarification that does not change intent.

Each amendment MUST update the Sync Impact Report and all affected templates and guidance
in the same change. Compliance MUST be checked during specification readiness, planning,
task generation, implementation, review, migration review, and release approval.

**Project**: Geographic Reference Service | **Status**: active

**Version**: 1.0.0 | **Ratified**: 2026-07-23 | **Last Amended**: 2026-07-23
