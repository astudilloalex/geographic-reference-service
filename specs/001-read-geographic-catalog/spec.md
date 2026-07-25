# Feature Specification: Read Geographic Catalog API

**Feature Branch**: `1-ft-1`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "Define the main structure of a new independently deployable,
read-only geographic reference microservice. At runtime it exclusively exposes geographic
reference information through query endpoints and never creates, updates, deletes, imports,
activates, deprecates, retires, or otherwise modifies catalog data. High-level architecture
and database definitions are provided under docs/architecture and docs/database."

## Scope and Boundaries *(mandatory)*

### Business Problem and Value

Other internal systems need one dependable answer for country and Ecuadorian administrative
references. Without this service, each consumer can interpret codes, names, hierarchy, and
history differently or couple itself to a shared database. This feature provides a single,
independently deployable query service so consumers can validate and display approved
geographic references while catalog maintenance remains controlled and separate from runtime
traffic.

### Glossary

- **Catalog revision**: Immutable identity of the exact approved source snapshot being served.
- **Canonical code**: Stable country-scoped code that consumers retain for an Ecuadorian
  division; in v1 this is the official INEC DPA code.
- **`asOf`**: Explicit calendar date used to ask which data is visible within declared
  historical coverage.
- **Lifecycle status**: Catalog state controlling whether a record is visible in current or
  historical queries.
- **Localized-name fallback**: Deterministic selection of an exact language, its primary
  language, or the default source name.

### In Scope

- An independently deployable, read-only catalog for countries, ISO-recognized territories,
  and Ecuador's political-administrative divisions.
- Catalog metadata describing coverage, source authority, source revision, effective date,
  and the immutable catalog revision currently being served.
- Listing and resolving countries by ISO 3166-1 alpha-2, alpha-3, or numeric code.
- Listing country names and selecting a preferred localized country name.
- Listing Ecuadorian administrative division types, root divisions, and direct children for
  the approved three-level v1 coverage.
- Resolving an Ecuadorian administrative division by country-scoped canonical code or an
  approved external identifier.
- Retrieving bounded ancestors and localized names for an administrative division.
- Current-catalog queries and explicit historical queries using an `asOf` calendar date.
- Bounded pagination, deterministic ordering, explicit language fallback, lifecycle and
  temporal visibility, stable errors, and HTTP cache validation.
- Initial schema and catalog establishment through reviewed, immutable SQL migrations using
  the pinned [catalog source manifest](catalog-source-manifest.md): 249 ISO-aligned countries
  and territories, 24 Ecuadorian provinces, 222 cantons, and 1,047 primary level-three areas.
- Readiness behavior that prevents serving an incomplete or unexpected catalog revision.

### Non-Goals

- The runtime application does not create, update, delete, import, activate, deprecate,
  retire, publish, correct, or otherwise modify catalog records.
- Geographic schema and catalog mutation MUST occur only through reviewed, controlled SQL
  migrations and MUST NOT be modeled as application functionality.
- The runtime application MUST NOT provide administration, import, publication,
  lifecycle-command, bulk-upload, database-maintenance, generic CRUD, scheduled mutation,
  or message-consumer capabilities.
- Postal or physical addresses, postal codes, geocoding, reverse geocoding, customer or
  organization coordinates, tax jurisdictions, commercial territories, sales regions,
  service coverage, branches, warehouses, tenants, users, organizations, or
  organization-specific geographic configuration are outside this bounded context.
- Fuzzy, phonetic, full-text, or geospatial search is not part of this feature.
- Unbounded hierarchy export, arbitrary descendant recursion, and ordinary result streaming
  are not part of this feature.
- The 269 urban parishes nested below cantonal-seat areas in the INEC 2026 classifier are not
  part of v1 because they require a fourth hierarchy level or another approved hierarchy
  model. Catalog metadata MUST disclose this exclusion.
- Public or external access, distributed caching, application-managed caching, messaging,
  and shared-database integration are not part of this feature.
- Internal database identifiers and write-oriented row versions are not public integration
  identifiers.

### Consuming Systems and Read Access

- Internal services use published country and division codes to validate or resolve
  geographic references without connecting directly to this service's database.
- Internal application backends browse bounded country and division data for selection and
  presentation workflows.
- Approved historical or localization consumers request names and catalog state for a
  specific language or date.
- Access is internal and authenticated. The approved gateway is the only catalog ingress,
  establishes caller identity, and passes a trusted authentication context. The service
  validates that context and enforces `geographic-reference.read` before parsing catalog
  input or revealing whether a resource exists. No alternate direct identity path is allowed.
- Missing authentication returns `401`; an authenticated caller without the read permission
  receives `403`. There is no application administration permission.

## Read-Only Enforcement *(mandatory)*

- **RO-001**: The v1 catalog and operational API MUST expose only `GET` and `HEAD`. Application
  `OPTIONS` routes are not required because v1 is internal machine-to-machine traffic and
  does not support browser cross-origin access.
- **RO-002**: The HTTP API MUST NOT expose `POST`, `PUT`, `PATCH`, or `DELETE`.
- **RO-003**: The feature MUST NOT introduce a mutation use case, mutation repository
  method, scheduled mutation job, message consumer, startup catalog writer, or hidden write
  path.
- **RO-004**: Runtime PostgreSQL access MUST use a SELECT-only identity.
- **RO-005**: Flyway and catalog SQL MUST execute outside the runtime application identity.
- **RO-006**: A write method requested against a known catalog path MUST return `405` and an
  `Allow` header containing only the safe methods supported by that resource.
- **Evidence**: The canonical contract and registered-route tests prove method exclusion;
  architecture tests prove mutation paths are absent; database privilege tests prove that
  the runtime identity can execute approved reads but cannot execute data, schema, ownership,
  or privilege changes; deployment verification proves migration and runtime credential
  separation.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Resolve a Country (Priority: P1)

An internal consuming service lists the current country catalog or resolves a country or
ISO-recognized territory from an alpha-2, alpha-3, or three-digit numeric ISO 3166-1 code so
that it can retain a stable logical reference without database coupling.

**Why this priority**: Country resolution is the smallest independently useful geographic
capability and is required by all division queries.

**Independent Test**: Given the pinned catalog source and derived-record manifests, resolve the same country using
each of its three ISO codes and verify that every lookup returns the same canonical country,
selected name, provenance, and catalog revision. Verify the bounded country listing against
the manifest independently of all division behavior.

**Acceptance Scenarios**:

1. **Given** current active countries in the approved catalog, **When** a permitted consumer
   requests a page, **Then** the response contains only currently visible countries in stable
   alpha-2 order and identifies the catalog revision.
2. **Given** one visible country, **When** a consumer resolves its alpha-2, alpha-3, or numeric
   code, **Then** every form returns the same canonical representation.
3. **Given** an alphabetic country code in lowercase, **When** a consumer resolves it,
   **Then** it is normalized to uppercase before lookup.
4. **Given** malformed or well-formed but unknown country input, **When** a consumer performs
   the query, **Then** the service returns the corresponding stable validation or not-found
   problem.
5. **Given** the activated initial catalog, **When** a consumer requests catalog metadata,
   **Then** it receives the exact country and Ecuador coverage, source revisions, effective
   dates, exclusions, supported identifier schemes, and immutable catalog revision from the
   pinned source manifest.
6. **Given** a visible country with pinned English source names, **When** a consumer lists
   names with an allowed language or name-type filter, **Then** a bounded literal-filtered
   page is returned; a valid filter with no matches returns an empty page.

---

### User Story 2 - Resolve and Browse Ecuadorian Divisions (Priority: P2)

An internal consumer lists Ecuador's administrative division types and navigates from roots
to direct children, or resolves a division by canonical code or approved external identifier,
without loading the complete hierarchy. The three-level v1 view includes provinces, cantons,
and primary local areas while explicitly excluding nested urban parishes.

**Why this priority**: Division resolution allows consuming systems to store stable logical
references and supports bounded geographic selection after country resolution is available.

**Independent Test**: Using a fixed Ecuador catalog revision, resolve one division through
its canonical code and each approved external identifier, then verify root, direct-child, and
ancestor results against the same manifest. Verify that every collection remains bounded and
that no query crosses the Ecuador country boundary.

**Acceptance Scenarios**:

1. **Given** the Ecuador country and its configured division types, **When** a consumer lists
   types or root divisions, **Then** only currently visible records are returned in stable
   hierarchy and code order.
2. **Given** a visible parent division, **When** a consumer requests its children, **Then**
   only direct children from the immediately following configured level are returned.
3. **Given** a division with canonical and external identifiers, **When** either approved
   identifier is queried with Ecuador as country context, **Then** the same division is
   returned.
4. **Given** a visible level-three area, **When** its ancestors are requested, **Then** the bounded
   chain is returned from immediate parent to root without cycles or unrelated divisions.
5. **Given** a visible country other than Ecuador, **When** any division capability is
   requested, **Then** the service returns `DIVISION_COVERAGE_NOT_AVAILABLE` rather than an
   empty hierarchy or country-not-found result.
6. **Given** a visible Ecuadorian division, **When** a consumer lists its names with allowed
   language or name-type filters, **Then** a bounded, deterministically ordered page is
   returned and valid filters with no matches produce an empty page.

---

### User Story 3 - Resolve Localized or Historical Data (Priority: P3)

An approved internal consumer requests a country or division representation in a supported
language or asks what geographic record was valid on an explicit calendar date.

**Why this priority**: Localization and history make the reference catalog usable across
languages and catalog revisions while preserving strict current-data behavior.

**Independent Test**: Against fixtures containing exact-language, parent-language, missing
translation, deprecated, and retired data, verify deterministic name fallback and test the
instant before, at, and after every temporal boundary.

**Acceptance Scenarios**:

1. **Given** a preferred name for the requested canonical language tag, **When** a consumer
   requests that language, **Then** the exact preferred localized name is selected.
2. **Given** no exact localized name, **When** a consumer requests a regional language tag,
   **Then** the primary-language preferred name is used when available, otherwise the
   record's default name is returned.
3. **Given** historical records, **When** a consumer supplies a valid `asOf` date, **Then**
   only non-draft records whose half-open validity period contains that date are returned.
4. **Given** no `asOf` input, **When** a current query is performed, **Then** deprecated,
   retired, not-yet-valid, and expired records are excluded.
5. **Given** the UTC date crosses a validity boundary without a catalog revision change,
   **When** a consumer revalidates a current response, **Then** the ETag changes and the
   newly visible representation is returned instead of `304`.
6. **Given** an `asOf` date before all datasets required by an endpoint are covered, including
   `2025-12-31` for every Ecuador division capability, **When** a historical query is
   performed, **Then** `AS_OF_OUTSIDE_CATALOG_COVERAGE` is returned rather than inferred
   historical data.

---

### User Story 4 - Operate the Catalog Safely (Priority: P4)

A platform operator deploys a validated catalog revision, starts the runtime with its
read-only identity, observes service and catalog health, and promotes traffic only after the
service proves that the expected revision can answer approved queries.

**Why this priority**: Consumers cannot rely on reference data unless deployment prevents
partial catalogs, privileged runtime access, and unhealthy revisions from receiving traffic.

**Independent Test**: Exercise startup, liveness, readiness, revision mismatch, database
failure and recovery, graceful shutdown, and migration failure against a fixed deployment
acceptance matrix while proving the runtime identity cannot modify catalog data.

**Acceptance Scenarios**:

1. **Given** a complete approved migration and matching expected revision, **When** the
   runtime starts with its read-only identity, **Then** startup and readiness succeed and the
   exact build and catalog revisions are observable.
2. **Given** a failed migration, incomplete activation, unavailable database, or revision
   mismatch, **When** readiness is checked, **Then** readiness fails safely and traffic is not
   promoted while liveness remains independent of temporary database loss.
3. **Given** a previously unavailable dependency that becomes healthy with the expected
   revision, **When** the next readiness check succeeds, **Then** readiness is restored without
   modifying catalog data.
4. **Given** graceful shutdown begins, **When** new and in-flight queries are present, **Then**
   new work is rejected, accepted work either completes atomically or fails without a partial
   catalog response, and the process terminates within 30 seconds.

### Edge Cases

- A code with valid syntax but no visible record returns a specific not-found problem rather
  than an empty item or a generic validation error.
- Numeric country codes retain leading zeroes and must contain exactly three digits.
- A syntactically valid but unsupported identifier scheme returns an unsupported-scheme
  problem; a supported scheme with an unknown value returns identifier-not-found.
- An invalid BCP 47 language tag is rejected; a valid unsupported language follows the
  documented fallback instead of failing.
- A collection with no visible records returns a successful empty page.
- Page numbers and sizes that are missing use documented defaults; zero, negative, malformed,
  repeated, or excessive values are rejected. A valid page beyond the final page returns an
  empty page with `hasNext=false`.
- Ecuador hierarchy responses stop at the configured three administrative levels and never
  expose arbitrary recursive descendants.
- `valid_from` is inclusive, `valid_until` is exclusive, and a null boundary is open-ended.
- Repeated safe requests against the same catalog revision, normalized input, effective
  `asOf` date, and authorization context produce equivalent content and cache validators.
- A valid non-Ecuador country returns division-coverage-not-available for every division
  capability; malformed and unknown countries retain their distinct errors.
- A database or query timeout, or failure during a multi-query snapshot, returns one atomic
  `503` problem and never a partial catalog body.
- Temporary database unavailability returns a safe `503` problem and causes readiness, but
  not liveness, to fail.
- A failed catalog migration, incomplete catalog activation, or expected-revision mismatch
  prevents readiness and traffic promotion.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST list currently visible countries and ISO-recognized territories
  using bounded pagination.
- **FR-002**: The system MUST resolve a country by ISO 3166-1 alpha-2, alpha-3, or numeric
  code and MUST preserve leading zeroes in numeric codes.
- **FR-003**: The system MUST use normalized alpha-2 as the canonical country reference in
  links and cross-service examples.
- **FR-004**: A country representation MUST include its stable ISO codes, selected name,
  lifecycle and validity information, approved provenance, and catalog revision. It MUST NOT
  expose or infer an independence indicator because the pinned source does not establish one.
- **FR-005**: The system MUST list localized country names using bounded pagination and
  optional exact `language` and `nameType` filters. Allowed name types are `OFFICIAL`,
  `COMMON`, `SHORT`, `ALTERNATIVE`, and `HISTORICAL`; filters select literal matching records
  and do not invoke presentation-name fallback.
- **FR-006**: The system MUST list the stable Ecuador division type codes `PROVINCE` at level
  one, `CANTON` at level two, and `CANTONAL_SEAT_AREA` plus `RURAL_PARISH` at level three, in
  hierarchy-level then type-code order.
- **FR-007**: The system MUST list Ecuador's currently visible root divisions and direct
  children using bounded pagination.
- **FR-008**: The system MUST resolve an Ecuadorian division by its country-scoped canonical
  INEC DPA code. The canonical form is an unchanged digit string: `PP` for a province,
  `PPCC` for a canton, or `PPCCSS` for a level-three area. Leading zeroes are significant;
  whitespace, signs, separators, and non-digits are invalid.
- **FR-009**: The system MUST support external scheme `EC_INEC_DPA` for every included
  division and `ISO_3166_2` for the 24 provinces. Scheme codes are normalized to uppercase.
  `EC_INEC_DPA` values follow the canonical 2, 4, or 6 digit rules. `ISO_3166_2` values are
  normalized to uppercase and match `EC-[A-Z]{1,2}`. Identifier uniqueness is within country
  and scheme; canonical response values preserve the forms defined here.
- **FR-010**: The system MUST return a division's bounded ancestor chain from immediate parent
  to root.
- **FR-011**: A division representation MUST include its stable canonical code, type, parent
  reference when present, selected name, lifecycle and validity information, approved
  identifiers, provenance, and catalog revision.
- **FR-012**: The system MUST list localized division names using bounded pagination and
  optional exact `language` and `nameType` filters with the same literal filtering and allowed
  name types as country names. The initial manifest contains exactly one preferred `es`
  `OFFICIAL` name for each included division and no other division-name record.
- **FR-013**: The system MUST expose catalog metadata containing coverage, source authority,
  source revision, effective date, migration revision, immutable catalog revision, language
  coverage, historical-coverage start, expected record counts, supported identifier schemes,
  included division levels, and explicit exclusions. Country coverage and division coverage
  MUST be distinguishable.
- **FR-014**: The system MUST expose only records contained in a successfully activated and
  deterministically validated catalog manifest.
- **FR-015**: Invalid input and a well-formed but absent resource MUST produce distinct stable
  problems.
- **FR-016**: Empty collection results MUST return a successful empty page rather than a
  not-found problem.
- **FR-017**: Public representations MUST NOT expose internal UUIDs, migration audit
  principals, database ownership information, or row versions as consumer identifiers or
  update tokens.
- **FR-018**: Consumers MUST be able to complete all catalog journeys using published logical
  identifiers and the HTTP contract without a direct database connection or foreign key.
- **FR-019**: A division capability requested for a visible country other than Ecuador MUST
  return `DIVISION_COVERAGE_NOT_AVAILABLE`. A malformed country still returns invalid format,
  and a well-formed unknown or invisible country still returns country not found. Coverage is
  evaluated before division code, scheme, identifier, language, date, or pagination input.
- **FR-020**: `EC_INEC_DPA` with a syntactically invalid value returns invalid identifier;
  `ISO_3166_2` with a syntactically invalid value returns invalid identifier; any scheme other
  than the two approved v1 schemes returns unsupported scheme; a valid approved value absent
  from the activated manifest returns identifier not found.

### Query Behavior *(mandatory)*

- **QR-001 Pagination**: Collection queries use one-based `page` and `pageSize` inputs. The
  default page size is 50 and the maximum is 100. Responses contain items, effective page,
  effective page size, whether another page exists, and catalog revision. Zero, negative,
  malformed, or excessive values are rejected; no unbounded collection operation exists.
- **QR-002 Filtering and Sorting**: Country collections are ordered by alpha-2 code;
  division types by hierarchy level then code; divisions by canonical code; names by
  canonical BCP 47 language tag, name-type order `OFFICIAL`, `COMMON`, `SHORT`, `ALTERNATIVE`,
  `HISTORICAL`, preferred names first, Unicode NFC name after Unicode default case folding,
  then the original NFC code-point sequence; ancestors from immediate parent to root. Name
  uniqueness within one owner, language, type, and original value prevents a remaining tie.
  Each capability accepts only the inputs listed in the query-capability matrix below.
  Arbitrary sorting, unknown filters, repeated singleton parameters, and transport values that
  could become unrestricted query expressions are rejected.
- **QR-003 Hierarchy Bounds**: The initial Ecuador hierarchy has three configured levels:
  province, canton, and primary local area. Level three contains 222 cantonal-seat areas and
  825 rural parishes. The API lists roots and direct children and returns at most two
  ancestors for a level-three record. It provides no arbitrary recursive-descendant or
  full-tree operation. Cycles or cross-country relationships are invalid catalog data and
  prevent catalog activation.
- **QR-004 Localization**: Consumers use an explicit `language` query input containing a
  canonical BCP 47 tag. Selection order is exact tag, primary-language tag, then the
  entity's default name. Omitting `language` selects the default name. Invalid tags are
  rejected; valid tags without a translation use fallback.
- **QR-005 Temporal Visibility**: `asOf` accepts an ISO calendar date and no timestamp. When
  omitted, the service uses the current UTC calendar date and current-catalog lifecycle
  rules. `valid_from` is inclusive, `valid_until` is exclusive, and null means no known
  record boundary. Draft records are never externally visible. Historical requests before
  the latest coverage start among every dataset required by the requested response return
  `AS_OF_OUTSIDE_CATALOG_COVERAGE` rather than inferring validity from a current snapshot.
  Country and all Ecuador division capabilities therefore start on 2026-01-01; an Ecuador
  division request for 2025-12-31 is outside coverage even though the INEC source is effective
  on that date.

#### Query Capability Matrix

| Capability | Accepted query inputs | Result order and empty behavior |
|------------|-----------------------|---------------------------------|
| Catalog metadata | None | One metadata representation; unavailable revision is `503` |
| Country list | `page`, `pageSize`, `language`, `asOf` | Alpha-2; out-of-range page is empty |
| Country resolution | `language`, `asOf` | One country or country not found |
| Country names | `page`, `pageSize`, `language`, `nameType`, `asOf` | Literal filters; stable name order; no matches is empty |
| Division types | `page`, `pageSize` | Hierarchy level then code; non-Ecuador is coverage unavailable |
| Root divisions | `page`, `pageSize`, `language`, `asOf` | Canonical DPA code; no matches is empty |
| Division resolution | `language`, `asOf` | One division or division not found |
| Identifier resolution | `language`, `asOf` | One division or identifier not found |
| Division names | `page`, `pageSize`, `language`, `nameType`, `asOf` | Literal filters; stable name order; no matches is empty |
| Direct children | `page`, `pageSize`, `language`, `asOf` | Canonical DPA code; leaf children page is empty |
| Ancestors | `language`, `asOf` | Immediate parent to root; root result is empty |

All division capabilities apply authentication, authorization, country format, country
existence, and division coverage in that order before validating remaining path or query
inputs. A valid non-Ecuador country therefore receives `DIVISION_COVERAGE_NOT_AVAILABLE` even
when another division-specific input is invalid; it is never represented as an empty Ecuador
hierarchy. `language` on item and hierarchy queries selects a presentation name using
fallback, while `language` on name-list queries is a literal record filter.

### Query Consistency and Snapshot Semantics *(mandatory)*

- **QC-001**: One database statement supplies each response snapshot by default. A multi-query
  read-only transaction is prohibited unless the approved plan names the affected endpoint,
  demonstrates why one statement cannot satisfy its defined representation, and proves that
  every statement observes the same snapshot and catalog revision.
- **QC-002**: Every successful catalog response identifies the revision used. Independent
  requests may observe a newly promoted revision, but one response MUST NOT mix revisions.
  Consumers reissue a query when they require data from the newly advertised revision.

### HTTP Cache Validation *(mandatory)*

- **HC-001**: Successful item, collection, and catalog-metadata responses provide a
  representation-specific ETag derived from the catalog revision and normalized request
  dimensions, including the effective `asOf` date. A current query therefore receives a new
  validator when the UTC date changes even if the catalog revision does not. A matching
  `If-None-Match` returns `304 Not Modified` with no body. Initial responses use
  `Cache-Control: private, no-cache` so stored data is revalidated. A
  `Last-Modified` validator is not required because no reliable response-level modification
  timestamp is established for this feature.
- **HC-002**: ETag represents read-response cache validation only and MUST NOT imply caller
  update rights or optimistic write concurrency.
- **HC-003**: Application-managed and distributed caching are outside scope; introducing one
  requires measured evidence and a separately approved decision.

### Error Behavior *(mandatory)*

- **ER-001**: Every applicable invalid-input, not-found, access, limit, revision, and
  temporary database failure MUST have a stable application error code and RFC 9457 problem
  type.
- **ER-002**: HTTP errors MUST use `application/problem+json` with type, title, status, safe
  detail, machine-readable code, request or instance reference when appropriate, and trace
  or correlation identifier.
- **ER-003**: The following conditions MUST map to the stated status and stable code:
  invalid country format (`400`, `INVALID_COUNTRY_CODE_FORMAT`); country absent (`404`,
  `COUNTRY_NOT_FOUND`); invalid division code (`400`, `INVALID_DIVISION_CODE_FORMAT`);
  division absent (`404`, `DIVISION_NOT_FOUND`); unsupported identifier scheme (`400`,
  `UNSUPPORTED_IDENTIFIER_SCHEME`); invalid identifier (`400`, `INVALID_IDENTIFIER_FORMAT`);
  identifier absent (`404`, `IDENTIFIER_NOT_FOUND`); invalid language (`400`,
  `INVALID_LANGUAGE_TAG`); invalid date (`400`, `INVALID_AS_OF_DATE`); invalid pagination
  (`400`, `INVALID_PAGINATION`); excessive page size (`400`, `PAGE_SIZE_LIMIT_EXCEEDED`);
  invalid name type (`400`, `INVALID_NAME_TYPE`); repeated singleton input (`400`,
  `DUPLICATE_QUERY_PARAMETER`); date before declared coverage (`400`,
  `AS_OF_OUTSIDE_CATALOG_COVERAGE`); unsupported input (`400`,
  `UNSUPPORTED_QUERY_PARAMETER`); division catalog absent for a visible country (`404`,
  `DIVISION_COVERAGE_NOT_AVAILABLE`); authentication missing (`401`,
  `AUTHENTICATION_REQUIRED`); read permission missing (`403`, `ACCESS_DENIED`); unsafe method
  (`405`, `METHOD_NOT_ALLOWED`); database unavailable (`503`,
  `GEOGRAPHIC_CATALOG_UNAVAILABLE`); database or query timeout (`503`,
  `GEOGRAPHIC_CATALOG_TIMEOUT`); expected revision unavailable (`503`,
  `CATALOG_REVISION_MISMATCH`); and unexpected failure (`500`, `INTERNAL_ERROR`).
- **ER-004**: Problems MUST NOT expose stack traces, SQL, credentials, tokens, connection
  strings, schema internals, or confidential configuration.
- **ER-005**: The specification defines no write-specific conflict, stale-update,
  duplicate-creation, lifecycle-transition, idempotency-key, import, or publication errors.
- **ER-006**: Known `GET` and `HEAD` routes evaluate missing identity, missing route permission,
  country-code format when country context exists, country existence and visibility, division
  coverage when a division capability is requested, remaining path and query input, then
  requested-resource existence. A dependency failure that prevents the next evaluation step
  returns its `503` problem. Authentication and authorization therefore reveal no input
  validity or resource existence, and a visible non-Ecuador country reveals no
  division-specific validation result. `HEAD` uses the same status and headers as `GET`
  without a body. Operational routes use the same identity-first order with the observation
  permission. Unsupported methods, including application `OPTIONS`, return `405` with
  `Allow: GET, HEAD` without evaluating catalog input or resource existence.

### Security and Operational Access Logging *(mandatory)*

- **SR-001**: Catalog access is internal and authenticated through the approved gateway, which
  is the only v1 catalog ingress and establishes caller identity. The application MUST
  validate the trusted authentication context and enforce `geographic-reference.read`;
  direct or alternate identity ingress is prohibited. Missing authentication returns `401`,
  and missing read authorization returns `403` before catalog input is evaluated.
- **SR-002**: Runtime and migration credentials MUST be separate and supplied by approved
  secret management. Runtime processes receive only SELECT privileges and never receive the
  migration credential. Secrets and credential-bearing values MUST NOT appear in source,
  images, logs, health details, metrics, or error responses.
- **SR-003**: Operational access logs MAY record route, status, duration, caller identity,
  catalog revision, and trace identifier. They MUST NOT record credentials, tokens, or full
  response payloads. A separate audit trail for every successful GET is not required.
- **SR-004**: Input values MUST be validated and bounded before querying. No input may become
  an unrestricted sort expression, filter expression, or database command.

### Operational Behavior *(mandatory for independent deployment)*

- **OR-001**: The approved operational routes are `GET /q/health/live`,
  `GET /q/health/ready`, `GET /q/health/started`, `GET /q/metrics`, and `GET /q/info`.
  Every route also supports matching `HEAD`; every other method returns `405` with
  `Allow: GET, HEAD`. No other framework or management route may be exposed in production.
- **OR-002**: Operational routes are reachable only from the approved internal management
  ingress and require an authenticated platform monitoring identity with
  `geographic-reference.observe`. Missing identity returns `401`; an identity without that
  permission returns `403`; both checks occur before operational details are evaluated.
  Health details, metrics, and info MUST NOT expose credentials, connection strings, source
  file locations, or confidential infrastructure data.
- **OR-003**: Startup remains unsuccessful until configuration is valid and the expected
  schema and catalog revision have been verified. Readiness succeeds only while approved
  queries can reach the expected activated revision. Liveness reports process viability and
  MUST NOT fail solely because PostgreSQL is temporarily unavailable.
- **OR-004**: After a database or revision failure clears, a successful subsequent readiness
  check restores readiness without restarting or modifying catalog data.
- **OR-005**: Structured JSON logs MUST carry timestamp, severity, service and build revision,
  trace or correlation identifier, route category, status, and safe diagnostic context.
  Catalog response bodies and secrets MUST NOT be logged.
- **OR-006**: Required metrics cover request count and duration, errors, not-found outcomes,
  database pool utilization, reactive connection acquisition duration, query count by route
  category, readiness state, and current catalog revision. Runtime write, import, publication,
  and command metrics MUST NOT exist.
- **OR-007**: `/q/info` identifies the application version, build revision, expected catalog
  revision, active catalog revision when available, and declared catalog coverage without
  exposing internal credentials or database details.
- **OR-008**: Graceful shutdown stops accepting new work, permits already accepted bounded
  queries to complete within 30 seconds, emits no partial response, closes runtime resources,
  and terminates without changing catalog data. Work that cannot complete within 30 seconds
  fails safely before process termination.
- **OR-009**: Deployment order is migration validation, approved recovery point when required,
  external migration execution, integrity and manifest verification, runtime startup with the
  SELECT-only identity, startup/readiness and smoke verification, then traffic promotion.
  Failure at any step prevents promotion.

### Lifecycle and Temporal Behavior *(mandatory when catalog records are queried)*

- **LR-001**: Current queries return only `ACTIVE` records whose validity interval contains
  the current UTC date. `DRAFT`, `DEPRECATED`, and `RETIRED` records are excluded from current
  listings and resolution.
- **LR-002**: Historical queries require an explicit `asOf` date and may return `ACTIVE`,
  `DEPRECATED`, or `RETIRED` records valid on that date. `DRAFT` is never externally visible.
- **LR-003**: Temporal intervals are half-open: a non-null `valid_from` is inclusive, a
  non-null `valid_until` is exclusive, and null is open-ended. A non-null end MUST be later
  than a non-null start.
- **LR-004**: A division is visible only when its country, type, and required ancestor chain
  are visible for the same query date. A name inherits parent visibility and must also match
  its own validity interval. An identifier must also satisfy its status and validity rules.
  The response coverage start is the latest start among these dependencies.
- **LR-005**: Retired codes and identifiers MUST NOT be silently reused. Historical
  resolution returns the record valid for the requested date or the documented not-found
  problem.
- **LR-006**: Current division-type listings include only `ACTIVE` types. V1 division types
  have no independent validity dates and apply to the entire activated catalog revision.
  Historical division responses may include an `ACTIVE`, `DEPRECATED`, or `RETIRED` type as
  a descriptor when the related division is visible on `asOf`; `DRAFT` types are never
  visible. A future time-varying type model requires a new approved specification and
  migration.
- **LR-007**: Current identifier resolution includes only `ACTIVE` identifiers whose validity
  interval contains the current UTC date. Historical resolution may include `ACTIVE`,
  `DEPRECATED`, or `RETIRED` identifiers whose interval contains `asOf`. Identifier status has
  no `DRAFT`; an identifier outside those rules resolves as not found.
- **LR-008**: The initial snapshots provide no source-backed change history before the
  endpoint coverage dates in the source manifest. The service MUST reject earlier `asOf`
  values, including every Ecuador division request on 2025-12-31, rather than treat null
  source dates as evidence of historical validity.

### Contract Impact *(mandatory for HTTP capabilities)*

- **CR-001**: The canonical OpenAPI change defines versioned, read-only paths for catalog
  metadata, countries, country names, division types, root divisions, division lookup,
  external-identifier resolution, division names, direct children, and ancestors. The
  initial paths are `/v1/catalog`, `/v1/countries`, `/v1/countries/{countryCode}`,
  `/v1/countries/{countryCode}/names`,
  `/v1/countries/{countryCode}/division-types`,
  `/v1/countries/{countryCode}/divisions`,
  `/v1/countries/{countryCode}/divisions/{canonicalCode}`,
  `/v1/countries/{countryCode}/division-identifiers/{schemeCode}/{identifierValue}`,
  `/v1/countries/{countryCode}/divisions/{canonicalCode}/names`,
  `/v1/countries/{countryCode}/divisions/{canonicalCode}/children`, and
  `/v1/countries/{countryCode}/divisions/{canonicalCode}/ancestors`.
- **CR-002**: Each catalog resource supports `GET` and matching `HEAD`. V1 exposes no
  application `OPTIONS` route. `HEAD` applies the same access, validation, coverage, and
  resource rules and returns the same status and headers as `GET` without a body.
- **CR-003**: The contract MUST define normalization, validation, fixed ordering, filters,
  pagination bounds, hierarchy bounds, language fallback, temporal visibility, cache
  validation, access requirements, all problem responses, and representative examples.
- **CR-004**: Contract tests MUST prove `POST`, `PUT`, `PATCH`, and `DELETE` are absent and
  unavailable, application `OPTIONS` is unavailable, and only OR-001 operational routes exist
  in production.
- **CR-005**: Breaking changes to the v1 consumer contract require an explicit versioning
  decision; undocumented routes, fields, or methods are prohibited.
- **CR-006**: The fixed contract acceptance matrix covers every catalog and operational path,
  every allowed and forbidden method, authenticated and unauthenticated access, missing read
  permission, each accepted parameter alone and in allowed combinations, every malformed and
  repeated parameter class, first/final/out-of-range pages, exact/primary/default language
  selection, every allowed name type, current and explicit historical dates, each temporal
  boundary, Ecuador and visible non-Ecuador coverage, canonical and both external identifier
  schemes, database timeout/unavailability, revision mismatch, migration failure, UTC-date
  ETag rollover, and dependency recovery. Success criteria referring to tested requests use
  this complete matrix, not an arbitrary sample.

### Data and Migration Impact *(mandatory)*

- **DR-001**: Initial immutable Flyway migrations are required to create the geographic
  schema, database constraints, roles and grants, catalog-revision metadata, and the approved
  initial country and Ecuador INEC catalogs. The DBML is a design input, not an executable
  schema. The normative source, revision, coverage, extraction, count, hash, and legal rules
  are pinned in [catalog-source-manifest.md](catalog-source-manifest.md).
- **DR-002**: Country data MUST come from Debian `iso-codes` 4.20.1 using the pinned source and
  artifact hashes and deterministic field mapping, producing exactly 249 country and 433
  English country-name records. Ecuador data MUST come from INEC's
  Clasificador Geografico Estadistico 2026 archive with effective date 2025-12-31 and pinned
  archive hash, producing exactly 24 provinces, 222 cantons, and 1,047 primary level-three
  areas. A catalog owner MUST approve the derived manifest, attribution, license evidence,
  legal usage, expected counts, and digest before activation.
- **DR-003**: Catalog records are global and MUST contain no `tenant_id`. Country ISO codes
  are globally unique; division canonical codes are unique within a country; external
  identifiers are unique within their documented country and scheme scope.
- **DR-004**: The persisted model MUST define approved identifier schemes and their
  normalization, character set, case sensitivity, scope, status, and provenance rather than
  treating every scheme code as implicitly supported.
- **DR-005**: Parent and child divisions MUST belong to the same country, follow configured
  adjacent hierarchy levels, reject self-parenting and cycles, and require roots at the first
  configured level. V1 loads provinces at level one, cantons at level two, and 222
  cantonal-seat areas plus 825 rural parishes at level three. It excludes all 269 fourth-level
  urban parishes and every study-zone record identified by the source manifest. The data
  design MUST use public codes `PROVINCE`, `CANTON`, `CANTONAL_SEAT_AREA`, and
  `RURAL_PARISH`, allow the last two types to share hierarchy level three, and enforce
  division-type uniqueness by country and code rather than one type per country and level.
- **DR-006**: Database validity constraints MUST enforce a strict non-empty interval when both
  dates exist. Preferred names MUST be unique for an owner and language at any instant while
  allowing non-overlapping historical preferred names.
- **DR-007**: Provenance MUST cover countries, division types, divisions, names, and
  identifiers directly or through immutable catalog metadata. Audit principals, if retained,
  identify the migration, deployment, or source process and never an HTTP caller.
- **DR-008**: Internal identifiers are UUIDs but are not preferred public references. Row
  version fields, if retained for migration traceability, MUST NOT become write-concurrency
  behavior. The schema MUST allow country independence to remain unknown and the API MUST
  omit that field until an approved source and specification define it.
- **DR-009**: Initial migrations MUST be deterministic from a clean PostgreSQL 18 database,
  atomically activate a complete catalog revision, reject invalid reference data, verify the
  exact source hashes, 249 countries, 1,293 divisions, 1,293 DPA identifiers, 24 ISO 3166-2
  province identifiers, 433 country names, 1,293 division names, and derived-manifest digest,
  and document recovery after failure.
- **DR-010**: There is no previous supported production catalog revision for this initial
  feature, so clean-database migration testing is mandatory and upgrade testing becomes
  mandatory after the baseline is released.
- **DR-011**: Privilege tests MUST prove independent migration and runtime identities and
  rejection of runtime `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, DDL, ownership changes, and
  privilege escalation.
- **DR-012**: SQL catalog changes are migration impact only and MUST NOT become runtime
  application functionality. Migration failure or incomplete activation prevents runtime
  readiness and traffic promotion.
- **DR-013**: INEC-derived data MUST NOT be committed to a distributable artifact, deployed,
  or promoted until the responsible legal or data-governance owner records written approval
  for the intended use. Missing approval blocks activation and MUST NOT cause silent source
  substitution.

### Key Read Models and Domain Concepts *(include if feature involves data)*

- **Catalog Revision**: Immutable identity for one approved, fully activated combination of
  schema and source datasets, including coverage, provenance, effective date, migration
  revision, and validation evidence.
- **Catalog Coverage**: Consumer-visible declaration of country and administrative dataset
  boundaries, languages, historical start dates, identifier schemes, record counts, included
  levels, and explicit exclusions.
- **Country**: Country or ISO-recognized territory identified publicly by normalized ISO
  alpha-2, alpha-3, and numeric codes, with names, lifecycle, temporal validity, and
  provenance. V1 does not infer independence.
- **Country Name**: Localized official, common, short, alternative, or historical country
  name identified by canonical BCP 47 language tag, preference, and temporal validity.
- **Administrative Division Type**: Ecuador-scoped `PROVINCE`, `CANTON`,
  `CANTONAL_SEAT_AREA`, or `RURAL_PARISH` and its one-based hierarchy level. The last two
  public types occupy level three while sharing the same hierarchy depth.
- **Administrative Division**: Ecuadorian political-administrative unit identified publicly
  by a country-scoped canonical code, connected to one type and an optional same-country
  parent, with names, lifecycle, temporal validity, and provenance.
- **Division Identifier**: `EC_INEC_DPA` digit code for any included division or uppercase
  `ISO_3166_2` code for an included province, with country-and-scheme uniqueness, primary
  designation, lifecycle, temporal validity, and provenance.
- **Division Name**: Localized official, common, short, alternative, or historical division
  name with language, preference, and temporal validity.
- **Page Result**: Deterministically ordered, bounded collection result carrying effective
  page information, next-page availability, and catalog revision.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 249 countries in the pinned source manifest resolve to the expected canonical
  record through each declared ISO code, and every other syntactically valid code in the fixed
  acceptance matrix returns the documented not-found problem.
- **SC-002**: All 1,293 included Ecuadorian divisions resolve to exactly one expected record
  through canonical DPA code and all 1,293 DPA plus 24 ISO 3166-2 identifiers resolve to the
  manifest-defined division.
- **SC-003**: Across the complete current-catalog acceptance matrix, zero responses contain a draft,
  deprecated, retired, not-yet-valid, or expired record.
- **SC-004**: Temporal verification passes at every point immediately before, at, and after
  every applicable fixture start, end, and declared coverage boundary in CR-006, proving
  inclusive starts, exclusive ends, and rejection outside source coverage.
- **SC-005**: Every collection returns at most 100 items, uses the documented deterministic
  order, and rejects every excessive or malformed page case in CR-006; no unbounded
  collection or descendant query is present.
- **SC-006**: Every localization case in CR-006 selects names in exact-tag, primary-language,
  and default-name fallback order, and both name-list capabilities apply literal filters,
  stable ordering, and empty-result behavior exactly as specified.
- **SC-007**: Contract and route verification finds zero `POST`, `PUT`, `PATCH`, or `DELETE`
  operations, mutation jobs, message consumers, or undocumented write paths.
- **SC-008**: 100% of attempted runtime data changes, schema changes, ownership changes, and
  privilege escalations are denied while all approved read scenarios succeed.
- **SC-009**: Revalidating every unchanged cacheable path and request dimension in CR-006 with
  its ETag returns `304`; changing the catalog revision or crossing an effective UTC date
  boundary changes the validator and returns the current representation.
- **SC-010**: Every migration, dependency, timeout, and revision failure in CR-006 prevents
  readiness and traffic promotion without exposing confidential diagnostics or partial data,
  and every corresponding recovery case restores readiness after the condition clears.
- **SC-011**: Each consuming-system acceptance journey is completed using only published
  logical codes and the HTTP contract, with zero direct database connections or foreign keys
  from consumers.
- **SC-012**: 100% of documented error scenarios return the expected RFC 9457 type, stable
  code, status, and trace identifier without stack traces, SQL, credentials, or tokens.
- **SC-013**: Catalog metadata reports exactly 249 countries, Ecuador-only division coverage,
  433 English country-name records, 24 provinces, 222 cantons, 1,047 primary level-three
  areas, 1,293 Spanish division-name records, two identifier schemes, declared language and
  historical coverage, all exclusions, and the active revision matching the derived-manifest
  digest.
- **SC-014**: Operational verification exposes only the five approved operational GET routes,
  their matching HEAD routes, and no other management route; distinguishes startup, readiness,
  and liveness in every CR-006 condition; enforces the observation permission; emits the
  required logs and metrics; and completes every graceful-shutdown case within 30 seconds
  without partial responses or catalog mutation.
- **SC-015**: No INEC-derived distributable artifact or production deployment is promoted
  without recorded legal or data-governance approval and source attribution evidence.

## Assumptions

- The service is internal and authenticated by default; external or public exposure requires
  separate architecture and security approval.
- Consumers persist published ISO, canonical division, or approved external identifiers, not
  internal UUIDs.
- Alpha-2 is the canonical country link form, while all three ISO 3166-1 forms remain valid
  lookup inputs.
- The initial country catalog is the 249-record ISO-aligned snapshot and the initial
  administrative catalog is the Ecuador-only three-level snapshot pinned in
  [catalog-source-manifest.md](catalog-source-manifest.md).
- Ecuador's approved initial hierarchy consists of province, canton, and primary local area.
  Nested urban parishes are intentionally deferred; a later source revision or expanded
  coverage triggers model review and a new specification before implementation.
- Calendar-date interpretation uses UTC, and explicit language input is preferred over
  implicit content negotiation.
- Default page size 50 and maximum page size 100 are initial safety bounds; an approved
  contract change may lower them if planning evidence shows a stricter bound is required.
- Catalog mutation is delivered separately from runtime execution through controlled SQL
  migrations using a dedicated migration identity.
- No prior production schema or dataset revision must be upgraded by this initial feature.
- No latency, throughput, or concurrency target is assumed until an approved consumer
  workload supplies evidence for one.

## Dependencies and Risks

- The pinned Debian source is usable only with its LGPL notices. INEC-derived data requires
  recorded legal or data-governance approval for the intended distribution and deployment;
  missing approval blocks activation.
- The approved gateway and identity platform must supply a validated caller identity and the
  `geographic-reference.read` authorization context.
- PostgreSQL 18, separate migration and runtime identities, approved secret management, and a
  controlled external Flyway execution mechanism must be available.
- Catalog ownership must approve coverage, source provenance, code normalization, expected
  record counts, and digests before catalog activation.
- Worldwide country coverage combined with Ecuador-only division coverage may be
  misinterpreted by consumers; catalog metadata and documentation must state coverage
  explicitly.
- The current DBML allows equal temporal endpoints and contains write-oriented audit and
  optimistic-concurrency wording. Implementing it unchanged would contradict this
  specification and the constitution.
- The current architecture diagram places the migrator and database within ambiguous
  boundaries and does not show identity separation, gateway access, revision-aware readiness,
  or deployment ordering.
- The current data model lacks a catalog-level revision model, authoritative identifier
  scheme definitions, and support for multiple division types at one hierarchy level,
  creating readiness, cache validation, normalization, and Ecuador modeling risks until
  corrected through reviewed design and migrations.
- V1 intentionally excludes 269 nested urban parishes. Consumers that require full urban
  subdivision coverage cannot use v1 for that purpose and need a separately approved model
  expansion.
- No response-time objective can be validated until consuming systems provide an approved
  workload; planning must record that evidence before adding a performance commitment.

## Documentation Impact

- Add the canonical OpenAPI v1 contract and read-only examples before endpoint implementation.
- Update the README to distinguish the current scaffold from proposed and implemented
  behavior and to document internal read access and catalog coverage.
- Revise the C4 architecture to show consuming systems, approved gateway, independently
  deployed runtime, external one-shot migrator, PostgreSQL, separate identities, deployment
  ordering, and revision-aware readiness.
- Revise the DBML to remove mutable-runtime implications, enforce exclusive temporal ends,
  define catalog revision and identifier schemes, allow multiple types at hierarchy level
  three, complete provenance, and clarify migration-owned audit fields.
- Preserve the pinned source manifest and document its legal approval, derived-record manifest,
  immutable SQL migration strategy, database grants, migration and runtime secret separation,
  recovery process, and deterministic validation.
- Add security, deployment, rootless service operation, local-development, testing,
  observability, and operational runbook documentation aligned with the permanent read-only
  boundary.
