---
description: "Executable task list for the read-only geographic catalog"
---

# Tasks: Read Geographic Catalog

**Input**: Design documents from `/specs/001-read-geographic-catalog/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`catalog-source-manifest.md`, `contracts/openapi.yaml`, and accepted ADRs 0001 and 0002

**Tests**: Tests are mandatory and precede their corresponding implementation. Test tasks must
first fail for the expected missing behavior when practical, then pass after the implementation
task. PostgreSQL integration, migration, and privilege suites use the exact digest-pinned
PostgreSQL 18 image from `plan.md`; H2 and compatibility substitutions are prohibited.

**Organization**: Tasks are grouped by consuming-system story. Shared read-only enforcement is
completed first. Source-derived catalog assembly is isolated behind the manual legal/data-
governance gate and is never part of the pre-approval synthetic implementation path.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: May run in parallel because the task changes different files and has no dependency on
  incomplete work in the same phase.
- **[Story]**: `US1`, `US2`, `US3`, or `US4` identifies the consuming-system story.
- Every task below names the exact file or files it creates or changes.

## Legal And Data Gate

- Tasks T001-T087 are executable before INEC approval only with visibly synthetic fixtures under
  `src/test/resources/fixtures/`.
- T088 is an authorized manual governance decision. T089-T092 must not run until T088 is complete.
- Before T088, do not generate or commit bulk INEC rows, production V002/V003, source-derived
  responses, production smoke values, or a distributable migration image. Ephemeral synthetic
  V002/V003 files under `build/` remain permitted for T015 and are never release artifacts.
- Generated files under `build/catalog/` are release evidence, not repository source. Source
  archives and confidential approval records remain in approved external stores.

## Phase 1: Setup (Shared Query Infrastructure)

**Purpose**: Establish the reproducible Java 25, Quarkus, Gradle, and query-only project shape.

- [ ] T001 Remove `mavenLocal()`, retain only approved repositories, add exactly the Quarkus 3.33.2.1 `quarkus-arc`, `quarkus-rest-jackson`, `quarkus-reactive-pg-client`, `quarkus-oidc`, `quarkus-micrometer`, `micrometer-registry-prometheus`, `quarkus-logging-json`, BOM-supplied Mutiny, and test-scoped PostgreSQL/JDBC/Flyway/Testcontainers/ArchUnit/OpenAPI dependencies, explicitly excluding SmallRye Health, Quarkus Info, runtime OpenAPI, runtime JDBC, ORM, and runtime Flyway, and verify Java 25 in `build.gradle.kts` and `settings.gradle.kts`
- [ ] T002 Configure Spotless, SpotBugs, dependency locking/checksum verification, the non-runtime `catalogTool` Java source set, and named `architectureTest`, `reactiveTest`, `blockingCanary`, `oidcSecurityTest`, `openApiContractTest`, `routeInventoryTest`, `packagedOpenApiTest`, `documentationTest`, `postgresIntegrationTest`, `migrationTest`, `runtimePrivilegeTest`, `gracefulShutdownTest`, and `quadletTest` tasks in `build.gradle.kts` and `gradle/verification-metadata.xml`
- [ ] T003 [P] Declare inward-only package responsibilities in `src/main/java/com/alexastudillo/geographicreference/domain/package-info.java`, `src/main/java/com/alexastudillo/geographicreference/application/package-info.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/package-info.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/package-info.java`, and `src/main/java/com/alexastudillo/geographicreference/infrastructure/package-info.java`
- [ ] T004 [P] Add a visibly artificial, non-source-derived active catalog fixture with fabricated countries, divisions, names, identifiers, provenance, coverage, and revision values in `src/test/resources/fixtures/synthetic-catalog-v1.json`
- [ ] T005 [P] Copy the already approved canonical contract byte-for-byte from `specs/001-read-geographic-catalog/contracts/openapi.yaml` to `src/main/resources/META-INF/openapi.yaml` before implementing any REST resource
- [ ] T006 Configure commit-pinned CI actions and the synthetic pre-approval quality suites with Java 25, dependency verification, digest-pinned PostgreSQL 18, `docker.io/aquasec/trivy:0.72.0@sha256:cffe3f5161a47a6823fbd23d985795b3ed72a4c806da4c4df16266c02accdd6f`, and `docker.io/zricethezav/gitleaks:v8.30.1@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f`, while gating source-derived image stages and retaining test/SBOM evidence in `.github/workflows/ci.yml`

**Checkpoint**: The build has reproducible dependencies, isolated catalog tooling, named quality
gates, the canonical contract resource, and synthetic-only test input.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Prove and implement architecture, security, HTTP, migration, role, and reactive
constraints shared by every story.

**Critical**: Complete this phase before implementing any query story.

**Traceability**: CR-002 through CR-006, DR-001, DR-003 through DR-013, ER-001 through ER-006,
SR-001 through SR-004, QC-001 through QC-002, and Constitution Principles II, III, IV, VI, IX,
and XI.

### Foundational Tests (write before foundational implementation)

- [ ] T007 [P] Add failing ArchUnit and source-rule coverage for inward dependencies, query-only vocabulary, no generic CRUD, no write annotations, no REST-to-persistence access, no runtime JDBC/Flyway, no blocking calls, and no manual Mutiny subscription in `src/test/java/com/alexastudillo/geographicreference/architecture/ReadOnlyArchitectureTest.java`
- [ ] T008 [P] Add failing static/runtime contract tests for exactly 16 paths and 32 GET/HEAD operations, no request bodies/webhooks/writes/OPTIONS/TRACE, GET/HEAD parity, packaged-contract byte identity, exact production route inventory, and `405 Allow: GET, HEAD` in `src/test/java/com/alexastudillo/geographicreference/contract/ReadOnlyOpenApiTest.java`, `src/test/java/com/alexastudillo/geographicreference/contract/RouteInventoryTest.java`, and `src/test/java/com/alexastudillo/geographicreference/contract/PackagedOpenApiTest.java`
- [ ] T009 [P] Add failing shared-domain tests for BCP 47 canonicalization, name-type ordering, one-based bounded pagination, UTC query dates, half-open validity, lifecycle visibility, normalized catalog revisions, and deterministic name fallback in `src/test/java/com/alexastudillo/geographicreference/domain/shared/SharedQuerySemanticsTest.java`
- [ ] T010 [P] Add failing tests for unsupported/repeated parameters, validation bounds, the ER-006 precedence matrix, stable RFC 9457 fields/codes, safe details, trace identifiers, and secret/SQL/stack-trace exclusion in `src/test/java/com/alexastudillo/geographicreference/adapter/in/rest/RequestValidationTest.java` and `src/test/java/com/alexastudillo/geographicreference/adapter/in/rest/ProblemMapperTest.java`
- [ ] T011 [P] Add failing hermetic OIDC tests for bearer-only identity, forwarded identity-header rejection, RS256, issuer, audience, expiry, optional not-before, subject, exact read/observe scope separation, identity-first `401`/`403`, 30-second initial-key timeout, fixed five-minute refresh, atomic rotation, cached-key validation plus warning/metric on refresh failure, one-minute unknown-key rate limiting, empty-key readiness, and restart while the issuer is unavailable in `src/test/java/com/alexastudillo/geographicreference/infrastructure/security/OidcSecurityTest.java`
- [ ] T012 [P] Add failing reactive tests for event-loop blocking detection, framework-owned subscription, failure-cause propagation, no shared concurrent session, and the expected one-second blocked-event-loop canary signature in `src/test/java/com/alexastudillo/geographicreference/support/ReactiveBehaviorTest.java` and `src/test/java/com/alexastudillo/geographicreference/support/BlockedEventLoopCanaryTest.java`
- [ ] T013 [P] Add failing clean-PostgreSQL-18 V001 tests for schemas, types, stable registries, revision snapshots, generated ranges, strict intervals, preferred-name exclusions, hierarchy/identifier constraints, active views, indexes, PUBLIC revocation, and absence of credentials in `src/test/java/com/alexastudillo/geographicreference/migration/SchemaMigrationTest.java`
- [ ] T014 [P] Add failing catalog-tool tests using only synthetic archives for pinned-input rejection, exact manifest shape, NFC, RFC 8785 bytes, duplicate/orphan/exclusion failures, fixed counts, approval-reference binding, deterministic digest, smoke output, and no operator override of manifest rules in `src/test/java/com/alexastudillo/geographicreference/catalogtool/CatalogSourcePipelineTest.java`
- [ ] T015 [P] Add failing generated-migration tests using synthetic manifests for clean V001/V002/V003 execution, candidate atomicity, invalid-data rollback, active-pointer preservation, source/provenance/count checks, registry non-reuse, and independent version-2 digest equality across all 21 logical sections in `src/test/java/com/alexastudillo/geographicreference/migration/GeneratedCatalogMigrationTest.java`
- [ ] T016 [P] Add failing PostgreSQL 18 tests for every injected finalization-stage rollback, corrected rerun, a second idempotent run against the accepted final state, and two simultaneous finalizers proving advisory-lock blocking, exactly one state transition, and acceptance of the committed state, plus exact `pg_auth_members` options, owner transfer, migrator hardening, login-specific runtime defaults, and reconnect behavior in `src/test/java/com/alexastudillo/geographicreference/migration/RoleFinalizationTest.java`
- [ ] T017 [P] Add failing runtime-login tests that verify all login defaults and timeout behavior, explicitly override `default_transaction_read_only` before the full matrix, execute every approved SELECT/built-in, and reject INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER, temporary objects, DDL, sequence/internal/inactive/Flyway-history access, mutation-capable routines, ownership changes, `SET ROLE`, grants/grant options, privileged attributes/memberships, default-privilege inheritance, and escalation in `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/RuntimeRolePrivilegeTest.java`
- [ ] T018 [P] Add failing documentation consistency tests for every required implementation document, canonical OpenAPI identity, implemented/proposed labels, read-only vocabulary, migration/runtime separation, and absence of secret examples in `src/test/java/com/alexastudillo/geographicreference/documentation/DocumentationConsistencyTest.java`

### Foundational Implementation

- [ ] T019 Implement immutable shared values and policies in `src/main/java/com/alexastudillo/geographicreference/domain/shared/LanguageTag.java`, `src/main/java/com/alexastudillo/geographicreference/domain/shared/NameType.java`, `src/main/java/com/alexastudillo/geographicreference/domain/shared/LifecycleStatus.java`, `src/main/java/com/alexastudillo/geographicreference/domain/shared/ValidityPeriod.java`, `src/main/java/com/alexastudillo/geographicreference/domain/shared/QueryDate.java`, `src/main/java/com/alexastudillo/geographicreference/domain/shared/PageRequest.java`, `src/main/java/com/alexastudillo/geographicreference/domain/shared/CatalogRevision.java`, `src/main/java/com/alexastudillo/geographicreference/domain/shared/Provenance.java`, `src/main/java/com/alexastudillo/geographicreference/domain/shared/VisibilityPolicy.java`, and `src/main/java/com/alexastudillo/geographicreference/domain/shared/NameSelectionPolicy.java`
- [ ] T020 Implement bounded `PageResult`, query context, authorization context, typed query failures, and stable problem codes without transport or persistence types in `src/main/java/com/alexastudillo/geographicreference/application/result/PageResult.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/QueryContext.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/AuthorizationContext.java`, `src/main/java/com/alexastudillo/geographicreference/application/result/QueryFailure.java`, and `src/main/java/com/alexastudillo/geographicreference/application/result/ProblemCode.java`
- [ ] T021 Implement the fixed query-surface allowlists, singleton detection, pagination/language/date/name-type validation, precedence coordinator, RFC 9457 DTO, and safe failure mapper in `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/validation/RequestValidator.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/validation/RequestPrecedence.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/problem/ProblemResponse.java`, and `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/problem/ProblemMapper.java`
- [ ] T022 Implement the ADR 0001 trusted-gateway OIDC profile, exact `geographic-reference.read`/`geographic-reference.observe` permission mapping, canonical authentication/authorization failures, and bounded issuer/JWKS key-state readiness in `src/main/java/com/alexastudillo/geographicreference/infrastructure/security/GatewayIdentity.java`, `src/main/java/com/alexastudillo/geographicreference/infrastructure/security/PermissionPolicy.java`, and `src/main/java/com/alexastudillo/geographicreference/infrastructure/security/SecurityFailureHandler.java`
- [ ] T023 Implement the non-blocking pre-security known-path Vert.x filter that passes only GET/HEAD and emits canonical bodyless-aware `405` responses for all other methods in `src/main/java/com/alexastudillo/geographicreference/infrastructure/http/KnownPathMethodFilter.java`
- [ ] T024 Implement deterministic representation ETags, `If-None-Match` handling, `Cache-Control: private, no-cache`, catalog-revision headers, atomic response construction, and bodyless HEAD conversion in `src/main/java/com/alexastudillo/geographicreference/infrastructure/http/CatalogResponseFactory.java` and `src/main/java/com/alexastudillo/geographicreference/infrastructure/http/HeadResponseFilter.java`
- [ ] T025 [P] Implement the mandatory blocked-thread JUnit extension, subscription instrumentation, current-report canary verification, and 200 ms/50 ms Vert.x test thresholds in `src/test/java/com/alexastudillo/geographicreference/support/FailOnBlockedEventLoopExtension.java`, `src/test/java/com/alexastudillo/geographicreference/support/SubscriptionProbe.java`, and `src/test/resources/application.properties`
- [ ] T026 [P] Implement digest-pinned PostgreSQL 18 Testcontainers support, migration/source fixture profiles, statement counting, runtime-login reconnection, and server-version assertions in `src/test/java/com/alexastudillo/geographicreference/support/Postgres18TestResource.java`, `src/test/java/com/alexastudillo/geographicreference/support/StatementCountingPool.java`, and `src/test/java/com/alexastudillo/geographicreference/support/SyntheticCatalogFixture.java`
- [ ] T027 [P] Configure only the reactive runtime datasource, runtime identity, statement timeout, expected schema/catalog revision, OIDC/JWKS bounds, JSON logs, trace correlation, application-owned operational behavior, 30-second shutdown, and suppression of extension-owned health/info/metrics/OpenAPI/UI routes in `src/main/resources/application.properties`
- [ ] T028 Implement immutable V001 with PostgreSQL 18 schemas, roles, types, stable identity/registry tables, revision/source/provenance/coverage/snapshot tables, constraints, indexes, active views, PUBLIC revocations, and exact reviewed SELECT grants in `database/migration/V001__create_geographic_catalog.sql`
- [ ] T029 Implement secret-free initial migrator preparation and advisory-locked fail-atomic role finalization from ADR 0002 in `database/bootstrap/prepare-initial-migrator.sql` and `database/bootstrap/finalize-runtime-login.sql`
- [ ] T030 [P] Implement the isolated deterministic source extractor, independent RFC 8785 validator, approval-reference binder, smoke-fixture builder, and version-2 21-section relational digest producer in `src/catalogTool/java/com/alexastudillo/geographicreference/catalogtool/CatalogSourceExtractor.java`, `src/catalogTool/java/com/alexastudillo/geographicreference/catalogtool/DerivedManifestValidator.java`, `src/catalogTool/java/com/alexastudillo/geographicreference/catalogtool/RelationalProjectionDigest.java`, and `src/catalogTool/java/com/alexastudillo/geographicreference/catalogtool/CatalogSourceValidationMain.java`
- [ ] T031 Implement deterministic V002/V003 SQL assembly from a validated manifest, with explicit columns, fixed UUIDv7 values, no secrets, staged candidate loading, SQL-side 21-section digest verification, exact assertions, and final pointer activation in `src/catalogTool/java/com/alexastudillo/geographicreference/catalogtool/CatalogMigrationAssembler.java`

**Checkpoint**: Clean Architecture, safe HTTP behavior, OIDC access, reactive execution,
PostgreSQL 18 schema/roles, synthetic catalog tooling, and legal-gate enforcement are testable and
implemented without source-derived INEC artifacts.

---

## Phase 3: User Story 1 - Resolve a Country (Priority: P1)

**Goal**: Let an authorized consumer inspect catalog metadata, list bounded current countries,
resolve one country through any ISO code, and list its literal-filtered names.

**Independent Test**: With the fixed synthetic manifest before approval, resolve one fabricated
country by alpha-2, alpha-3, and three-digit numeric code and obtain the same canonical country,
selected name, provenance, and revision; verify country listing and metadata without invoking any
division query. After approval, repeat against all 249 manifest countries.

**Traceability**: FR-001 through FR-005, FR-013 through FR-018, QR-001 through QR-005, HC-001
through HC-003, SC-001, SC-005, SC-009, SC-011, SC-012, and SC-013.

### Tests For User Story 1 (write all before implementation)

- [ ] T032 [P] [US1] Add failing domain tests for alpha-2/alpha-3/numeric classification, locale-neutral uppercase normalization, leading zero preservation, malformed versus unknown codes, immutable country/name/provenance models, and no independence projection in `src/test/java/com/alexastudillo/geographicreference/domain/country/CountryCodeTest.java` and `src/test/java/com/alexastudillo/geographicreference/domain/country/CountryTest.java`
- [ ] T033 [P] [US1] Add failing application tests for active catalog metadata, revision propagation, source/coverage/exclusion mapping, unavailable revision, and reactive failure propagation in `src/test/java/com/alexastudillo/geographicreference/application/CatalogQueryServiceTest.java`
- [ ] T034 [P] [US1] Add failing application tests for country list/resolution/name-list orchestration, normalization, bounds, literal filters, empty pages, selected names, ER-006 precedence, not-found behavior, and one repository invocation in `src/test/java/com/alexastudillo/geographicreference/application/CountryQueryServiceTest.java`
- [ ] T035 [P] [US1] Add failing PostgreSQL 18 reactive tests proving catalog metadata uses one prepared statement and returns exact revision-bound source, coverage, count, language, scheme, type, and exclusion projections in `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/CatalogRepositoryIntegrationTest.java`
- [ ] T036 [P] [US1] Add failing PostgreSQL 18 reactive tests for all country-code forms, 101-row bounded pagination, alpha-2/name ordering, literal name filters, active-view isolation, selected-artifact provenance, timeouts, and one statement per response, plus a source-gated approved-profile matrix resolving every manifest country through alpha-2, alpha-3, and numeric code in `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/CountryRepositoryIntegrationTest.java` and `src/test/java/com/alexastudillo/geographicreference/acceptance/ApprovedCountryCatalogAcceptanceTest.java`
- [ ] T037 [P] [US1] Add failing authenticated GET/HEAD contract tests for `/v1/catalog`, including exact metadata, ETag/304 behavior, bodyless HEAD parity, unsupported inputs, dependency failure, revision mismatch, and safe RFC 9457 output in `src/test/java/com/alexastudillo/geographicreference/contract/CatalogContractTest.java`
- [ ] T038 [P] [US1] Add failing complete GET/HEAD contract tests for `/v1/countries`, `/v1/countries/{countryCode}`, and `/v1/countries/{countryCode}/names`, including all parameters/combinations, first/final/out-of-range pages, repeated/unknown input, code normalization, every allowed name type, access precedence, ETags, empty results, and failures in `src/test/java/com/alexastudillo/geographicreference/contract/CountryContractTest.java`

### Implementation For User Story 1

- [ ] T039 [US1] Implement immutable catalog metadata, coverage, exclusion, country code, country, and country-name read models in `src/main/java/com/alexastudillo/geographicreference/domain/catalog/CatalogMetadata.java`, `src/main/java/com/alexastudillo/geographicreference/domain/catalog/CatalogCoverage.java`, `src/main/java/com/alexastudillo/geographicreference/domain/catalog/CatalogExclusion.java`, `src/main/java/com/alexastudillo/geographicreference/domain/country/CountryCode.java`, `src/main/java/com/alexastudillo/geographicreference/domain/country/Country.java`, and `src/main/java/com/alexastudillo/geographicreference/domain/country/CountryName.java`
- [ ] T040 [US1] Define explicit query values and query-only input ports in `src/main/java/com/alexastudillo/geographicreference/application/query/GetCatalogQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ListCountriesQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ResolveCountryQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ListCountryNamesQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/port/in/CatalogQueries.java`, and `src/main/java/com/alexastudillo/geographicreference/application/port/in/CountryQueries.java`
- [ ] T041 [US1] Define query-only, non-generic `CatalogQueryRepository` and `CountryQueryRepository` output ports with `Uni` single results and `Uni<PageResult<T>>` collections in `src/main/java/com/alexastudillo/geographicreference/application/port/out/CatalogQueryRepository.java` and `src/main/java/com/alexastudillo/geographicreference/application/port/out/CountryQueryRepository.java`
- [ ] T042 [US1] Implement access-first catalog/country orchestration, error precedence, normalization, effective-date handling, fallback selection, and failure preservation in `src/main/java/com/alexastudillo/geographicreference/application/query/CatalogQueryService.java` and `src/main/java/com/alexastudillo/geographicreference/application/query/CountryQueryService.java`
- [ ] T043 [US1] Implement named, explicitly columned, single-statement metadata/country/name SQL with active-revision isolation, fixed filters/order, 101-row pagination, selected-name fallback, and outcome discriminators in `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/sql/CatalogSql.java` and `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/sql/CountrySql.java`
- [ ] T044 [US1] Implement reactive prepared-query execution and row-to-domain mapping without JDBC, ORM, N+1 reads, or manual subscription in `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/repository/VertxCatalogQueryRepository.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/repository/VertxCountryQueryRepository.java`, and `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/mapper/CountryRowMapper.java`
- [ ] T045 [US1] Implement transport-only catalog/country/page/provenance DTOs and explicit domain-to-response mapping that omits UUIDs, ownership, audit principals, row versions, and independence in `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/CatalogResponse.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/CountryResponse.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/CountryNameResponse.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/PageResponse.java`, and `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/mapper/CountryResponseMapper.java`
- [ ] T046 [US1] Implement only the approved authenticated GET/HEAD catalog and country endpoints through input ports, shared validation, RFC 9457 mapping, ETags, revision headers, and bodyless HEAD behavior in `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/resource/CatalogResource.java` and `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/resource/CountryResource.java`

**Checkpoint**: US1 is independently usable against synthetic data and exposes no division or
mutation dependency. Source-backed distributable acceptance remains gated by Phase 7.

---

## Phase 4: User Story 2 - Resolve And Browse Ecuadorian Divisions (Priority: P2)

**Goal**: Let an authorized consumer list Ecuador division types/roots, resolve canonical and
external identifiers, list names/direct children, and obtain at most two ancestors without
loading a tree.

**Independent Test**: In the fixed synthetic Ecuador-like fixture, resolve one fabricated level-
three division through its canonical code and every approved synthetic identifier, then verify
root, child, name, and immediate-parent-to-root results are bounded, correctly ordered, and never
cross the country boundary.

**Traceability**: FR-006 through FR-012, FR-019 through FR-020, QR-001 through QR-005, QC-001,
SC-002, SC-005, SC-008, SC-011, and SC-012.

### Tests For User Story 2 (write all before implementation)

- [ ] T047 [P] [US2] Add failing domain tests for exact 2/4/6 digit canonical codes, significant zeroes, fixed type/level coupling, uppercase schemes, DPA/ISO identifier normalization, unsupported schemes, and immutable division models in `src/test/java/com/alexastudillo/geographicreference/domain/division/DivisionCodeAndIdentifierTest.java`
- [ ] T048 [P] [US2] Add failing domain tests for same-country adjacent parentage, root/child rules, shared level-three types, cycle rejection, direct-child-only navigation, and immediate-parent-to-root maximum-two ancestor ordering in `src/test/java/com/alexastudillo/geographicreference/domain/division/DivisionHierarchyTest.java`
- [ ] T049 [P] [US2] Add failing application tests for all seven division intentions, bounded results, canonical/identifier resolution equivalence, literal name filters, empty leaf/root results, country/coverage/resource precedence, and reactive failure propagation in `src/test/java/com/alexastudillo/geographicreference/application/DivisionQueryServiceTest.java`
- [ ] T050 [P] [US2] Add failing PostgreSQL 18 reactive tests for ordered active division types and roots, 101-row pagination, Ecuador scoping, known non-Ecuador coverage outcomes, and one prepared statement in `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/DivisionBrowseRepositoryIntegrationTest.java`
- [ ] T051 [P] [US2] Add failing PostgreSQL 18 reactive tests for canonical code, both identifier schemes, normalized ISO values, literal division-name filters, active identifier/name visibility, not-found discriminators, provenance, and one statement per response, plus a source-gated approved-profile matrix resolving all 1,293 divisions canonically and all 1,293 DPA/24 ISO identifiers in `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/DivisionResolutionRepositoryIntegrationTest.java` and `src/test/java/com/alexastudillo/geographicreference/acceptance/ApprovedDivisionCatalogAcceptanceTest.java`
- [ ] T052 [P] [US2] Add failing PostgreSQL 18 recursive-query and plan tests for direct children, depth-two ancestor bounds, `CYCLE` defense, deterministic order, no N+1 reads, supporting indexes, and evidence-based acceptance of small-table sequential scans in `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/DivisionHierarchyPlanTest.java`
- [ ] T053 [P] [US2] Add failing authenticated GET/HEAD contract tests for all seven division paths, pagination, hierarchy bounds, canonical/scheme normalization, names, bodyless HEAD parity, ETags, errors, and no descendant/full-tree operation in `src/test/java/com/alexastudillo/geographicreference/contract/DivisionContractTest.java`
- [ ] T054 [P] [US2] Add failing mixed-precedence contract cases for malformed/unknown/known-non-Ecuador/Ecuador country context, invalid downstream values, dependency failure, date-specific country invisibility, and division-type current invisibility in `src/test/java/com/alexastudillo/geographicreference/contract/DivisionPrecedenceContractTest.java`

### Implementation For User Story 2

- [ ] T055 [US2] Implement immutable division type, canonical code, identifier scheme/value, division, division name, summary, and bounded ancestor-chain models in `src/main/java/com/alexastudillo/geographicreference/domain/division/DivisionType.java`, `src/main/java/com/alexastudillo/geographicreference/domain/division/DivisionCode.java`, `src/main/java/com/alexastudillo/geographicreference/domain/division/DivisionIdentifier.java`, `src/main/java/com/alexastudillo/geographicreference/domain/division/Division.java`, `src/main/java/com/alexastudillo/geographicreference/domain/division/DivisionName.java`, `src/main/java/com/alexastudillo/geographicreference/domain/division/DivisionSummary.java`, and `src/main/java/com/alexastudillo/geographicreference/domain/division/AncestorChain.java`
- [ ] T056 [US2] Define the seven explicit division query values and input port in `src/main/java/com/alexastudillo/geographicreference/application/query/ListDivisionTypesQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ListRootDivisionsQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ResolveDivisionQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ResolveDivisionIdentifierQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ListDivisionNamesQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ListChildrenQuery.java`, `src/main/java/com/alexastudillo/geographicreference/application/query/ListAncestorsQuery.java`, and `src/main/java/com/alexastudillo/geographicreference/application/port/in/DivisionQueries.java`
- [ ] T057 [US2] Define the non-generic `DivisionQueryRepository` output port with explicit type/root/canonical/identifier/name/child/ancestor query methods and bounded Mutiny results in `src/main/java/com/alexastudillo/geographicreference/application/port/out/DivisionQueryRepository.java`
- [ ] T058 [US2] Implement identity-first and country-format/dependency/identity/coverage-first division orchestration, downstream validation, visibility, limits, and typed outcome mapping in `src/main/java/com/alexastudillo/geographicreference/application/query/DivisionQueryService.java`
- [ ] T059 [US2] Implement named, explicitly columned single-statement SQL for types, roots, canonical/identifier resolution, names, direct children, and depth-two ancestors with fixed country scope, filters, ordering, bounds, and outcome discriminators in `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/sql/DivisionSql.java`
- [ ] T060 [US2] Implement the reactive division repository and explicit row mappings without tree aggregation, dynamic expressions, JDBC, ORM, or manual subscription in `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/repository/VertxDivisionQueryRepository.java` and `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/mapper/DivisionRowMapper.java`
- [ ] T061 [US2] Implement transport-only division/type/identifier/name/ancestor DTOs and mapping that publishes logical codes while omitting internal IDs and persistence metadata in `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/DivisionResponse.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/DivisionTypeResponse.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/DivisionIdentifierResponse.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/DivisionNameResponse.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/dto/AncestorResponse.java`, and `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/mapper/DivisionResponseMapper.java`
- [ ] T062 [US2] Implement only the seven approved authenticated GET/HEAD division endpoints through `DivisionQueries`, shared precedence/validation, revision/ETag headers, and bodyless HEAD conversion in `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/resource/DivisionResource.java`

**Checkpoint**: US1 and US2 provide independently testable, bounded country and Ecuador-like
division journeys using only public logical identifiers.

---

## Phase 5: User Story 3 - Resolve Localized Or Historical Data (Priority: P3)

**Goal**: Apply exact/primary/default presentation-name selection and consistent current or
explicit historical visibility across every country and division capability.

**Independent Test**: Against a visibly synthetic history fixture containing exact, primary,
missing, draft, active, deprecated, retired, future, expired, and adjacent-period rows, verify
every point immediately before, at, and after each boundary plus UTC-date ETag rollover.

**Traceability**: QR-004, QR-005, LR-001 through LR-008, HC-001, CR-006, SC-003, SC-004,
SC-006, and SC-009.

### Synthetic Test Setup For User Story 3

- [ ] T063 [US3] Add visibly artificial exact/primary/missing translation and lifecycle/boundary records, without any source-derived value, in `src/test/resources/fixtures/synthetic-catalog-history-v1.json`

### Tests For User Story 3 (write all before production implementation)

- [ ] T064 [P] [US3] Add failing exhaustive domain tests for canonical/regional/unsupported language tags, exact-primary-default fallback, literal name filtering, draft exclusion, current versus historical lifecycle, open/half-open periods, adjacent boundaries, and dependency visibility in `src/test/java/com/alexastudillo/geographicreference/domain/shared/LocalizationAndTemporalPolicyTest.java`
- [ ] T065 [P] [US3] Add failing application tests applying one effective UTC date, coverage-start precedence, valid future `asOf`, fallback, inherited country/type/ancestor/name/identifier visibility, and atomic failure outcomes to country and division queries in `src/test/java/com/alexastudillo/geographicreference/application/HistoricalQueryServiceTest.java`
- [ ] T066 [P] [US3] Add failing PostgreSQL 18 reactive tests for every lifecycle/interval boundary, preferred-name period, retired code non-reuse, identifier visibility, dependency visibility, literal name order, selected fallback, and one-revision/one-statement snapshots in `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/HistoricalRepositoryIntegrationTest.java`
- [ ] T067 [P] [US3] Add failing GET/HEAD acceptance tests for exact/primary/default localization, explicit historical dates, current exclusions, future dates, all coverage boundaries, and all applicable country/division paths in `src/test/java/com/alexastudillo/geographicreference/contract/HistoricalAndLocalizedContractTest.java`
- [ ] T068 [P] [US3] Add failing cache tests proving normalized dimensions produce equivalent validators, every unchanged cacheable request returns `304`, and catalog revision or effective UTC-date rollover changes the ETag and returns a body in `src/test/java/com/alexastudillo/geographicreference/contract/CatalogEtagContractTest.java`
- [ ] T069 [P] [US3] Add failing reactive consistency tests proving one prepared statement per database-backed response, zero statements for pre-database outcomes, no mixed revisions, timeout/unavailability as one safe `503`, and no partial body in `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/QuerySnapshotConsistencyTest.java`

### Implementation For User Story 3

- [ ] T070 [US3] Complete lifecycle/dependency visibility and exact-primary-default presentation selection for current and historical query modes in `src/main/java/com/alexastudillo/geographicreference/domain/shared/VisibilityPolicy.java` and `src/main/java/com/alexastudillo/geographicreference/domain/shared/NameSelectionPolicy.java`
- [ ] T071 [US3] Apply one effective UTC date, coverage-start checks, historical lifecycle, fallback selection, and failure precedence consistently across `src/main/java/com/alexastudillo/geographicreference/application/query/CountryQueryService.java` and `src/main/java/com/alexastudillo/geographicreference/application/query/DivisionQueryService.java`
- [ ] T072 [US3] Extend the named country/division statements and mappings with historical lifecycle/interval/dependency predicates, literal versus presentation-name semantics, and same-revision snapshot outcomes in `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/sql/CountrySql.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/sql/DivisionSql.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/mapper/CountryRowMapper.java`, and `src/main/java/com/alexastudillo/geographicreference/adapter/out/postgresql/mapper/DivisionRowMapper.java`
- [ ] T073 [US3] Include canonical language, effective `asOf`, pagination/filter dimensions, and authorization context in ETag construction while preserving GET/HEAD parity and atomic errors in `src/main/java/com/alexastudillo/geographicreference/infrastructure/http/CatalogResponseFactory.java`, `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/resource/CountryResource.java`, and `src/main/java/com/alexastudillo/geographicreference/adapter/in/rest/resource/DivisionResource.java`

**Checkpoint**: All catalog paths apply the same localization, temporal, dependency, revision,
and cache-validation semantics.

---

## Phase 6: User Story 4 - Operate The Catalog Safely (Priority: P4)

**Goal**: Deploy externally migrated/finalized catalog data, start only with a SELECT-only
identity, expose exactly five protected operational routes, recover readiness, and stop safely.

**Independent Test**: Exercise startup, liveness, readiness, revision mismatch, database failure
and recovery, metrics/info, graceful shutdown, migration/finalization failure, runtime privilege
denial, image separation, and Quadlet ordering against the fixed deployment matrix.

**Traceability**: OR-001 through OR-009, SR-002 through SR-004, DR-011 through DR-013, SC-008,
SC-010, SC-014, and Constitution Principles X and XI.

### Tests For User Story 4 (write all before implementation)

- [ ] T074 [P] [US4] Add failing authenticated GET/HEAD tests for exactly `/q/health/live`, `/q/health/ready`, `/q/health/started`, `/q/metrics`, and `/q/info`, observation permission, bodyless parity, method exclusion, and confidential-detail suppression in `src/test/java/com/alexastudillo/geographicreference/contract/OperationalContractTest.java`
- [ ] T075 [P] [US4] Add failing reactive startup/readiness/liveness tests for configuration/schema/revision checks, approved query reachability, database timeout/unavailability, incomplete activation, revision mismatch, subsequent recovery without restart/write, zero-statement liveness, and one-statement checks in `src/test/java/com/alexastudillo/geographicreference/infrastructure/operations/CatalogHealthCheckTest.java`
- [ ] T076 [P] [US4] Add failing tests for required safe JSON log fields, trace propagation, request/error/not-found/query/pool/acquisition/readiness/revision metrics, JWKS refresh-failure warning/metric, revision-bound info, and absence of payload/secret/write-command telemetry in `src/test/java/com/alexastudillo/geographicreference/infrastructure/observability/ObservabilityTest.java`
- [ ] T077 [P] [US4] Add failing in-flight shutdown tests proving new work rejection, accepted bounded query completion or atomic failure, resource closure, no partial response or catalog mutation, and termination within 30 seconds in `src/test/java/com/alexastudillo/geographicreference/infrastructure/operations/GracefulShutdownTest.java`
- [ ] T078 [P] [US4] Add failing deployment tests for prepare/migrate/verify/finalize/runtime/smoke/promote order, separate administrator/migration/runtime secrets, failure stop points, recovery-point requirement, no startup Flyway, hardened finalization prerequisite, and the complete token/precedence matrix through the approved catalog and management gateway ingress in `src/test/java/com/alexastudillo/geographicreference/deployment/DeploymentOrderTest.java`
- [ ] T079 [P] [US4] Add failing image tests for digest-pinned non-root JVM/Flyway/role-management bases, build revision metadata, required port only, and strict runtime/migration/bootstrap/source/credential file separation in `src/test/java/com/alexastudillo/geographicreference/deployment/ContainerImageTest.java`
- [ ] T080 [P] [US4] Add failing Quadlet tests for rootless dry-run, two successful one-shots, finalization-gated runtime, exact secret boundaries, no PostgreSQL publication, ingress assumptions, restart/stop behavior, and 30-second timeout in `src/test/java/com/alexastudillo/geographicreference/deployment/QuadletTest.java`

### Implementation For User Story 4

- [ ] T081 [P] [US4] Implement non-blocking startup, readiness, and liveness checks with expected schema/revision verification, one approved prepared query, independent liveness, and automatic recovery in `src/main/java/com/alexastudillo/geographicreference/infrastructure/operations/CatalogStartupCheck.java`, `src/main/java/com/alexastudillo/geographicreference/infrastructure/operations/CatalogReadinessCheck.java`, and `src/main/java/com/alexastudillo/geographicreference/infrastructure/operations/ProcessLivenessCheck.java`
- [ ] T082 [P] [US4] Implement structured safe access logging, trace correlation, route-category query counters/timers, not-found/error metrics, pool/acquisition gauges, readiness state, active revision observation, and ADR 0001 JWKS refresh-failure warning/metric in `src/main/java/com/alexastudillo/geographicreference/infrastructure/observability/CatalogObservability.java`
- [ ] T083 [US4] Implement application-owned observation-protected GET/HEAD resources for health, OpenMetrics, and safe revision-bound info without enabling stock undeclared management routes in `src/main/java/com/alexastudillo/geographicreference/infrastructure/operations/HealthResource.java`, `src/main/java/com/alexastudillo/geographicreference/infrastructure/operations/MetricsResource.java`, and `src/main/java/com/alexastudillo/geographicreference/infrastructure/operations/InfoResource.java`
- [ ] T084 [US4] Implement shutdown admission control and bounded in-flight query tracking without manual subscription or catalog mutation in `src/main/java/com/alexastudillo/geographicreference/infrastructure/operations/ShutdownCoordinator.java` and `src/main/java/com/alexastudillo/geographicreference/infrastructure/http/ShutdownAdmissionFilter.java`
- [ ] T085 [US4] Finalize production-only health/metrics/info route suppression, OIDC observation policy, startup/readiness values, JSON logging, build metadata, runtime resource closure, and graceful shutdown settings in `src/main/resources/application.properties`
- [ ] T086 [P] [US4] Implement digest-pinned non-root, JVM-only runtime, Flyway-only migration, and bootstrap/finalization-only role-management images with no shared secret or artifact leakage in `src/main/docker/Dockerfile.jvm`, `src/main/docker/Dockerfile.flyway`, and `src/main/docker/Dockerfile.role-management`
- [ ] T087 [US4] Implement rootless migration, finalization, and runtime Quadlets with exact secret possession, `After`/`Requires` ordering, one-shot semantics, loopback/internal ingress assumptions, restart policy, and 30-second stop timeout in `deploy/quadlet/geographic-reference-migration.container`, `deploy/quadlet/geographic-reference-finalization.container`, `deploy/quadlet/geographic-reference-runtime.container`, and `deploy/quadlet/README.md`

**Checkpoint**: The synthetic deployment path proves safe operations and identity separation. A
distributable migration image and production promotion remain prohibited until Phase 7.

---

## Phase 7: Post-Approval Catalog Assembly And Release Gate

**Purpose**: Convert the already tested synthetic pipeline to the pinned source catalog only
after authorized written approval. This phase completes source-backed acceptance for US1-US4.

**Traceability**: DR-002, DR-007, DR-009, DR-013, SC-001, SC-002, SC-013, and SC-015.

- [ ] T088 Have an authorized release reviewer verify the governed INEC approval record's use/scope, decision, approver, and date plus Debian notices/attribution, then record only the approved evidence reference and review outcome in `specs/001-read-geographic-catalog/catalog-source-manifest.md`; stop here if approval is absent or rejected
- [ ] T089 Run the tested `catalogSourceValidation` pipeline against the pinned external archives and evidence directory, independently reproduce the manifest/projection digests, and retain non-placeholder `build/catalog/catalog-derived-manifest-v1.json`, `build/catalog/catalog-derived-manifest-v1.approval.json`, `build/catalog/validation-report.json`, `build/catalog/catalog-revision.txt`, and `build/catalog/smoke-fixture.json` as governed release evidence rather than hand-editing them
- [ ] T090 Have the catalog owner review the actual T089 derived manifest, attribution/license evidence, extraction report, exact counts, exclusions, RFC 8785 digest, relational-projection digest, and public revision; retain the signed decision as `$CATALOG_EVIDENCE_DIRECTORY/catalog-owner-approval.json` and its governed reference in `build/reports/release/catalog-owner-approval-reference.txt`, and do not treat the four-field `build/catalog/catalog-derived-manifest-v1.approval.json` digest artifact as human approval
- [ ] T091 Generate and review immutable, secret-free, source-derived `database/migration/V002__load_initial_catalog_candidate.sql` and `database/migration/V003__validate_activate_initial_catalog.sql` only from the T089 artifacts approved in T090; do not copy source archives or generated response examples into repository source
- [ ] T092 Run the source-backed PostgreSQL 18 migration, digest/count/provenance/constraint/atomicity/recovery, runtime privilege, every-country three-code matrix, and every-division canonical/external-identifier matrix defined in `src/test/java/com/alexastudillo/geographicreference/acceptance/ApprovedCountryCatalogAcceptanceTest.java`, `src/test/java/com/alexastudillo/geographicreference/acceptance/ApprovedDivisionCatalogAcceptanceTest.java`, `src/test/java/com/alexastudillo/geographicreference/migration/GeneratedCatalogMigrationTest.java`, and `src/test/java/com/alexastudillo/geographicreference/adapter/out/postgresql/RuntimeRolePrivilegeTest.java`
- [ ] T093 Build and inspect the now-approved migration image, execute authenticated startup/readiness and `build/catalog/smoke-fixture.json` journeys, and retain migration/image/smoke evidence in `build/reports/release/migration-image-inspection.txt` and `build/reports/release/smoke-test-results.xml` as required by `specs/001-read-geographic-catalog/quickstart.md`

**Checkpoint**: The approved source manifest, immutable migrations, active revision, runtime
queries, migration image, and smoke evidence agree exactly; only now is distributable acceptance
possible.

---

## Phase 8: Polish And Cross-Cutting Read-Only Verification

**Purpose**: Make current-behavior documentation and release evidence match the completed
implementation, then run every constitutional quality gate.

- [ ] T094 [P] Replace scaffold wording with implemented query-only behavior, global-country/Ecuador-division coverage, internal gateway access, external migration/finalization order, legal gate, and validation commands in `README.md`
- [ ] T095 [P] Update explicit C4 context and container views with consuming systems, catalog/management ingress, runtime, migration/finalization one-shots, role-management image, PostgreSQL, identity/secret boundaries, revision-aware readiness, and traffic-promotion order in `docs/architecture/geographic-reference-service-v1.drawio`
- [ ] T096 [P] Align revision scope, stable registries, nullable independence, strict temporal ends, multiple level-three types, schemes, provenance, active pointer, exact indexes/grants, and migration-owned audit fields in `docs/database/v1-schema.dbml`
- [ ] T097 [P] Document temporary PostgreSQL 18 creator grants, exact final memberships/defaults, SELECT-only runtime denial matrix, V001-V003 activation, projection digest, failure recovery, and immutable-forward migration policy in `docs/database/roles-and-privileges.md` and `docs/database/migration-strategy.md`
- [ ] T098 [P] Document ADR 0001 gateway/OIDC trust, issuer/JWKS behavior, exact read/observe permissions, error precedence, alternate-ingress prohibition, secret handling, and logging exclusions in `docs/security/read-access.md`
- [ ] T099 [P] Document all three non-root images, rootless Quadlet units, secret possession, migration/finalization/runtime ordering, ingress/ports, recovery point, startup, shutdown, smoke, and promotion in `docs/deployment/rootless-quadlet.md`
- [ ] T100 [P] Document revision mismatch, database timeout/unavailability and recovery, failed migration/finalization, readiness versus liveness, safe logs/metrics/info, restore/forward-fix rules, and smoke diagnosis in `docs/operations/runbook.md`
- [ ] T101 [P] Document the synthetic-only pre-approval workflow, approved source workflow, Java 25/Gradle commands, PostgreSQL 18/Testcontainers suites, blocked-event-loop canary, contract/privilege/deployment checks, and no-native policy in `docs/local-development.md` and `docs/testing.md`
- [ ] T102 Finalize `.github/workflows/ci.yml` so compilation, formatting, SpotBugs, dependency verification, all named tests, current canary failure signature, JVM build, three non-root image checks, Quadlet validation, digest-only Trivy `docker.io/aquasec/trivy:0.72.0@sha256:cffe3f5161a47a6823fbd23d985795b3ed72a4c806da4c4df16266c02accdd6f` filesystem/image/SBOM gates, digest-only Gitleaks `docker.io/zricethezav/gitleaks:v8.30.1@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f` history scan, and release-evidence retention are mandatory without host-scanner substitution or silent skips
- [ ] T103 Run `documentationTest`; fix current-behavior inconsistencies only in `README.md`, `docs/architecture/geographic-reference-service-v1.drawio`, `docs/database/v1-schema.dbml`, `docs/database/roles-and-privileges.md`, `docs/database/migration-strategy.md`, `docs/security/read-access.md`, `docs/deployment/rootless-quadlet.md`, `docs/operations/runbook.md`, `docs/local-development.md`, and `docs/testing.md`, and restore `src/main/resources/META-INF/openapi.yaml` from the approved `specs/001-read-geographic-catalog/contracts/openapi.yaml` if byte identity fails rather than changing the canonical contract after implementation
- [ ] T104 Execute every access, metadata, country, division, localization/history, ETag/HEAD, precedence, privilege, operations, shutdown, image, Quadlet, and supply-chain scenario in `specs/001-read-geographic-catalog/quickstart.md` without inventing source-derived values
- [ ] T105 Run the complete local/CI gate set from `build.gradle.kts`, verify no mutation route/use case/repository/job/consumer, no unbounded query, no runtime JDBC/Flyway, no leaked secret/source archive, and no HIGH/CRITICAL unsanctioned finding, then record the exact command/result inventory in `build/reports/release-gates.txt`

---

## Dependencies And Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependency.
- **Phase 2 Foundational**: Depends on Phase 1 and blocks all story implementation.
- **Phase 3 US1**: Depends on Phase 2; it is the engineering MVP and supplies country context for
  division stories.
- **Phase 4 US2**: Depends on Phase 2 and US1 country resolution/coverage semantics.
- **Phase 5 US3**: Depends on US1 and US2 because it verifies localization/history across both
  country and division capabilities.
- **Phase 6 US4**: Depends only on Phase 2 and may proceed in parallel with US1-US3 when files do
  not overlap; its final acceptance also exercises their approved queries.
- **Phase 7 Post-Approval**: T088 is the external legal/data-governance gate; T089 depends on T030
  and T088; T090 approves T089's actual generated evidence; T091 depends on T031 and T090; T092
  depends on all selected stories and T091; T093 depends on T092. No step may be skipped.
- **Phase 8 Polish**: Depends on all selected stories; distributable release also depends on
  successful Phase 7.

### Story Dependency Graph

```text
Setup -> Foundational -> US1 -> US2 -> US3
                      \-> US4

Manual approval + Foundational catalog tooling -> Post-Approval Assembly
US1 + US2 + US3 + US4 + Post-Approval Assembly -> Polish/Release Verification
```

### Within Every Story

1. Complete every task in the story's test subsection and confirm failures represent missing
   behavior, not broken test infrastructure.
2. Implement domain values before application ports and orchestration.
3. Implement application ports before PostgreSQL and REST adapters.
4. Keep the canonical OpenAPI resource in place before REST implementation.
5. Run domain/application tests, then PostgreSQL 18 integration tests, then contract tests.
6. Re-run architecture, reactive, method-exclusion, and runtime-role tests before the checkpoint.

### Parallel Opportunities

- All tasks marked `[P]` in a completed phase may run concurrently.
- In each story, domain, application, persistence, and contract test files may be authored in
  parallel, but all must exist before any story implementation task starts.
- US4 tests/implementation may proceed beside US1-US3 after Foundational completes.
- Documentation tasks T094-T101 affect independent files and may run concurrently after behavior
  stabilizes.
- Never parallelize T088-T093 past an incomplete legal, catalog-owner, digest, migration, or
  verification gate.

---

## Implementation Strategy

### Synthetic Engineering MVP

1. Complete Setup and Foundational.
2. Complete every US1 test before US1 implementation.
3. Validate metadata and country journeys against only `synthetic-catalog-v1.json`.
4. Re-run method exclusion, architecture, reactive, and runtime-role denial suites.
5. Do not call this increment distributable or source-backed until Phase 7 passes.

### Incremental Delivery

1. Add US2 bounded Ecuador-like hierarchy behavior without full-tree or descendant queries.
2. Add US3 localization/history semantics across the existing country and division paths.
3. Add US4 safe operations and deployment using only synthetic migration inputs pre-approval.
4. After authorized legal approval and catalog-owner approval of the generated manifest/digests,
   generate production V002/V003 once through the tested deterministic pipeline.
5. Complete documentation, supply-chain, quickstart, and full release verification.

## Notes

- No task adds POST, PUT, PATCH, DELETE, application OPTIONS, generic CRUD, mutation use cases,
  runtime import/publication, scheduled mutation, messaging, caching, native compilation, or a
  second deployable application module.
- Every collection fetches at most 101 rows to return at most 100; ancestor recursion stops at
  depth two; no query returns a full hierarchy.
- Runtime code receives only reactive PostgreSQL dependencies and the SELECT-only credential.
  JDBC and Flyway remain external/test/catalog-tool concerns.
- V001 and ephemeral synthetic V002/V003 test output under `build/` may be implemented
  pre-approval. Production `database/migration/V002__load_initial_catalog_candidate.sql`,
  `database/migration/V003__validate_activate_initial_catalog.sql`, and the migration image
  containing them are post-approval outputs only.
- The first release has no prior supported revision, so clean migration testing is mandatory;
  upgrade testing becomes mandatory after this baseline ships.
