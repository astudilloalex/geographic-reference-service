---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are MANDATORY. Generate test-first tasks for every applicable domain,
application, PostgreSQL persistence, OpenAPI contract, architecture, migration, and
reactive behavior before the corresponding implementation tasks.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Application**: `src/main/java/`
- **Resources and migrations**: `src/main/resources/`
- **Tests**: `src/test/java/` and `src/test/resources/`
- **Canonical contracts**: `openapi/` and, when approved, `asyncapi/`
- **Documentation and deployment**: `docs/` and `deploy/`

<!--
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.

  The /speckit-tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/

  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment

  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Verify Java 25 Gradle Wrapper/Kotlin DSL and approved Quarkus BOM in build.gradle.kts
- [ ] T003 [P] Configure formatting, static analysis, vulnerability, secret, and SBOM tooling in build.gradle.kts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T004 Define package-level Clean Architecture boundaries in src/main/java/
- [ ] T005 Add architecture rules in src/test/java/
- [ ] T006 Configure isolated Flyway migration and reactive PostgreSQL runtime access in src/main/resources/application.properties
- [ ] T007 [P] Configure authentication and authorization adapters in src/main/java/
- [ ] T008 [P] Establish canonical OpenAPI contract location in openapi/
- [ ] T009 Configure RFC 9457 error mapping, structured logging, tracing, metrics, and health in src/main/java/

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (MANDATORY; write before implementation) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Add domain/application tests for [behavior] in src/test/java/[package]/[Name]Test.java
- [ ] T011 [P] [US1] Add OpenAPI contract test for [endpoint] in src/test/java/[package]/[Name]ContractTest.java
- [ ] T012 [P] [US1] Add PostgreSQL/Flyway/reactive integration tests in src/test/java/[package]/[Name]PersistenceTest.java

### Implementation for User Story 1

- [ ] T013 [P] [US1] Implement [value object/aggregate] in src/main/java/[domain-package]/[Name].java
- [ ] T014 [P] [US1] Define [use case/port] in src/main/java/[application-package]/[Name].java
- [ ] T015 [US1] Add immutable Flyway migration in src/main/resources/db/migration/[version]__[name].sql
- [ ] T016 [US1] Implement reactive persistence adapter in src/main/java/[outbound-package]/[Name].java
- [ ] T017 [US1] Update canonical contract in openapi/[contract].yaml before implementing the REST adapter
- [ ] T018 [US1] Implement REST adapter and RFC 9457 mapping in src/main/java/[inbound-package]/[Name]Resource.java
- [ ] T019 [US1] Add security, audit, observability, and documentation required by the story

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (MANDATORY; write before implementation) ⚠️

- [ ] T020 [P] [US2] Add domain/application tests in src/test/java/[package]/[Name]Test.java
- [ ] T021 [P] [US2] Add applicable contract, PostgreSQL, migration, and reactive tests in src/test/java/[package]/

### Implementation for User Story 2

- [ ] T022 [P] [US2] Implement domain behavior in src/main/java/[domain-package]/[Name].java
- [ ] T023 [US2] Implement use case and ports in src/main/java/[application-package]/
- [ ] T024 [US2] Implement required migration, contract, and adapters in their planned paths
- [ ] T025 [US2] Integrate with User Story 1 components without violating story independence

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (MANDATORY; write before implementation) ⚠️

- [ ] T026 [P] [US3] Add domain/application tests in src/test/java/[package]/[Name]Test.java
- [ ] T027 [P] [US3] Add applicable contract, PostgreSQL, migration, and reactive tests in src/test/java/[package]/

### Implementation for User Story 3

- [ ] T028 [P] [US3] Implement domain behavior in src/main/java/[domain-package]/[Name].java
- [ ] T029 [US3] Implement use case, contract, migration, and adapters in their planned paths

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in docs/
- [ ] TXXX Code cleanup and refactoring
- [ ] TXXX Evidence-based performance validation for approved workloads
- [ ] TXXX [P] Complete required unit, integration, contract, architecture, migration, and reactive test coverage in src/test/
- [ ] TXXX Security and secret-handling verification
- [ ] TXXX Build non-root JVM OCI image and validate deploy/ Quadlet manifests
- [ ] TXXX Run compilation, static analysis, dependency and secret scans, formatting, tests, container build, and manifest quality gates
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all independently parallelizable tests for User Story 1:
Task: "Contract test for [endpoint] in src/test/java/[package]/[Name]ContractTest.java"
Task: "PostgreSQL integration test in src/test/java/[package]/[Name]PersistenceTest.java"

# Launch all models for User Story 1 together:
Task: "Create [ValueObject] in src/main/java/[domain-package]/[ValueObject].java"
Task: "Create [Aggregate] in src/main/java/[domain-package]/[Aggregate].java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Required tests MUST be written before their corresponding implementation and MUST
  demonstrate the missing behavior when practical
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
