---
description: "Task list template for read-only geographic query feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for query user stories),
research.md, data-model.md, contracts/

**Tests**: Tests are MANDATORY and MUST precede corresponding implementation. Generate
test-first tasks for every applicable domain query, application query, PostgreSQL 18
reactive persistence, runtime-role privilege, OpenAPI/HTTP-method exclusion,
architecture, conditional migration, bounded pagination/hierarchy, and reactive behavior.

**Organization**: Tasks are grouped by consuming-system query story so each story can be
implemented and verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it affects different files and has no dependency
  on incomplete work.
- **[Story]**: Query user story label, such as US1, US2, or US3.
- Every task MUST include an exact file path.

## Read-Only Task Rules

- Tasks MUST implement only query use cases and `GET`, `HEAD`, or required `OPTIONS`
  adapters.
- Tasks MUST NOT create `POST`, `PUT`, `PATCH`, `DELETE`, mutation use cases, mutation
  repository methods, scheduled mutation jobs, message consumers, generic CRUD,
  application imports, publication flows, outbox code, or command idempotency.
- Flyway schema or catalog-data tasks MUST be generated only when the feature changes
  schema or reference data.
- Every migration test task MUST precede its migration implementation task.
- SQL catalog changes MUST include provenance, PostgreSQL 18 validation, atomicity,
  recovery, deterministic count/checksum validation, and protection from partial
  activation.
- Runtime database-role tests MUST prove required reads succeed and `INSERT`, `UPDATE`,
  and `DELETE` fail.
- OpenAPI tests MUST prove mutation methods are absent and unavailable.
- Deployment tasks MUST keep migration and runtime identities separate and MUST execute
  Flyway outside application startup.

## Path Conventions

- **Application**: `src/main/java/`
- **Resources and conditional migrations**: `src/main/resources/`
- **Tests**: `src/test/java/` and `src/test/resources/`
- **Canonical read-only contract**: `openapi/`
- **Documentation and deployment**: `docs/` and `deploy/`

<!--
  ============================================================================
  IMPORTANT: The sample tasks below MUST be replaced by /speckit-tasks using:
  - Consuming-system query stories and priorities from spec.md
  - Read-only design and Constitution Check evidence from plan.md
  - Read-domain concepts from data-model.md
  - GET/HEAD/required OPTIONS operations from contracts/

  Do not preserve sample tasks in generated tasks.md.
  ============================================================================
-->

## Phase 1: Setup (Shared Query Infrastructure)

**Purpose**: Establish the approved build and query-only project structure.

- [ ] T001 Verify Java 25 Gradle Wrapper/Kotlin DSL and approved Quarkus BOM in build.gradle.kts
- [ ] T002 Define query-focused Clean Architecture package boundaries in src/main/java/[base-package]/package-info.java
- [ ] T003 [P] Configure formatting, static analysis, vulnerability, secret, and SBOM tooling in build.gradle.kts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Enforce read-only behavior before any query story is implemented.

**⚠️ CRITICAL**: No query story work can begin until this phase is complete.

- [ ] T004 Add Clean Architecture and no-mutation rules in src/test/java/[base-package]/architecture/ArchitectureTest.java
- [ ] T005 Add OpenAPI method-exclusion tests for POST, PUT, PATCH, and DELETE in src/test/java/[base-package]/contract/ReadOnlyOpenApiTest.java
- [ ] T006 Add PostgreSQL 18 privilege tests proving runtime SELECT succeeds and INSERT, UPDATE, and DELETE fail in src/test/java/[base-package]/persistence/RuntimeRolePrivilegeTest.java
- [ ] T007 Configure only reactive PostgreSQL runtime access with the read-only identity in src/main/resources/application.properties
- [ ] T008 Configure separate external Flyway migration and runtime identities in deploy/geographic-migration.container and deploy/geographic-reference-service.container
- [ ] T009 [P] Establish the canonical read-only OpenAPI contract in openapi/[contract].yaml
- [ ] T010 [P] Configure RFC 9457 query error mapping in src/main/java/[base-package]/adapter/inbound/rest/ProblemMapper.java
- [ ] T011 [P] Configure structured JSON logs, tracing, metrics, health, build revision, and catalog revision reporting in src/main/java/[base-package]/infrastructure/ObservabilityConfiguration.java

**Checkpoint**: Read-only architecture, contract, credentials, and operations are
enforceable.

---

## Phase 3: User Story 1 - [Resolve Reference Data] (Priority: P1) 🎯 MVP

**Goal**: [Describe the independent query value delivered to a consuming system.]

**Independent Test**: [Describe input, visible catalog fixture, and expected query result
or RFC 9457 error.]

### Tests for User Story 1 (MANDATORY; write before implementation) ⚠️

> Write these tests first and demonstrate the missing query behavior when practical.

- [ ] T012 [P] [US1] Add domain normalization and visibility tests in src/test/java/[base-package]/domain/[Concept]Test.java
- [ ] T013 [P] [US1] Add application query orchestration and failure tests in src/test/java/[base-package]/application/[Query]Test.java
- [ ] T014 [P] [US1] Add PostgreSQL 18 reactive query integration tests in src/test/java/[base-package]/persistence/[Query]PersistenceTest.java
- [ ] T015 [P] [US1] Add GET/HEAD contract and RFC 9457 tests in src/test/java/[base-package]/contract/[Query]ContractTest.java

### Conditional Migration Tests and Implementation for User Story 1

<!-- Include both tasks only when US1 changes schema or reference data. -->

- [ ] T016 [US1] Add clean and previous-revision migration, constraint, atomic-failure, recovery, provenance, and checksum tests in src/test/java/[base-package]/migration/[Catalog]MigrationTest.java
- [ ] T017 [US1] Add immutable Flyway schema/catalog migration in src/main/resources/db/migration/[version]__[name].sql

### Implementation for User Story 1

- [ ] T018 [P] [US1] Implement immutable read-domain value objects in src/main/java/[base-package]/domain/[Concept].java
- [ ] T019 [P] [US1] Define the query input port, query repository output port, and bounded result model in src/main/java/[base-package]/application/[Query].java
- [ ] T020 [US1] Implement the reactive query persistence adapter in src/main/java/[base-package]/adapter/outbound/persistence/[Query]Adapter.java
- [ ] T021 [US1] Update GET/HEAD paths in openapi/[contract].yaml before the REST adapter
- [ ] T022 [US1] Implement the query-only REST adapter in src/main/java/[base-package]/adapter/inbound/rest/[Query]Resource.java
- [ ] T023 [US1] Add read access, query metrics, catalog provenance/revision, and documentation required by the story in docs/[feature].md

**Checkpoint**: User Story 1 is independently queryable and exposes no mutation path.

---

## Phase 4: User Story 2 - [Browse Bounded Hierarchy] (Priority: P2)

**Goal**: [Describe the bounded hierarchy query.]

**Independent Test**: [Describe pagination, depth, result-limit, lifecycle, and temporal
verification.]

### Tests for User Story 2 (MANDATORY; write before implementation) ⚠️

- [ ] T024 [P] [US2] Add hierarchy-navigation application tests in src/test/java/[base-package]/application/[HierarchyQuery]Test.java
- [ ] T025 [P] [US2] Add PostgreSQL 18 recursive query, pagination, and limit tests in src/test/java/[base-package]/persistence/[HierarchyQuery]PersistenceTest.java
- [ ] T026 [P] [US2] Add bounded hierarchy OpenAPI contract tests in src/test/java/[base-package]/contract/[HierarchyQuery]ContractTest.java
- [ ] T027 [US2] Add approved query-plan and index validation in src/test/java/[base-package]/persistence/[HierarchyQuery]PlanTest.java

### Implementation for User Story 2

- [ ] T028 [P] [US2] Implement hierarchy read-domain semantics in src/main/java/[base-package]/domain/[HierarchyConcept].java
- [ ] T029 [US2] Implement bounded hierarchy query ports and orchestration in src/main/java/[base-package]/application/[HierarchyQuery].java
- [ ] T030 [US2] Implement the reactive recursive query adapter in src/main/java/[base-package]/adapter/outbound/persistence/[HierarchyQuery]Adapter.java
- [ ] T031 [US2] Update the GET hierarchy contract in openapi/[contract].yaml
- [ ] T032 [US2] Implement the GET hierarchy REST adapter in src/main/java/[base-package]/adapter/inbound/rest/[HierarchyQuery]Resource.java

**Checkpoint**: User Stories 1 and 2 work independently with bounded results.

---

## Phase 5: User Story 3 - [Resolve Localized or Historical Data] (Priority: P3)

**Goal**: [Describe the localization fallback or explicit historical query.]

**Independent Test**: [Describe language fallback and/or half-open `asOf` boundaries.]

### Tests for User Story 3 (MANDATORY; write before implementation) ⚠️

- [ ] T033 [P] [US3] Add language fallback and temporal visibility tests in src/test/java/[base-package]/domain/[Visibility]Test.java
- [ ] T034 [P] [US3] Add application localization and historical query tests in src/test/java/[base-package]/application/[HistoricalQuery]Test.java
- [ ] T035 [P] [US3] Add PostgreSQL 18 name and `asOf` query tests in src/test/java/[base-package]/persistence/[HistoricalQuery]PersistenceTest.java
- [ ] T036 [P] [US3] Add cache-validation and historical contract tests in src/test/java/[base-package]/contract/[HistoricalQuery]ContractTest.java

### Implementation for User Story 3

- [ ] T037 [P] [US3] Implement lifecycle, temporal, and localization read semantics in src/main/java/[base-package]/domain/[Visibility].java
- [ ] T038 [US3] Implement localization or historical query ports and orchestration in src/main/java/[base-package]/application/[HistoricalQuery].java
- [ ] T039 [US3] Implement the reactive name or temporal query adapter in src/main/java/[base-package]/adapter/outbound/persistence/[HistoricalQuery]Adapter.java
- [ ] T040 [US3] Update the GET historical/localized contract in openapi/[contract].yaml
- [ ] T041 [US3] Implement the GET historical/localized REST adapter in src/main/java/[base-package]/adapter/inbound/rest/[HistoricalQuery]Resource.java

**Checkpoint**: All query stories are independently functional.

---

[Add more consuming-system query story phases as needed.]

---

## Phase N: Polish & Cross-Cutting Read-Only Verification

- [ ] TXXX [P] Update read-only API, database identity, migration, security, deployment, and operations documentation in docs/
- [ ] TXXX Re-run OpenAPI tests proving only GET, HEAD, and required OPTIONS are exposed in src/test/java/[base-package]/contract/
- [ ] TXXX Re-run architecture tests proving no mutation use case, repository method, runtime JDBC, startup Flyway, or resource-to-persistence dependency exists in src/test/java/[base-package]/architecture/
- [ ] TXXX Re-run PostgreSQL 18 runtime-role read and mutation-rejection tests in src/test/java/[base-package]/persistence/
- [ ] TXXX Validate bounded pagination, hierarchy depth/results, indexes, and approved query plans in src/test/java/[base-package]/persistence/
- [ ] TXXX Validate external one-shot migration ordering and read-only runtime startup in deploy/
- [ ] TXXX Validate RFC 9457 errors, cache behavior, access policy, logs, traces, metrics, health, and catalog revision metadata in src/test/
- [ ] TXXX Build the non-root JVM OCI image and validate rootless Quadlet manifests in deploy/
- [ ] TXXX Run compilation, formatting, static analysis, dependency/secret scans, all tests, container build, and manifest quality gates
- [ ] TXXX Run quickstart.md read-only validation scenarios in specs/[###-feature-name]/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup**: No dependencies.
- **Foundational**: Depends on Setup and blocks every query story.
- **Query Stories**: Depend on Foundational; MAY proceed independently when their files
  and data dependencies do not overlap.
- **Polish**: Depends on all selected query stories.

### Within Each Query Story

- Tests MUST be written before implementation.
- Migration tests MUST precede a conditional migration.
- Read-domain values precede application query ports.
- Application query ports precede persistence and REST adapters.
- Canonical OpenAPI changes MUST precede REST adapter implementation.
- Query limits and privilege enforcement MUST be verified before story completion.

### Parallel Opportunities

- Tasks marked `[P]` affect independent files and MAY run in parallel.
- Domain, application, persistence, and contract test tasks MAY run in parallel when
  their fixtures and files are independent.
- Different query stories MAY run in parallel only after Foundational completes.

---

## Implementation Strategy

### MVP First

1. Complete Setup.
2. Complete Foundational read-only enforcement.
3. Complete User Story 1 tests and implementation.
4. Validate User Story 1 independently, including method exclusion and runtime-role
   mutation rejection.

### Incremental Delivery

1. Deliver each query story as a bounded, independently tested increment.
2. Introduce SQL catalog migrations only for documented data or schema impact.
3. Keep the migration identity outside the long-running application for every increment.
4. Re-run cross-cutting read-only verification before promotion.

## Notes

- Every task MUST remain traceable to a query requirement or constitutional gate.
- Required tests MUST precede their corresponding implementation.
- No task MAY expand runtime scope into administration, imports, publication, lifecycle
  commands, messaging, or data mutation.
- Avoid vague tasks, file conflicts, unbounded queries, and cross-story dependencies that
  break independent verification.
