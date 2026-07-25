# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 25

**Primary Dependencies**: Approved Quarkus LTS/latest approved patch, Quarkus REST,
Jackson, Mutiny, Hibernate Reactive with Panache repositories, Vert.x Reactive PostgreSQL
Client

**Storage**: PostgreSQL 18 or explicitly approved baseline; Flyway migrations

**Testing**: Domain and application unit, PostgreSQL persistence integration, OpenAPI
contract, architecture, migration, and reactive tests

**Target Platform**: Java 25 JVM in a non-root OCI container; rootless Podman Quadlet

**Project Type**: Independently deployable reactive web service

**Performance Goals**: [Measured or approved workload objectives from spec; MUST NOT be
invented, or NOT PERFORMANCE-SENSITIVE with evidence]

**Constraints**: Non-blocking runtime I/O; bounded pagination; strict Clean Architecture;
contract-first OpenAPI; JVM runtime; no speculative infrastructure

**Scale/Scope**: [Expected catalog size, request volume, page size, hierarchy depth,
access patterns, import size, and concurrency when applicable]

## Constitution Check

*GATE: Every item MUST be marked PASS, FAIL, or N/A with evidence before Phase 0 and
re-evaluated after Phase 1. Any FAIL blocks progress unless an approved constitutional
exception is recorded in Complexity Tracking.*

| Gate | Pre-Research | Post-Design | Evidence |
|------|--------------|-------------|----------|
| Scope is inside the geographic bounded context; excluded domains, `tenant_id`, shared schema, and cross-service database FKs are absent | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [spec/design reference] |
| Java 25, Gradle Wrapper/Kotlin DSL, approved Quarkus LTS, reactive PostgreSQL/Flyway, OpenAPI, JVM OCI, and Quadlet baseline is preserved | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [reference] |
| Clean Architecture dependencies point inward and architecture tests cover all boundaries | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [packages/tests] |
| Runtime I/O is non-blocking Mutiny; prohibited blocking, manual subscription, and unsafe session concurrency are absent | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [flow design] |
| Domain-oriented repositories and explicit reactive transaction boundaries avoid external or long-running work inside transactions | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [ports/transaction map] |
| Aggregates, constraints, lifecycle, temporal, hierarchy, identifier, concurrency, and provenance rules are defined where applicable | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [data model/invariants] |
| Every schema change uses a new immutable Flyway migration with named integrity objects, recovery strategy, and empty/upgrade PostgreSQL tests | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [migration plan] |
| Repository OpenAPI/AsyncAPI is updated before implementation and defines security, errors, pagination, concurrency, idempotency, and versioning | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [contract path] |
| RFC 9457 errors, stable codes, safe diagnostics, ETag/`If-Match`, and retry behavior are defined where applicable | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [contract/error model] |
| Least privilege, explicit access, principal-derived audit identity, secrets, and confidential logging are addressed | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [security model] |
| Import/release/event coherence, provenance, idempotency, and local outbox atomicity are defined where applicable | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [workflow/event design] |
| Test-first tasks cover domain, application, PostgreSQL persistence, contracts, architecture, migrations, and reactive behavior | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [test strategy] |
| Performance targets are evidence-based; results are bounded; indexes follow access patterns; cache/denormalization decisions have evidence and ADRs | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [workload/query model] |
| Health, JSON logs, correlation, tracing, metrics, graceful shutdown, and build metadata are addressed | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [operability plan] |
| Wrapper/BOM/SBOM, non-root JVM container, deterministic tags, Quadlet, network boundaries, and manifest validation are addressed | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [delivery plan] |
| Required documentation and ADRs are included; no speculative abstraction or infrastructure is introduced | [PASS/FAIL/N/A] | [PASS/FAIL/N/A] | [docs/decision list] |

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the package placeholders below with the concrete
  Clean Architecture packages and repository paths affected by this feature.
  Preserve the single deployable module unless independent compilation has a
  demonstrated benefit.
-->

```text
src/
├── main/
│   ├── java/             # domain, application, adapters, infrastructure packages
│   └── resources/
│       ├── db/migration/ # immutable Flyway migrations
│       └── application.properties
└── test/
    ├── java/             # unit, integration, contract, architecture, migration, reactive
    └── resources/

openapi/                  # canonical HTTP contracts
docs/                     # architecture, data, security, deployment, operations, ADRs
deploy/                   # version-controlled Quadlet and deployment configuration
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY for SHOULD deviations or proposed exceptions. A MUST/MUST NOT conflict
> requires constitutional compliance or an explicit constitutional amendment; an ADR
> alone cannot approve it.**

| Principle / Gate | Reason | Alternatives | Risk | Compensating Controls | Approval | Review / Removal Date |
|------------------|--------|--------------|------|-----------------------|----------|-----------------------|
| [reference] | [current requirement] | [options considered] | [risk] | [controls] | [required approver] | [YYYY-MM-DD or permanent] |
