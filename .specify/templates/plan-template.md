# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition
describes the execution workflow.

## Summary

[Extract the consuming-system query requirement and the reactive read-only technical
approach from the feature specification]

## Technical Context

<!--
  ACTION REQUIRED: Replace every placeholder with evidence from the approved
  specification or research. This service is a reactive, runtime read-only geographic
  reference-data service.
-->

**Language/Version**: Java 25

**Primary Dependencies**: Approved Quarkus LTS/latest approved patch, Quarkus REST,
Jackson, Mutiny, Hibernate Reactive with Panache repositories or justified Vert.x
Reactive PostgreSQL Client queries

**Storage**: PostgreSQL 18; immutable Flyway schema and catalog-data migrations

**Runtime Database Identity**: Read-only role with only required `CONNECT`, `USAGE`, and
`SELECT` privileges; no ownership or mutation privileges

**Migration Execution**: Separate migration identity outside application startup;
execution SHOULD use a CI/CD stage or controlled one-shot deployment unit

**Testing**: Domain query, application query, PostgreSQL 18 persistence, runtime-role
privilege, OpenAPI and HTTP-method exclusion, architecture, conditional migration, and
reactive tests

**Target Platform**: Java 25 JVM in a non-root OCI container; rootless Podman Quadlet

**Project Type**: Independently deployable reactive read-only reference-data service

**Performance Goals**: [Measured or approved read-workload objectives from the spec;
MUST NOT be invented, or NOT PERFORMANCE-SENSITIVE with evidence]

**Constraints**: Only `GET`, `HEAD`, and required `OPTIONS`; non-blocking runtime I/O;
SELECT-only runtime credentials; Flyway outside runtime identity; bounded pagination,
hierarchy depth, and results; strict Clean Architecture; contract-first OpenAPI; JVM
runtime; no speculative infrastructure

**Scale/Scope**: [Expected catalog size, request volume, maximum page size, maximum
hierarchy depth and result count, access patterns, localization/temporal dimensions, and
read concurrency when applicable]

**Catalog Revision/Provenance**: [Expected dataset revision, source authority/reference,
checksum strategy, readiness compatibility, or N/A with evidence]

**Query Consistency**: [Single-statement snapshot, justified multi-query read-only
transaction, or N/A; explain why one statement is insufficient when a transaction is
used]

## Constitution Check

*GATE: Every item MUST be marked PASS, FAIL, or N/A with concrete evidence before Phase 0
and re-evaluated after Phase 1. Any FAIL blocks task generation unless the constitution
itself is amended.*

| Gate | Pre-Research | Post-Design | Evidence |
|------|--------------|-------------|----------|
| The capability is a geographic query inside the bounded context | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [spec/design reference] |
| Only `GET`, `HEAD`, or required `OPTIONS` endpoints are introduced | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [contract paths and methods] |
| No `POST`, `PUT`, `PATCH`, `DELETE`, mutation job, or message consumer is introduced | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [negative contract/architecture evidence] |
| Runtime PostgreSQL access is reactive and non-blocking | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [query flow design] |
| Runtime PostgreSQL credentials have SELECT-only privileges | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [role/grant design and tests] |
| Flyway and catalog SQL execute outside the runtime application identity | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [migration unit/pipeline design] |
| Clean Architecture dependency direction is preserved | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [packages and architecture tests] |
| Application ports and repositories expose only query operations | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [port/repository design] |
| OpenAPI is updated before implementation | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [canonical contract path] |
| Pagination, depth, and result sizes are bounded | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [limits and validation] |
| Lifecycle and temporal visibility are defined | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [current/historical semantics] |
| Localization and fallback behavior are defined where applicable | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [language behavior or N/A reason] |
| RFC 9457 query errors are defined | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [problem types and error codes] |
| HTTP caching behavior is defined or explicitly not required | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [ETag/date validation or rationale] |
| PostgreSQL query and migration tests use PostgreSQL 18 | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [test environment] |
| SQL catalog changes are immutable, reviewed, traceable, and recoverable | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [migration/provenance/recovery plan] |
| Database constraints continue to enforce reference-data integrity | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [constraints and rejection tests] |
| Architecture tests prohibit write endpoints and mutation use cases | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [architecture rules] |
| Reactive tests prohibit blocking and manual subscriptions | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [reactive test strategy] |
| Deployment separates migration and runtime database identities | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [deployment ordering and secret boundaries] |
| Observability, security, documentation, and operational changes are covered | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [cross-cutting design] |
| No speculative messaging, cache, native build, or other infrastructure is added | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [dependency/ADR review] |

## Read-Only and Migration Design

**HTTP Surface**: [List every introduced or changed path and its `GET`, `HEAD`, or
required `OPTIONS` method; prove mutation methods remain absent]

**Query Ports and Repositories**: [List query input ports, query repository output ports,
bounded result models, and reactive return types]

**Runtime Role**: [Document exact grants, forbidden privileges, role-ownership checks,
and privilege-test approach]

**Migration Role and Ordering**: [Document external Flyway execution, credential
separation, schema/catalog verification, readiness dependency, and recovery point]

**SQL Catalog Impact**: [List immutable migration files, source provenance, atomicity,
recovery, deterministic count/checksum validation, or state that no schema/catalog
change occurs]

**Query Bounds and Indexes**: [Maximum page size, hierarchy depth/result bounds, sorting,
filtering, query-plan/index validation, and approved workload evidence]

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

<!--
  ACTION REQUIRED: Replace package placeholders with concrete Clean Architecture
  packages and repository paths. Preserve one deployable module unless independent
  compilation has a demonstrated benefit.
-->

```text
src/
├── main/
│   ├── java/             # read domain, query application, adapters, infrastructure
│   └── resources/
│       ├── db/migration/ # immutable schema and catalog SQL migrations, when required
│       └── application.properties
└── test/
    ├── java/             # query, PostgreSQL, privilege, contract, architecture, reactive
    └── resources/

openapi/                  # canonical read-only HTTP contract
docs/                     # architecture, data, security, deployment, operations, ADRs
deploy/                   # separate migration and runtime identities; Quadlet artifacts
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY for justified SHOULD/SHOULD NOT deviations. A MUST/MUST NOT conflict cannot
> be approved here or by an ADR; it requires constitutional compliance or an explicit
> constitutional amendment.**

| Principle / Gate | Reason | Alternatives | Risk | Compensating Controls | Approval | Review / Removal Date |
|------------------|--------|--------------|------|-----------------------|----------|-----------------------|
| [reference] | [current requirement] | [options considered] | [risk] | [controls] | [required approver] | [YYYY-MM-DD or permanent] |
