# Phase 1 Data Model: Read Geographic Catalog

**Feature**: [Read Geographic Catalog API](spec.md)
**Source manifest**: [Initial Catalog Source Manifest](catalog-source-manifest.md)
**Database**: PostgreSQL 18
**Status**: Normative Phase 1 design

## 1. Model Goals

The model provides one immutable, source-backed geographic catalog snapshot to the read-only
runtime. It is designed to:

- represent the 249-record ISO-aligned country catalog and the approved three-level Ecuador
  administrative catalog without tenant or consumer data;
- make catalog revision, source evidence, row provenance, declared coverage, validation, and
  activation explicit and queryable;
- prevent a runtime response from mixing revisions, including during concurrent activation;
- preserve public logical identifiers while keeping internal UUIDs private;
- enforce code formats, temporal intervals, preferred-name uniqueness, identifier non-reuse,
  and hierarchy integrity in PostgreSQL;
- support each database-backed v1 catalog query with at most one bounded, fixed, prepared
  `SELECT` statement; and
- make staged or inactive revisions inaccessible to the runtime database identity.

The model does not provide runtime mutation, generic CRUD, import, publication, lifecycle
commands, arbitrary revision selection, full-tree loading, or tenant-specific geography.

## 2. Invariants

1. Every snapshot row has a deterministic UUIDv7 internal identifier, a non-null
   `catalog_revision_id`, and a non-null row-provenance reference. Snapshot rows are immutable
   after their migration commits.
2. There is no `tenant_id`. Country and division data is global, and other services use public
   codes rather than database foreign keys.
3. A public catalog revision is the literal prefix `sha256:` followed by exactly 64 lowercase
   hexadecimal characters and is derived from the approved derived-record manifest.
4. Exactly one singleton pointer identifies the active revision. Runtime views join through
   that pointer, so unvalidated, staged, and superseded snapshots cannot be read.
5. The active pointer can reference only a validation whose result is `PASS` for the same
   revision. The pointer switch is the final statement of the catalog migration transaction.
6. Country alpha-2, alpha-3, and numeric codes are unique within a revision and are registered
   globally by code kind to one stable country identity. Numeric codes are text and retain all
   three digits, including leading zeroes. A code can never map to another country identity in a
   later revision.
7. Division canonical codes are unique within revision and country and are registered globally
   to one stable division identity. External identifier values are registered globally within
   country and scheme to that same identity. Retained snapshot FKs and global registry keys make
   reassignment across revisions unrepresentable.
8. A division, its type, parent, and country always belong to the same revision and country.
   Roots are level one; every non-root has a parent at exactly the preceding level. Strictly
   decreasing parent levels make cycles impossible.
9. V1 has four Ecuador division type codes over three levels: `PROVINCE` at level 1, `CANTON`
   at level 2, and `CANTONAL_SEAT_AREA` and `RURAL_PARISH` at level 3. Type uniqueness is by
   revision, country, and code, not by hierarchy level.
10. Temporal periods are half-open. `valid_from` is inclusive, `valid_until` is exclusive,
    either boundary may be null, and a present end is strictly later than a present start.
11. At most one preferred name exists for an owner and language at any instant. Non-overlapping
    and adjacent historical preferred-name periods are allowed.
12. Current reads return only `ACTIVE` rows valid on the current UTC date. Reads with explicit
    `asOf` may return `ACTIVE`, `DEPRECATED`, or `RETIRED` rows valid on that date, including a
    future date; `DRAFT` is never externally visible.
13. A division is visible only when its country, type, and complete required ancestor chain are
    visible for the same date. Names inherit owner visibility. Identifiers additionally satisfy
    their own status and validity rules.
14. The source effective date and API historical-coverage start are distinct facts. Ecuador's
    source is effective on 2025-12-31, but every v1 Ecuador division capability starts coverage
    on 2026-01-01 because it depends on the Ecuador country snapshot.
15. Internal UUIDs, migration principals, active-pointer details, ownership, and any retained
    migration row metadata are never public integration identifiers.

## 3. Common Types and Conventions

| Type | Values or rule |
|---|---|
| `geographic_record_status` | `DRAFT`, `ACTIVE`, `DEPRECATED`, `RETIRED` |
| `geographic_identifier_status` | `ACTIVE`, `DEPRECATED`, `RETIRED`; identifiers have no draft state |
| `geographic_name_type` | `OFFICIAL`, `COMMON`, `SHORT`, `ALTERNATIVE`, `HISTORICAL` |
| `catalog_validation_result` | `PASS`, `FAIL` |
| `catalog_coverage_kind` | `COUNTRY`, `ADMINISTRATIVE_DIVISION` |
| `identifier_normalization` | `EXACT`, `UPPERCASE` |
| `country_code_kind` | `ALPHA2`, `ALPHA3`, `NUMERIC` |
| UUID | PostgreSQL `uuid`; catalog migrations provide fixed UUIDv7 literals |
| Hash | Lowercase SHA-256 as `varchar(64)` with a hexadecimal check |
| Language | Canonical BCP 47 tag as `varchar(35)` |
| Display text | Unicode NFC, preserved without trimming, translation, correction, accent removal, or case conversion |

The database encoding is UTF-8. Name ordering and its indexes apply a deterministic code-point
collation to both `name_sort_key` and the original NFC `name`; locale-dependent database
collation is not permitted to change the specified order.

All foreign keys use `ON DELETE RESTRICT`; identity and registry mapping FKs also use
`ON UPDATE RESTRICT`. No snapshot table has cascading update or deletion. Natural keys include
`catalog_revision_id` because the same approved public code may appear unchanged in successive
immutable snapshots. Within the active revision, this is equivalent to the public global or
country-scoped uniqueness stated by the API.

Snapshot tables do not need mutable `created_*`, `updated_*`, or optimistic `version` columns.
The revision, migration version, validation, and provenance records provide migration
traceability. If physical migrations retain additional audit columns, they identify only the
controlled migration process and remain absent from runtime views and public models.

## 4. Stable Identity, Catalog Control, and Evidence Entities

Stable identity and registry entities enforce historical non-reuse across every retained
revision. Catalog-control and evidence entities describe why a snapshot is trustworthy and
whether it can be served. They are internal except for approved metadata projected by
`CatalogMetadata`.

### 4.1 `country_identities`

One immutable logical country identity independent of any snapshot or later code assignment.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `country_identity_id` | `uuid` | No | Primary key; internal physical identity |
| `country_identity_key` | `varchar(100)` | No | Unique immutable logical lineage key; v1 `^COUNTRY/[A-Z]{2}$` |

Initial keys are `COUNTRY/<initial-alpha2>`, for example `COUNTRY/EC`. A future revision retains
the key even if an authority approves an additional code for the same logical country. The key is
internal lineage metadata, not a public API identifier. Unique consistency key
`(country_identity_id, country_identity_key)` supports composite FKs.

### 4.2 `country_code_registry`

An append-only global assignment of every country code to one stable country identity.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `code_kind` | `country_code_kind` | No | Part of primary key |
| `code_value` | `varchar(3)` | No | Part of primary key; conditional format by kind |
| `country_identity_id` | `uuid` | No | FK to `country_identities` |

Primary key `(code_kind, code_value)` permits one identity for a code across all revisions.
Unique mapping key `(code_kind, code_value, country_identity_id)` is the snapshot FK target.
Checks enforce two uppercase letters for `ALPHA2`, three uppercase letters for `ALPHA3`, and
three digits for `NUMERIC`. Registry rows cannot be deleted or remapped while any retained
snapshot references the full mapping.

### 4.3 `division_identities`

One immutable logical identity for an administrative division.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `division_identity_id` | `uuid` | No | Primary key; internal physical identity |
| `division_identity_key` | `varchar(180)` | No | Unique immutable logical lineage key; v1 `^DIVISION/EC/(?:[0-9]{2}\|[0-9]{4}\|[0-9]{6})$` |
| `country_identity_id` | `uuid` | No | Stable owning country identity |
| `country_identity_key` | `varchar(100)` | No | Propagated stable country key |

The country pair has a composite FK to `country_identities`. Initial keys are
`DIVISION/<country-alpha2>/<initial-canonical-code>`, for example `DIVISION/EC/01`. The key is
retained if an approved later revision adds another canonical code for the same logical division.
Consistency key
`(country_identity_id, division_identity_id, division_identity_key)` supports registries and
snapshots.

### 4.4 `division_code_registry`

An append-only country-scoped canonical-code assignment.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `country_identity_id` | `uuid` | No | Stable country identity |
| `canonical_code` | `varchar(64)` | No | Country-scoped canonical code |
| `division_identity_id` | `uuid` | No | Stable division identity |
| `division_identity_key` | `varchar(180)` | No | Propagated logical division key |

Primary key `(country_identity_id, canonical_code)` prevents code reassignment across revisions.
The division triple has a composite FK to `division_identities`. Unique mapping key
`(country_identity_id, canonical_code, division_identity_id, division_identity_key)` is the
snapshot FK target. V1 registry values must be exact 2-, 4-, or 6-digit DPA strings; the snapshot
type/level checks impose the narrower form.

### 4.5 `division_identifier_registry`

An append-only assignment for every external division identifier, including canonical DPA
identifiers represented in the identifier collection.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `country_identity_id` | `uuid` | No | Stable country identity |
| `scheme_code` | `varchar(64)` | No | Uppercase approved scheme code |
| `identifier_value` | `varchar(128)` | No | Canonically normalized value |
| `division_identity_id` | `uuid` | No | Stable division identity |
| `division_identity_key` | `varchar(180)` | No | Propagated logical division key |

Primary key `(country_identity_id, scheme_code, identifier_value)` prevents reassignment even
after retirement or absence from a later snapshot. The division triple has a composite FK to
`division_identities`. Unique mapping key
`(country_identity_id, scheme_code, identifier_value, division_identity_id,
division_identity_key)` is the snapshot FK target. V1 checks enforce the two approved schemes and
their existing syntax; later schemes require an approved schema change rather than a free-form
registry insert.

### 4.6 `catalog_revisions`

One immutable identity for a complete snapshot.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `catalog_revision_id` | `uuid` | No | Primary key; fixed UUIDv7 |
| `revision_code` | `varchar(71)` | No | Unique; literal `sha256:` concatenated with `derived_manifest_sha256` |
| `derived_manifest_sha256` | `varchar(64)` | No | Unique lowercase SHA-256 |
| `projection_sha256` | `varchar(64)` | No | Digest of the deterministic relational projection |
| `schema_version` | `integer` | No | Positive runtime compatibility version |
| `migration_version` | `varchar(50)` | No | Unique immutable Flyway version that introduced the snapshot |
| `recorded_at` | `timestamptz` | No | Controlled migration timestamp |
| `recorded_by` | `varchar(128)` | No | Migration or deployment principal, never an HTTP caller |

Natural key: `revision_code`. `derived_manifest_sha256` must equal the digest suffix in
`revision_code` and the concrete value in the generated derived-manifest approval artifact.
Example, empty, zero-filled, or unresolved values are rejected.

### 4.7 `catalog_sources`

One source dataset used by a revision. The initial revision has the Debian `iso-codes` source
and the INEC classifier source.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `source_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to `catalog_revisions` |
| `source_code` | `varchar(64)` | No | Stable revision-local source code |
| `dataset_name` | `varchar(200)` | No | Published dataset name |
| `authority` | `varchar(160)` | No | Source authority |
| `publisher` | `varchar(160)` | No | Publisher |
| `source_revision` | `varchar(100)` | No | `4.20.1` or classifier revision `2026` |
| `source_uri` | `varchar(500)` | No | Pinned archive URI |
| `release_date` | `date` | Yes | Country release date; null when not published separately |
| `effective_date` | `date` | Yes | Source effective date when supplied |
| `retrieved_on` | `date` | Yes | Retrieval date when recorded |
| `license_expression` | `varchar(100)` | No | License or governed-use expression |
| `attribution` | `text` | No | Required attribution |
| `authority_note` | `text` | Yes | Limits on source authority or redistribution |
| `license_evidence_reference` | `varchar(500)` | No | Retained evidence reference |
| `legal_approval_reference` | `varchar(500)` | Yes | Required and non-null for INEC production activation |

Unique key: `(catalog_revision_id, source_code)`. FK consistency keys also expose
`(catalog_revision_id, source_id)`.

### 4.8 `catalog_source_artifacts`

Pinned archive and selected artifact hashes for one source.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `source_artifact_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to `catalog_revisions` |
| `source_id` | `uuid` | No | Composite FK with revision to `catalog_sources` |
| `artifact_code` | `varchar(80)` | No | Stable role, such as `SOURCE_ARCHIVE` or `ISO_3166_1_JSON` |
| `artifact_reference` | `varchar(500)` | No | Archive URI or path inside the source archive |
| `sha256` | `varchar(64)` | No | Pinned lowercase SHA-256 |

Unique keys: `(catalog_revision_id, source_id, artifact_code)`,
`(catalog_revision_id, source_artifact_id)`, and
`(catalog_revision_id, source_id, source_artifact_id)`. The last key is the provenance FK
target. The initial evidence includes the source archive and two selected artifacts for each
source, six artifact records in total.

### 4.9 `catalog_provenance`

Immutable row-level evidence shared by snapshot rows derived from the same source object. A
snapshot row must not rely only on free-form source columns.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `provenance_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to `catalog_revisions` |
| `source_id` | `uuid` | No | Composite FK with revision to `catalog_sources` |
| `source_artifact_id` | `uuid` | No | Composite FK with revision and source to an artifact |
| `source_record_reference` | `varchar(500)` | No | JSON object, workbook sheet/row, or manifest-rule reference |
| `mapping_rule_code` | `varchar(100)` | No | Versioned deterministic mapping rule |
| `source_value_sha256` | `varchar(64)` | Yes | Digest of the normalized source values when retained |

Unique consistency key: `(catalog_revision_id, provenance_id)`. Every snapshot table has a
composite FK `(catalog_revision_id, provenance_id)` to this entity, preventing cross-revision
provenance. A `UNIQUE NULLS NOT DISTINCT` key on
`(catalog_revision_id, source_id, source_artifact_id, source_record_reference,
mapping_rule_code, source_value_sha256)` gives each logical provenance tuple one row and one
unambiguous projection sort key.

### 4.10 Coverage Entities

`catalog_coverages` declares an independently reportable API boundary.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `coverage_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to `catalog_revisions` |
| `source_id` | `uuid` | No | Composite FK to the governing source |
| `coverage_code` | `varchar(64)` | No | `ISO_COUNTRIES` or `EC_ADMINISTRATIVE_DIVISIONS` |
| `coverage_kind` | `catalog_coverage_kind` | No | Country or administrative-division coverage |
| `country_id` | `uuid` | Yes | Null for global country coverage; Ecuador FK for division coverage |
| `historical_coverage_start` | `date` | No | Earliest supported `asOf`; 2026-01-01 for both v1 coverages |
| `source_effective_date` | `date` | Yes | Null for countries; 2025-12-31 for divisions |
| `description` | `text` | No | Consumer-visible scope statement |

Unique key: `(catalog_revision_id, coverage_code)`. Consistency keys
`(catalog_revision_id, coverage_id)` and
`(catalog_revision_id, country_id, coverage_id)` support child FKs. A composite FK binds a
non-null `country_id` to a country in the same revision.

The following normalized children make metadata deterministic rather than embedding unchecked
JSON:

| Entity | Fields, types, and nullability | Key and foreign-key rules |
|---|---|---|
| `catalog_coverage_counts` | `coverage_id uuid` (not null), `count_code varchar(80)` (not null), `expected_count integer` (not null) | PK `(coverage_id, count_code)`; FK to coverage; count is non-negative |
| `catalog_coverage_languages` | `coverage_id uuid` (not null), `language_tag varchar(35)` (not null) | PK `(coverage_id, language_tag)`; canonical BCP 47 |
| `catalog_coverage_division_types` | `catalog_revision_id uuid` (not null), `country_id uuid` (not null), `coverage_id uuid` (not null), `division_type_id uuid` (not null), `division_type_code varchar(64)` (not null), `hierarchy_level smallint` (not null) | PK `(coverage_id, division_type_id)`; composite FKs bind coverage and the complete type key to the same revision/country and level |
| `catalog_coverage_identifier_schemes` | `catalog_revision_id uuid` (not null), `country_id uuid` (not null), `coverage_id uuid` (not null), `identifier_scheme_id uuid` (not null), `scheme_code varchar(64)` (not null) | PK `(coverage_id, identifier_scheme_id)`; composite FKs bind coverage and the complete scheme key to the same revision and country |
| `catalog_coverage_exclusions` | `coverage_id uuid` (not null), `exclusion_code varchar(80)` (not null), `excluded_count integer` (nullable), `description text` (not null) | PK `(coverage_id, exclusion_code)`; count is non-negative when known |

### 4.11 `catalog_validations` and `catalog_validation_checks`

One validation records the deterministic decision for a revision. Individual checks preserve
the expected and observed evidence used by activation.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `validation_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to `catalog_revisions` |
| `result` | `catalog_validation_result` | No | Only `PASS` can be activated |
| `validated_at` | `timestamptz` | No | Controlled migration timestamp |
| `validated_by` | `varchar(128)` | No | Migration principal |
| `derived_manifest_sha256` | `varchar(64)` | No | Must match the revision |
| `projection_sha256` | `varchar(64)` | No | Must match the loaded projection |
| `evidence_reference` | `varchar(500)` | No | Retained validation report |
| `failure_summary` | `text` | Yes | Safe migration evidence; null for `PASS` |

Unique consistency key: `(catalog_revision_id, validation_id, result)`.

`catalog_validation_checks` contains `validation_id uuid` (not null),
`check_code varchar(100)` (not null), `expected_value text` (not null),
`actual_value text` (not null), `result catalog_validation_result` (not null), and
`evidence_reference varchar(500)` (nullable). Its primary key is
`(validation_id, check_code)`. A passing parent validation requires every child check to pass;
this is asserted by the activation migration before pointer insertion or replacement.

### 4.12 `catalog_active_revision`

The only mutable catalog-control row is the migration-owned singleton pointer.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `singleton_id` | `smallint` | No | Primary key and check `singleton_id = 1` |
| `catalog_revision_id` | `uuid` | No | Unique FK to `catalog_revisions` |
| `validation_id` | `uuid` | No | Validation for the same revision |
| `validation_result` | `catalog_validation_result` | No | Check `validation_result = 'PASS'` |
| `activated_at` | `timestamptz` | No | Controlled activation timestamp |
| `activated_by` | `varchar(128)` | No | Migration principal |
| `activation_migration_version` | `varchar(50)` | No | Migration performing the switch |

The composite FK `(catalog_revision_id, validation_id, validation_result)` references the
matching key on `catalog_validations`. This declaratively prevents a failed or different
revision's validation from being activated.

## 5. Revision-Scoped Snapshot Entities

Country snapshots directly reference their stable identity and all three country-code mappings.
Division snapshots directly reference their stable country/division identities and canonical-code
mapping. Identifier snapshots directly reference the stable division identity and external
identifier mapping. Names, types, and scheme snapshots reference those constrained snapshot
owners through same-revision composite FKs, so every snapshot row is connected transitively to
stable lineage without duplicating registry columns on non-code records.

### 5.1 `countries`

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `country_id` | `uuid` | No | Primary key; fixed UUIDv7 |
| `catalog_revision_id` | `uuid` | No | FK to revision |
| `country_identity_id` | `uuid` | No | Stable country identity |
| `country_identity_key` | `varchar(100)` | No | Propagated logical lineage key |
| `provenance_id` | `uuid` | No | Composite FK to same-revision provenance |
| `alpha2_code` | `varchar(2)` | No | `^[A-Z]{2}$` |
| `alpha2_code_kind` | `country_code_kind` | No | Stored generated constant `ALPHA2` |
| `alpha3_code` | `varchar(3)` | No | `^[A-Z]{3}$` |
| `alpha3_code_kind` | `country_code_kind` | No | Stored generated constant `ALPHA3` |
| `numeric_code` | `varchar(3)` | No | `^[0-9]{3}$`; never numeric data type |
| `numeric_code_kind` | `country_code_kind` | No | Stored generated constant `NUMERIC` |
| `default_name` | `varchar(150)` | No | NFC source `name` |
| `official_name` | `varchar(250)` | No | Source `official_name`, otherwise `default_name` |
| `is_independent` | `boolean` | Yes | Null in v1; omitted from public read models |
| `status` | `geographic_record_status` | No | Lifecycle status |
| `valid_from` | `date` | Yes | Inclusive source validity start |
| `valid_until` | `date` | Yes | Exclusive source validity end |
| `valid_during` | `daterange` | No | Stored generated half-open range |

Unique natural keys: `(catalog_revision_id, alpha2_code)`,
`(catalog_revision_id, alpha3_code)`, and `(catalog_revision_id, numeric_code)`. A unique
consistency key `(catalog_revision_id, country_id)` supports direct owner FKs, and
`(catalog_revision_id, country_id, country_identity_id, country_identity_key)` supports division
FKs. The stable identity pair references `country_identities`. Three composite FKs from
`(alpha2_code_kind, alpha2_code, country_identity_id)`,
`(alpha3_code_kind, alpha3_code, country_identity_id)`, and
`(numeric_code_kind, numeric_code, country_identity_id)` to `country_code_registry` prove that
all three snapshot codes belong permanently to that same logical country.

### 5.2 `country_names`

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `country_name_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to revision |
| `country_id` | `uuid` | No | Composite FK to country in the same revision |
| `provenance_id` | `uuid` | No | Composite FK to same-revision provenance |
| `language_tag` | `varchar(35)` | No | Canonical BCP 47 |
| `name_type` | `geographic_name_type` | No | Public name type |
| `name` | `varchar(250)` | No | Original NFC display value |
| `is_preferred` | `boolean` | No | Preference within owner and language |
| `valid_from` | `date` | Yes | Inclusive |
| `valid_until` | `date` | Yes | Exclusive |
| `valid_during` | `daterange` | No | Stored generated range |
| `name_sort_key` | `text` | No | Stored generated NFC PostgreSQL 18 Unicode default case-folding key |

Natural unique key:
`(catalog_revision_id, country_id, language_tag, name_type, name)`. The original NFC `name`
remains the final code-point tie breaker after `name_sort_key`.

### 5.3 `administrative_division_types`

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `division_type_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to revision |
| `country_id` | `uuid` | No | Composite FK to same-revision country |
| `provenance_id` | `uuid` | No | Composite FK to same-revision provenance |
| `code` | `varchar(64)` | No | Stable uppercase public type code |
| `name` | `varchar(150)` | No | OpenAPI `name` English technical display field |
| `hierarchy_level` | `smallint` | No | V1 check from 1 through 3 |
| `status` | `geographic_record_status` | No | No independent validity dates in v1 |

Natural unique key: `(catalog_revision_id, country_id, code)`. There is deliberately no unique
key on country and hierarchy level. The consistency key
`(catalog_revision_id, country_id, division_type_id, code, hierarchy_level)` supports division
FKs. Activation validates exactly the four approved code/level pairs.

### 5.4 `identifier_schemes`

Persisted scheme definitions prevent arbitrary scheme codes from becoming supported.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `identifier_scheme_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to revision |
| `country_id` | `uuid` | No | Composite FK to same-revision country |
| `provenance_id` | `uuid` | No | Composite FK to same-revision provenance |
| `scheme_code` | `varchar(64)` | No | Uppercase public code |
| `display_name` | `varchar(160)` | No | Technical scheme name |
| `normalization` | `identifier_normalization` | No | Exact digits or locale-neutral uppercase |
| `validation_pattern` | `varchar(200)` | No | Anchored approved v1 pattern |
| `character_set` | `varchar(100)` | No | Consumer-visible character-set rule |
| `case_sensitive` | `boolean` | No | Sensitivity after normalization |
| `uniqueness_scope` | `varchar(40)` | No | V1 check `COUNTRY_AND_SCHEME` |
| `status` | `geographic_record_status` | No | Only active schemes are advertised |

Natural unique key: `(catalog_revision_id, country_id, scheme_code)`. The consistency key
`(catalog_revision_id, country_id, identifier_scheme_id, scheme_code)` supports identifier FKs.

### 5.5 `administrative_divisions`

Stable identity fields, `division_type_code`, `hierarchy_level`, parent identity/code fields, and
the generated required parent level intentionally propagate immutable key facts. Composite FKs
use those facts to enforce historical non-reuse and hierarchy rules without a trigger.

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `division_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to revision |
| `country_id` | `uuid` | No | Composite FK to same-revision country |
| `country_identity_id` | `uuid` | No | Propagated stable country identity |
| `country_identity_key` | `varchar(100)` | No | Propagated stable country lineage key |
| `division_identity_id` | `uuid` | No | Stable division identity |
| `division_identity_key` | `varchar(180)` | No | Propagated stable division lineage key |
| `division_type_id` | `uuid` | No | Composite FK to type, including code and level |
| `division_type_code` | `varchar(64)` | No | Propagated type code |
| `hierarchy_level` | `smallint` | No | Propagated type level |
| `parent_division_id` | `uuid` | Yes | Null only for level-one roots |
| `parent_division_identity_id` | `uuid` | Yes | Stable parent identity; null for roots |
| `parent_division_identity_key` | `varchar(180)` | Yes | Stable parent lineage key; null for roots |
| `parent_canonical_code` | `varchar(64)` | Yes | Null with parent ID; otherwise propagated parent code |
| `required_parent_level` | `smallint` | Yes | Stored generated `hierarchy_level - 1` for non-roots |
| `canonical_code` | `varchar(64)` | No | Unchanged country-scoped INEC DPA digit string |
| `default_name` | `varchar(200)` | No | NFC source name |
| `official_name` | `varchar(300)` | No | Same source name in initial catalog |
| `status` | `geographic_record_status` | No | Lifecycle status |
| `valid_from` | `date` | Yes | Inclusive |
| `valid_until` | `date` | Yes | Exclusive |
| `valid_during` | `daterange` | No | Stored generated range |
| `provenance_id` | `uuid` | No | Composite FK to same-revision provenance |

Natural unique key: `(catalog_revision_id, country_id, canonical_code)`. The composite
consistency key
`(catalog_revision_id, country_id, division_id, country_identity_id, division_identity_id,
division_identity_key, hierarchy_level, canonical_code)` is the parent and identifier target. A
second consistency key `(catalog_revision_id, country_id, division_id)` supports name and
direct-owner FKs.

The country FK includes
`(catalog_revision_id, country_id, country_identity_id, country_identity_key)`. The division
identity triple references `division_identities`, and
`(country_identity_id, canonical_code, division_identity_id, division_identity_key)` references
`division_code_registry`; these FKs prevent a later snapshot from assigning the code to another
logical division.
The type FK is
`(catalog_revision_id, country_id, division_type_id, division_type_code, hierarchy_level)`.
The parent FK is
`(catalog_revision_id, country_id, parent_division_id, country_identity_id,
parent_division_identity_id, parent_division_identity_key, required_parent_level,
parent_canonical_code)` to the corresponding division consistency key. Root/non-root checks
require all parent fields to be null at level 1 and all to be non-null at levels 2 and 3.
Self-parenting by either snapshot or stable identity is rejected. The child code must start with
the complete parent canonical code.

### 5.6 `administrative_division_identifiers`

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `division_identifier_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to revision |
| `country_id` | `uuid` | No | Same country as division and scheme |
| `country_identity_id` | `uuid` | No | Propagated stable country identity |
| `division_id` | `uuid` | No | Composite FK to division |
| `division_identity_id` | `uuid` | No | Propagated stable division identity |
| `division_identity_key` | `varchar(180)` | No | Propagated stable division lineage key |
| `division_hierarchy_level` | `smallint` | No | Propagated division level |
| `division_canonical_code` | `varchar(64)` | No | Propagated division canonical code |
| `identifier_scheme_id` | `uuid` | No | Composite FK to persisted scheme |
| `scheme_code` | `varchar(64)` | No | Propagated scheme code |
| `identifier_value` | `varchar(128)` | No | Canonical normalized value |
| `is_primary` | `boolean` | No | Primary within division and scheme at a date |
| `status` | `geographic_identifier_status` | No | Identifier lifecycle |
| `valid_from` | `date` | Yes | Inclusive |
| `valid_until` | `date` | Yes | Exclusive |
| `valid_during` | `daterange` | No | Stored generated range |
| `provenance_id` | `uuid` | No | Composite FK to same-revision provenance |

Natural unique key:
`(catalog_revision_id, country_id, scheme_code, identifier_value)`. This revision-wide key
prohibits duplicate temporal rows. The division FK includes revision, snapshot owner, stable
country/division identity, level, and canonical code as
`(catalog_revision_id, country_id, division_id, country_identity_id, division_identity_id,
division_identity_key, division_hierarchy_level, division_canonical_code)`. The registry FK
`(country_identity_id, scheme_code, identifier_value, division_identity_id,
division_identity_key)` targets `division_identifier_registry`, which prohibits cross-revision
reuse. The scheme FK
`(catalog_revision_id, country_id, identifier_scheme_id, scheme_code)` still binds the identifier
to the same revision and snapshot country.

### 5.7 `administrative_division_names`

| Field | PostgreSQL type | Nullable | Rule |
|---|---|---:|---|
| `division_name_id` | `uuid` | No | Primary key |
| `catalog_revision_id` | `uuid` | No | FK to revision |
| `country_id` | `uuid` | No | Same country as division |
| `division_id` | `uuid` | No | Composite FK to same-revision division |
| `provenance_id` | `uuid` | No | Composite FK to same-revision provenance |
| `language_tag` | `varchar(35)` | No | Canonical BCP 47 |
| `name_type` | `geographic_name_type` | No | Public name type |
| `name` | `varchar(300)` | No | Original NFC display value |
| `is_preferred` | `boolean` | No | Preference within division and language |
| `valid_from` | `date` | Yes | Inclusive |
| `valid_until` | `date` | Yes | Exclusive |
| `valid_during` | `daterange` | No | Stored generated range |
| `name_sort_key` | `text` | No | Stored generated NFC Unicode default case-folding key |

Natural unique key:
`(catalog_revision_id, country_id, division_id, language_tag, name_type, name)`.

## 6. Deterministic Relational Projection Digest

The relational projection digest is independent of the RFC 8785 derived-manifest digest. The
manifest digest identifies the approved logical source artifact and supplies the public revision;
the projection digest proves that loaded relational mappings equal the logical projection of that
manifest. It deliberately ignores physical row identity and migration execution metadata.

### 6.1 Canonical Byte Encoding

The fixed projection schema and algorithm version is the ASCII string
`geographic-reference-relational-projection/v2`. A different section, field, scalar, identity-key,
or sorting rule requires a new version string and an approved migration.

The hash input uses these byte primitives:

- `U16(n)`, `U64(n)`: unsigned 16-bit or 64-bit integer in network byte order;
- `NULL`: one byte `0x00`;
- `TEXT(s)`: one byte `0x01`, then `U64` of the UTF-8 byte length, then exactly those UTF-8
  bytes;
- `ROW(values)`: byte `0x52`, `U16` field count, then each field encoded as `NULL` or `TEXT` in
  the declared field order; and
- `SECTION(name, rows)`: byte `0x53`, `TEXT(name)`, `U64` row count, then the ordered encoded
  rows.

The complete stream is `TEXT(algorithmVersion)`, `U16(21)`, then the 21 sections below in the
listed order. Explicit null markers and byte lengths make null, empty string, field boundaries,
row boundaries, and section boundaries unambiguous.

Every non-null SQL value is converted to canonical text before `TEXT` encoding:

- strings and enums use their exact persisted, Unicode-NFC value;
- dates use `YYYY-MM-DD`;
- booleans use lowercase `true` or `false`;
- integers use base-10 ASCII without a sign, leading zero, or grouping;
- SHA-256 uses 64 lowercase hexadecimal characters without a prefix.

No internal UUID, execution timestamp, `recorded_by`, validation or activation principal,
catalog `schema_version`, `migration_version`, activation migration version, Flyway value,
generated range, generated parent level, generated name sort key, database OID, tuple location,
sequence value, or physical order enters the stream. Rows sort by the listed tuple using numeric
comparison for integers and
UTF-8 byte comparison after NFC for all text. `nameTypeRank` is `OFFICIAL=1`, `COMMON=2`,
`SHORT=3`, `ALTERNATIVE=4`, and `HISTORICAL=5`; `preferredRank` is 0 for true and 1 for false;
`codeKindRank` is `ALPHA2=1`, `ALPHA3=2`, and `NUMERIC=3`. A duplicate complete section sort key
is a validation failure.

Initial logical identity keys are derived directly from the approved manifest:
`COUNTRY/<alpha2Code>` and `DIVISION/<countryAlpha2>/<canonicalCode>`. A later approved manifest
schema that changes a code must carry forward the established lineage key from the retained
registry mapping; it cannot derive a new key for the existing entity. These keys are internal
lineage values, not public identifiers.

### 6.2 Version 2 Sections, Fields, and Sort Keys

| Order and section | Exact field order | Exact row sort key |
|---|---|---|
| 1 `country_identities` | `country_identity_key` | `country_identity_key` |
| 2 `country_code_registry` | `country_identity_key`, `code_kind`, `code_value` | `(codeKindRank, code_value)` |
| 3 `division_identities` | `country_identity_key`, `division_identity_key` | `(country_identity_key, division_identity_key)` |
| 4 `division_code_registry` | `country_identity_key`, `canonical_code`, `division_identity_key` | `(country_identity_key, canonical_code)` |
| 5 `division_identifier_registry` | `country_identity_key`, `scheme_code`, `identifier_value`, `division_identity_key` | `(country_identity_key, scheme_code, identifier_value)` |
| 6 `catalog_sources` | `source_code`, `dataset_name`, `authority`, `publisher`, `source_revision`, `source_uri`, `release_date`, `effective_date`, `retrieved_on`, `license_expression`, `attribution`, `authority_note`, `license_evidence_reference`, `legal_approval_reference` | `source_code` |
| 7 `catalog_source_artifacts` | `source_code`, `artifact_code`, `artifact_reference`, `sha256` | `(source_code, artifact_code)` |
| 8 `catalog_provenance` | `source_code`, `artifact_code`, `source_record_reference`, `mapping_rule_code`, `source_value_sha256` | all fields in field order, with null last |
| 9 `catalog_coverages` | `coverage_code`, `coverage_kind`, `country_alpha2`, `source_code`, `historical_coverage_start`, `source_effective_date`, `description` | `coverage_code` |
| 10 `catalog_coverage_counts` | `coverage_code`, `count_code`, `expected_count` | `(coverage_code, count_code)` |
| 11 `catalog_coverage_languages` | `coverage_code`, `language_tag` | `(coverage_code, language_tag)` |
| 12 `catalog_coverage_division_types` | `coverage_code`, `country_alpha2`, `division_type_code`, `hierarchy_level` | `(coverage_code, hierarchy_level, division_type_code)` |
| 13 `catalog_coverage_identifier_schemes` | `coverage_code`, `country_alpha2`, `scheme_code` | `(coverage_code, scheme_code)` |
| 14 `catalog_coverage_exclusions` | `coverage_code`, `exclusion_code`, `excluded_count`, `description` | `(coverage_code, exclusion_code)` |
| 15 `countries` | `country_identity_key`, `alpha2_code`, `alpha3_code`, `numeric_code`, `default_name`, `official_name`, `is_independent`, `status`, `valid_from`, `valid_until`, `provenance_source_code`, `provenance_artifact_code`, `source_record_reference`, `mapping_rule_code`, `source_value_sha256` | `alpha2_code` |
| 16 `country_names` | `country_identity_key`, `language_tag`, `name_type`, `name`, `is_preferred`, `valid_from`, `valid_until`, `provenance_source_code`, `provenance_artifact_code`, `source_record_reference`, `mapping_rule_code`, `source_value_sha256` | `(country_identity_key, language_tag, nameTypeRank, preferredRank, name)` |
| 17 `administrative_division_types` | `country_identity_key`, `code`, `name`, `hierarchy_level`, `status`, `provenance_source_code`, `provenance_artifact_code`, `source_record_reference`, `mapping_rule_code`, `source_value_sha256` | `(country_identity_key, hierarchy_level, code)` |
| 18 `identifier_schemes` | `country_identity_key`, `scheme_code`, `display_name`, `normalization`, `validation_pattern`, `character_set`, `case_sensitive`, `uniqueness_scope`, `status`, `provenance_source_code`, `provenance_artifact_code`, `source_record_reference`, `mapping_rule_code`, `source_value_sha256` | `(country_identity_key, scheme_code)` |
| 19 `administrative_divisions` | `country_identity_key`, `division_identity_key`, `country_alpha2`, `canonical_code`, `division_type_code`, `hierarchy_level`, `parent_division_identity_key`, `parent_canonical_code`, `default_name`, `official_name`, `status`, `valid_from`, `valid_until`, `provenance_source_code`, `provenance_artifact_code`, `source_record_reference`, `mapping_rule_code`, `source_value_sha256` | `(country_identity_key, canonical_code)` |
| 20 `administrative_division_identifiers` | `country_identity_key`, `division_identity_key`, `division_canonical_code`, `scheme_code`, `identifier_value`, `is_primary`, `status`, `valid_from`, `valid_until`, `provenance_source_code`, `provenance_artifact_code`, `source_record_reference`, `mapping_rule_code`, `source_value_sha256` | `(country_identity_key, scheme_code, identifier_value)` |
| 21 `administrative_division_names` | `country_identity_key`, `division_identity_key`, `language_tag`, `name_type`, `name`, `is_preferred`, `valid_from`, `valid_until`, `provenance_source_code`, `provenance_artifact_code`, `source_record_reference`, `mapping_rule_code`, `source_value_sha256` | `(country_identity_key, division_identity_key, language_tag, nameTypeRank, preferredRank, name)` |

Identity and registry sections contain only identities and mappings referenced by the candidate
manifest; older unreferenced rows remain protected by historical FKs but do not alter a candidate
projection digest.
The public `revision_code`, `derived_manifest_sha256`, and
`catalog_revisions.projection_sha256` are excluded: the first two are independently validated
manifest identity, and the last stores this digest's result. Validation rows, validation checks,
the active pointer, and all generated or audit fields are excluded. Source
`release_date`, `effective_date`, and `retrieved_on` are approved manifest facts, not execution
timestamps, and remain included.

### 6.3 SQL and Independent Reproduction

Before immutable migration approval, the independent validator reads the approved RFC 8785
manifest directly, derives the initial logical identity keys and registry mappings, maps manifest
field names to the 21 sections, and produces the framed bytes without consulting database UUIDs
or migration SQL. Its concrete expected digest is embedded in the generated revision row.

During the grouped production migration, V003 executes one explicitly columned logical
projection per section against its uncommitted staged revision. SQL joins physical FKs to stable
identity keys, source codes, artifact codes, coverage codes, owner codes, and provenance values;
no selected digest column is a UUID or execution value. It uses the exact field aliases and
`ORDER BY` lists above. SQL session date style is fixed to ISO YMD, all text is verified NFC, and
sorting uses deterministic `ucs_basic` semantics. Neither implementation may use `SELECT *`,
implicit locale-dependent casts, heap order, JSON object order, or a locale-dependent collation.

The SQL verifier builds the byte stream with explicit null tags, `octet_length`, UTF-8
conversion, network-order integer framing, and ordered byte aggregation, then applies PostgreSQL
18's SHA-256 function. The manifest validator applies the same framing to manifest-derived
logical values. Both results must match the concrete `catalog_revisions.projection_sha256`; a
production mismatch rolls back before validation or pointer activation. Repeating either
computation over the same approved logical values produces identical bytes regardless of UUID
assignment, insertion, heap, index, query-plan, migration version, principal, timestamp, or
execution order.

## 7. Temporal and Preferred-Name Constraints

Each temporal snapshot table generates its range from source boundaries rather than accepting
an independently writable range:

```sql
valid_during daterange GENERATED ALWAYS AS
  (daterange(valid_from, valid_until, '[)')) STORED
CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until > valid_from)
```

The database enables the trusted supplied `btree_gist` extension. Preferred country names use
this partial exclusion rule:

```sql
EXCLUDE USING gist
  (catalog_revision_id WITH =, country_id WITH =,
   language_tag WITH =, valid_during WITH &&)
WHERE (is_preferred)
```

Division names use the same rule with `(catalog_revision_id, country_id, division_id,
language_tag, valid_during)`. Consequently, adjacent periods such as `[2026-01-01, 2027-01-01)`
and `[2027-01-01, infinity)` do not overlap and are valid.

Primary identifiers use the analogous owner/scheme exclusion key
`(catalog_revision_id, country_id, division_id, identifier_scheme_id, valid_during)` with the
predicate `is_primary`. This prevents two simultaneous primary values while the natural unique
identifier key prevents duplicate snapshot rows and the global registry prevents historical
reassignment.

## 8. Declarative Hierarchy and Identifier Rules

### 8.1 Division Types and Codes

| Public type code | OpenAPI `name` | Level | Canonical DPA form | Initial count |
|---|---|---:|---|---:|
| `PROVINCE` | `Province` | 1 | `PP`, exactly two digits | 24 |
| `CANTON` | `Canton` | 2 | `PPCC`, exactly four digits | 222 |
| `CANTONAL_SEAT_AREA` | `Cantonal seat area` | 3 | `PPCC50`, exactly six digits with suffix `50` | 222 |
| `RURAL_PARISH` | `Rural parish` | 3 | `PPCCSS`, exactly six digits with suffix from `51` through `99` | 825 |

The division check couples `division_type_code`, `hierarchy_level`, canonical-code length, and
the level-three suffix. A canton's parent code equals its first two digits; a level-three
record's parent code equals its first four digits. The composite parent FK enforces the same
revision, country, preceding level, and actual parent code. These checks make a self-reference,
cross-country edge, skipped level, orphan, level-two or level-three root, and cycle
unrepresentable without a trigger.

### 8.2 Identifier Schemes

| Scheme | `display_name` | Normalization and persisted pattern | Character set and case | Scope and relation |
|---|---|---|---|---|
| `EC_INEC_DPA` | `INEC DPA` | `EXACT`; `^(?:[0-9]{2}\|[0-9]{4}\|[0-9]{6})$` | `ASCII_DIGITS`; not case-sensitive | Ecuador; one per included division; value must equal that division's canonical code |
| `ISO_3166_2` | `ISO 3166-2` | Locale-neutral `UPPERCASE`; `^EC-[A-Z]{1,2}$` | `ASCII_UPPERCASE_LETTERS_AND_HYPHEN`; not case-sensitive | Ecuador; provinces only; one per included province |

Scheme codes are normalized to uppercase before lookup. The identifier table check allows only
the two persisted v1 scheme definitions, applies the appropriate value pattern, requires
`division_hierarchy_level = 1` for `ISO_3166_2`, and requires DPA value/canonical-code equality.
Both definitions use `COUNTRY_AND_SCHEME` uniqueness scope and `ACTIVE` status. The escaped
vertical bars in the Markdown table are literal regex alternation characters in the persisted
pattern. Activation also verifies complete cardinality. An unsupported syntactically valid
scheme is distinguished from a malformed value in the query outcome; no arbitrary scheme is
queried.

## 9. Deterministic Source Mapping

### 9.1 Countries

For every object in `iso-codes` 4.20.1 `data/iso_3166-1.json`:

1. Copy `alpha_2`, `alpha_3`, and `numeric` unchanged. They must already satisfy their uppercase
   or digit patterns. Do not parse the numeric value as a number.
2. Normalize source display strings to Unicode NFC only.
3. Map source `name` to `countries.default_name` and to one preferred `en` `SHORT` name.
4. If `official_name` is present, map it to `countries.official_name` and one non-preferred
   `en` `OFFICIAL` name. If absent, set `official_name = default_name` but do not create a
   duplicate `OFFICIAL` name row.
5. If `common_name` is present, create one non-preferred `en` `COMMON` row.
6. Create no `ALTERNATIVE`, `HISTORICAL`, Spanish, inferred, translated, or corrected name.
7. Leave `is_independent` null. The field is not projected publicly.

This produces exactly 249 countries and 433 names: 249 preferred `SHORT`, 173 non-preferred
`OFFICIAL`, and 11 non-preferred `COMMON` records. Ecuador maps deterministically to `EC`,
`ECU`, and `218`.

For the initial revision, each country receives logical identity key `COUNTRY/<alpha2>` and
exactly three registry mappings, one for each code kind. The country snapshot references that
stable identity and all three mappings. A later migration must reuse the registry-resolved
identity key for every previously seen code; it cannot mint a new identity for an existing code.

Country resolution first classifies the normalized input as exactly two alphabetic characters,
three alphabetic characters, or three digits, then uses only the corresponding unique key.
Alphabetic input is uppercased with locale-neutral rules. The alpha-2 result is always the
canonical country reference.

### 9.2 Ecuador Divisions

Use the authoritative `PARROQUIAS` sheet and detailed classifier when the compact `CODIGOS`
sheet is incomplete, including `160167 SHUAR PASTAZA`. Preserve code strings and NFC-normalize
only the source name. Every included division contributes the same source name to
`default_name`, `official_name`, and one preferred `es` `OFFICIAL` name. No other division name
row is loaded.

Parentage is derived only from code prefixes: four-digit canton to its first two digits and
six-digit area to its first four digits. Source records are not silently supplemented or
corrected.

For the initial revision, each division receives logical identity key
`DIVISION/EC/<canonicalCode>`, one canonical-code registry mapping, and one identifier-registry
mapping for every loaded identifier. The division and identifier snapshots reference those full
mappings. A future approved code addition may point to an existing stable identity, but a code or
identifier already present in either registry can never point elsewhere.

### 9.3 Presentation-Name Selection and Name Listing

For a visible owner, presentation selection is deterministic:

1. If `language` is omitted, use `default_name`.
2. Otherwise select the preferred, visible name for the exact canonical language tag.
3. If absent and the request is regional, select the preferred, visible primary-language tag.
4. If still absent, use `default_name`.

Name-list `language` and `nameType` parameters are literal record filters and never invoke
fallback. Name rows sort by language tag, name-type order `OFFICIAL`, `COMMON`, `SHORT`,
`ALTERNATIVE`, `HISTORICAL`, preferred first, `name_sort_key` under code-point collation, then
original NFC `name` under the same collation. The natural key prevents a remaining tie.

## 10. Exact Initial Coverage and Exclusions

| Metric | Required value |
|---|---:|
| Countries and ISO-recognized territories | 249 |
| Stable country identities | 249 |
| Country-code registry mappings | 747 |
| English country names | 433 |
| Preferred English `SHORT` country names | 249 |
| English `OFFICIAL` country names | 173 |
| English `COMMON` country names | 11 |
| Ecuador provinces | 24 |
| Ecuador cantons | 222 |
| Ecuador level-three areas | 1,047 |
| Cantonal-seat areas within level three | 222 |
| Rural parishes within level three | 825 |
| Total Ecuador divisions | 1,293 |
| Stable Ecuador division identities | 1,293 |
| Ecuador canonical-code registry mappings | 1,293 |
| Preferred Spanish `OFFICIAL` division names | 1,293 |
| `EC_INEC_DPA` identifiers | 1,293 |
| `ISO_3166_2` province identifiers | 24 |
| Total division identifiers | 1,317 |
| External division-identifier registry mappings | 1,317 |
| Supported identifier schemes | 2 |

Stable identity and registry counts are internal migration assertions; they do not add public
coverage metrics or API fields.

Country coverage is global ISO-aligned coverage with English source names and a 2026-01-01
historical start. Division coverage is Ecuador-only with Spanish source names, source effective
date 2025-12-31, and API historical start 2026-01-01. All records in the approved initial
current snapshot that have lifecycle state are `ACTIVE`; no draft, deprecated, or retired row is
added outside a separately approved historical fixture or future revision.

The coverage metadata contains exactly these five exclusion categories:

| Coverage | Exclusion code | Count | Meaning |
|---|---|---:|---|
| `EC_ADMINISTRATIVE_DIVISIONS` | `EC_NESTED_URBAN_PARISHES` | 269 | Named urban parishes below 59 cantonal-seat areas require a fourth hierarchy level or another approved model. |
| `EC_ADMINISTRATIVE_DIVISIONS` | `EC_STUDY_ZONE_NON_ADMINISTRATIVE_RECORDS` | null | Province code `90`, `140190 SINAI-CUCHAENTZA`, and every other study-zone or non-administrative record are excluded. |
| `EC_ADMINISTRATIVE_DIVISIONS` | `EC_UNSUPPLIED_DIVISION_NAMES` | null | Alternative, translated, historical, and second preferred division names not supplied by INEC are excluded. |
| `ISO_COUNTRIES` | `COUNTRY_INDEPENDENCE_UNAVAILABLE` | 249 | The pinned country source establishes no independence indicator. V1 stores null and exposes no `isIndependent` field. |
| `ISO_COUNTRIES` | `COUNTRY_UNAPPROVED_LOCALIZED_NAMES` | null | Spanish, alternative, and historical country names have no approved pinned v1 source and are not invented or imported. |

## 11. Relationships

| Parent | Child | Cardinality and rule |
|---|---|---|
| Stable country identity | Country-code registry rows, country snapshots, division identities | One to many; immutable logical lineage across revisions |
| Country-code registry mapping | Country snapshots | One to many across revisions; a kind/value maps to exactly one stable country identity |
| Stable division identity | Canonical-code registry rows, external-identifier registry rows, division snapshots | One to many across revisions within one stable country identity |
| Division-code registry mapping | Division snapshots | One to many across revisions; country/canonical code maps to one stable division identity |
| Division-identifier registry mapping | Division-identifier snapshots | One to many across revisions; country/scheme/value maps to one stable division identity |
| Catalog revision | Sources, artifacts, provenance, coverages, validations, all snapshot rows | One to many; every child remains in one revision |
| Catalog source | Source artifacts and provenance | One to many within the same revision |
| Source artifact | Provenance | One to many; every row reference points to a hashed artifact |
| Catalog revision | Active pointer | Zero or one referenced revision; pointer table has exactly one row after initial activation |
| Validation | Active pointer | Zero or one; only a same-revision `PASS` validation is eligible |
| Country | Country names | One to many |
| Country | Division types, identifier schemes, and divisions | One to many; v1 division children exist only for Ecuador |
| Division type | Divisions | One to many; type code and hierarchy level propagate through the FK |
| Division | Child divisions | Zero to many; each non-root has exactly one preceding-level parent with the propagated stable parent identity |
| Division | Division names | One to many |
| Division | Division identifiers | One to many |
| Identifier scheme | Division identifiers | One to many within the same revision and country |
| Coverage | Counts, languages, types, schemes, exclusions | One to many metadata declarations |

## 12. Lifecycle, Temporal State, and Coverage Rules

The effective query date is the current UTC calendar date when `asOf` is absent and the parsed
ISO calendar date when it is present. A syntactically valid explicit `asOf`, whether past,
present, or future, uses ordinary interval, lifecycle, dependency, and coverage semantics. A
future date is not invalid merely because it is later than the current date; no visible match
produces the ordinary item-not-found or empty-collection outcome.

| Query mode | Record lifecycle | Temporal condition |
|---|---|---|
| Current | `status = ACTIVE` | `valid_during @> effective_utc_date` |
| Explicit `asOf` | `status IN (ACTIVE, DEPRECATED, RETIRED)` | `valid_during @> asOf` |
| Any external query | Never `DRAFT` | Must also be within declared endpoint coverage |

Names use their own `valid_during` and inherit owner visibility. Current identifier resolution
requires an `ACTIVE` identifier; explicit-`asOf` resolution permits all three identifier statuses.
Division types have no validity range in v1. A current type list contains only active types; a
division visible for explicit `asOf` may use an active, deprecated, or retired non-draft type as
its descriptor.

The endpoint coverage start is the latest `historical_coverage_start` among every dataset used
by its response. Therefore all country and Ecuador division queries reject dates before
2026-01-01. In particular, `asOf=2025-12-31` is outside every Ecuador division capability even
though it is the INEC source effective date.

Codes and identifiers are not reassigned after retirement. The explicit query date
selects the one visible record in the active snapshot; the API does not select an archived
catalog revision. Global registry primary keys permit only one stable identity per country code,
country-scoped canonical division code, or country/scheme identifier. Every relevant snapshot
references the complete registry mapping, so retained historical FKs also prevent changing or
deleting that mapping. V003 verifies candidate lineage and reports a clear migration failure, but
the PostgreSQL PK, unique, check, and FK constraints are the final concurrent integrity boundary.

## 13. Runtime Active-Only Views

Internal tables are owned in a non-runtime schema. `geographic_api` contains only reviewed
views that join `catalog_active_revision` to the selected revision:

- `active_catalog_revision`
- `active_catalog_metadata`
- `active_countries`
- `active_country_names`
- `active_administrative_division_types`
- `active_identifier_schemes`
- `active_administrative_divisions`
- `active_administrative_division_names`
- `active_administrative_division_identifiers`
- `active_provenance`

"Active-only" means active catalog revision, not only `ACTIVE` lifecycle status. The views
exclude `DRAFT` records and owners, but retain active, deprecated, and retired rows so fixed
prepared statements can apply current or explicit-`asOf` visibility. They do not embed the current
date and do not consume or store the configured expected revision. Every database-backed
statement receives the configured expected revision as a bind value, compares it with the
revision exposed by the active view, and returns a revision-mismatch outcome rather than reading
a different revision.

The views expose internal IDs only as private join keys to fixed repository SQL. REST and
application read models do not expose them. Flyway history, staged rows, validation principals,
stable identity keys, registry tables, and internal ownership remain outside the view schema.
The active snapshot has already been constrained against those registries, so route queries keep
their existing public-code predicates and at-most-one-statement shape; no runtime registry lookup
or additional statement is required. Provenance views join the row's selected artifact so
`sourceDigest` is deterministic without exposing the registry or artifact UUID.

## 14. Public Read Models

These are projections, not persistence entities.

| Read model | Public content |
|---|---|
| `CatalogMetadata` | Catalog revision, migration revision, source authority and revisions, source release/effective dates when supplied, source archive and selected-artifact digests, country and division coverage starts, languages, expected counts, included type codes and levels, supported identifier schemes, and exclusions. Values come from the active revision; initial hashes, dates, counts, and exclusions are activation assertions/examples rather than a contract freeze on later revisions within the fixed v1 type and identifier contract. |
| `Country` | Alpha-2 canonical reference, alpha-3 and numeric codes, selected name, lifecycle, validity, approved provenance, and catalog revision; no independence field |
| `CountryName` | Language, name type, original NFC value, preferred flag, validity, and approved provenance |
| `AdministrativeDivisionType` | Public type code, `name` technical display field, hierarchy level, lifecycle descriptor, country alpha-2, provenance, and catalog revision |
| `AdministrativeDivision` | Country alpha-2, canonical DPA code, type code and level, optional parent canonical reference, selected name, lifecycle, validity, ordered approved identifiers, approved provenance, and catalog revision |
| `DivisionIdentifier` | Scheme code, normalized value, primary flag, lifecycle, validity, and approved provenance; never an internal ID |
| `DivisionName` | Language, name type, original NFC value, preferred flag, validity, and approved provenance |
| `Provenance` | Source authority, dataset, source revision, nullable source `effectiveDate`, source-record reference, and `sourceDigest` equal to `sha256:` plus the selected `catalog_source_artifacts.sha256` for that row |
| `PageResult<T>` | Ordered items, one-based effective page, effective page size, `hasNext`, and catalog revision; no total-count query |
| `Ancestor` | Division projection ordered from immediate parent to root, bounded to at most two rows |

Name records have neither a lifecycle field nor a per-item catalog revision. Owner visibility
and the name validity period govern visibility, and the page envelope carries the catalog
revision. Public provenance always includes the source-record reference and represents the
source effective date as nullable `effectiveDate`, omitted or null when the source supplies none;
it does not substitute a release date or coverage start. It does not expose migration
credentials, filesystem locations, database object names, or principals. `sourceDigest` always
identifies the exact selected artifact reached by the row's provenance FK, such as
`ISO_3166_1_JSON`, `ISO_3166_2_JSON`, or `DETAILED_CLASSIFIER`. A source-archive digest remains
catalog metadata and is not substituted for the selected row artifact digest.

## 15. Indexes Tied to Routes

Indexes are on internal base tables; views rely on PostgreSQL predicate pushdown. Unique keys
listed above also provide their corresponding indexes.

| Base-table index | Route or purpose |
|---|---|
| Unique `country_identity_key` and country-code registry PK `(code_kind, code_value)` | Declarative cross-revision country-code lineage and migration FK checks; not a runtime route lookup |
| Unique `division_identity_key`, division-code registry PK `(country_identity_id, canonical_code)`, and identifier-registry PK `(country_identity_id, scheme_code, identifier_value)` | Declarative cross-revision division/identifier lineage and migration FK checks; not runtime route lookups |
| Singleton PK and unique active revision on `catalog_active_revision` | Every database-backed catalog query and readiness, startup, or info revision observation |
| Country unique keys for alpha-2, alpha-3, and numeric code | `/v1/countries/{countryCode}` and country-context resolution for all division routes |
| `(catalog_revision_id, status, alpha2_code)` on countries | `/v1/countries` stable page order |
| `(catalog_revision_id, country_id, language_tag, name_type, is_preferred DESC, name_sort_key COLLATE "ucs_basic", name COLLATE "ucs_basic")` on country names | Country name page, literal filters, and presentation fallback |
| `(catalog_revision_id, country_id, status, hierarchy_level, code)` on division types | `/division-types` order |
| Division unique canonical-code key | Division item, names, children, and ancestors owner resolution |
| `(catalog_revision_id, country_id, parent_division_id, status, canonical_code)` on divisions | Root list with null parent and direct-child list with one parent |
| `(catalog_revision_id, country_id, division_id, language_tag, name_type, is_preferred DESC, name_sort_key COLLATE "ucs_basic", name COLLATE "ucs_basic")` on division names | Division name page and presentation fallback |
| Identifier unique `(catalog_revision_id, country_id, scheme_code, identifier_value)` | `/division-identifiers/{schemeCode}/{identifierValue}` |
| `(catalog_revision_id, country_id, division_id, scheme_code, identifier_value)` on identifiers | Ordered identifier aggregation in a division representation |
| Division composite identity/level key | Bounded ancestor recursive joins and declarative parent FK |
| GiST preferred-name exclusions | Exact and primary-language fallback correctness across time |
| `(catalog_revision_id, coverage_code)` and coverage child PKs | `/v1/catalog` aggregation and coverage gate |
| `(catalog_revision_id, provenance_id)` and source/artifact keys | Provenance projection without N+1 reads |

The fixed catalog size does not justify text-search, full-text, geospatial, closure-table,
materialized-path, or speculative name-only indexes. Representative PostgreSQL 18 plans must
confirm these indexes before promotion.

## 16. Statement-Bounded Query Shapes

Each database-backed catalog query and each database-dependent readiness, startup, or info
observation uses at most one prepared SQL statement and therefore one PostgreSQL MVCC snapshot.
Responses completed before database access use zero statements. Liveness and metrics use zero
database statements. No approved capability requires a multi-query read-only transaction.

Every database-backed statement reads an active-only view and compares its revision with the
configured expected revision supplied as a bind value. A catalog statement also loads applicable
coverage and emits one outcome discriminator. Temporal division statements preserve the required
outcome order: country identity absent from the activated non-draft snapshot, division coverage
unavailable, remaining input invalid or unsupported, country not visible on the valid effective
date, then requested resource absent. Division types use the same order without `asOf` and apply
current visibility after coverage and their query inputs. This permits the HTTP layer to preserve
error precedence without an earlier database query.

| Capability | At-most-one-statement shape when database access is required |
|---|---|
| Catalog metadata | Active revision joined to sources, coverage children, and passing validation; ordered JSON aggregates build one object |
| Country list | Visible countries ordered by alpha-2, lateral selected-name lookup, `LIMIT pageSize + 1`, and offset from one-based page |
| Country resolution | Normalize and classify one raw code, select through only its matching unique key, then lateral selected name and provenance |
| Country names | Resolve visible country in the same statement, apply literal language/type filters, stable name order, and `LIMIT pageSize + 1` |
| Division types | Resolve country and coverage outcome first, then active types ordered by level and code with bounded pagination |
| Root divisions | Resolve Ecuador coverage, select visible divisions with null parent and visible type/country, lateral selected name and ordered identifiers, then bounded page |
| Division resolution | Resolve country and coverage, select by country/canonical unique key, require visible ancestor chain, and aggregate name, identifiers, and provenance |
| Identifier resolution | Resolve country, coverage, and persisted scheme; validate scheme-specific raw value in the outcome CTE; select visible identifier and its visible division |
| Division names | Resolve visible division and ancestor chain, apply literal filters, stable order, and bounded page |
| Direct children | Resolve visible parent, select only visible rows whose parent is that ID and level is parent level plus one, then bounded page |
| Ancestors | Resolve visible division, recurse only while `depth < 2`, retain PostgreSQL `CYCLE` defense, and order depth 1 then 2 |

Collection statements fetch at most 101 rows for the maximum page size of 100 and remove the
extra row after deriving `hasNext`. They do not execute a total-count query. Ordered JSON
aggregation and lateral subqueries prevent N+1 statements. A login-specific statement timeout
configured at five seconds provides a final query bound and is
mapped separately from dependency unavailability.

## 17. Activation State Transition and Recovery

Revision state is derived from evidence and the pointer rather than maintained as mutable flags
on every row:

| Derived state | Definition | Runtime visibility |
|---|---|---|
| `STAGED` | Revision and snapshot rows exist in the uncommitted migration transaction without a passing validation | None |
| `VALIDATED` | A matching validation and all checks are `PASS`, but the pointer has not switched | None |
| `ACTIVE` | The singleton pointer references the passing validation and revision | Visible only after commit |
| `SUPERSEDED` | A previously validated revision is no longer referenced | None; retained as immutable internal evidence |

The controlled deployment verifies source and artifact bytes against the pinned hashes before
image assembly; the database never fetches network data. V002 inserts stable identities and
registry mappings before the snapshot rows that reference them; any historical remap fails at
the declarative constraint boundary. V003 verifies approved hash constants and retained evidence,
checks constraints and exact counts, recomputes the logical projection digest, records all
passing validation checks, and switches the singleton pointer as its final statement. A
transaction commit makes the complete revision and pointer visible together. Concurrent runtime
statements see either the old or new pointer for their entire MVCC snapshot, never both.

If any load, constraint, digest, count, approval, validation, or pointer statement fails, the
transaction rolls back. A previous active pointer remains unchanged. During the initial release,
failure leaves no active pointer and readiness fails safely. The runtime never attempts repair
or activation.

Before production migration, operators establish and test the approved provider snapshot, PITR
point, or PostgreSQL 18 restore point. Recovery uses that approved recovery point or a new
reviewed forward migration; it does not edit an applied migration, run automated Flyway
`repair`, delete evidence, or manually alter the pointer. After recovery, startup/readiness
again verifies schema compatibility and exact expected revision before traffic promotion.

## 18. Runtime Grant Boundary

Initial bootstrap and recurring migration privileges are deliberately different:

1. Before the initial grouped migration, the platform pre-provisions only the target database,
   the Flyway history schema, and the secret-managed `geographic_migrator` login. That login has
   the database/history-schema privileges needed by Flyway and temporary `CREATEROLE`. The
   platform does not pre-create `geographic_owner`, `geographic_runtime`, or a runtime login.
2. PostgreSQL 18 automatically gives a `CREATEROLE` creator an `ADMIN OPTION` membership in each
   role it creates. The bootstrap-superuser-granted row has `admin_option=true`,
   `inherit_option=false`, and `set_option=false`; the non-superuser creator cannot remove or
   change it. It conveys role-administration authority and must not be mistaken for harmless
   metadata.
3. Initial Flyway `V001` creates `geographic_owner` and `geographic_runtime` as `NOLOGIN` roles,
   accepting that both temporary automatic admin memberships remain until privileged
   finalization. Using that temporary authority, V001 adds a separate non-admin owner membership
   with `inherit_option=false` and `set_option=true`; it adds no SET or INHERIT membership for
   `geographic_runtime`. The migrator can therefore `SET ROLE geographic_owner` for object work
   during the initial migration. The temporary admin rows are not an accepted recurring state.
4. V001 creates all initial catalog and API objects under `geographic_owner`, revokes defaults,
   and establishes every initial object grant. It contains no login role and no password or
   credential value.
5. In the same transactionally grouped V001-V003 run, V002 loads the pinned source evidence,
   stable identities, registry mappings, and revision-scoped candidate snapshot. V003 verifies
   the approved concrete derived-manifest digest, recomputes the logical relational-projection
   digest, validates exact counts, constraints and legal evidence, and then switches the active
   pointer as its final statement.
6. Only after grouped V001-V003 succeeds does a privileged, dedicated finalization one-shot run.
   In one database transaction it removes all temporary creator grants, creates the
   secret-managed runtime login with `INHERIT` and without `CREATEROLE`, installs the final exact
   memberships, transfers required ownership, minimizes recurring migrator privileges, applies
   runtime-login defaults, validates the complete final state, and commits. A failed assertion or
   injected failure rolls the entire finalization back; rerunning after rollback is safe. The
   runtime unit requires successful completion of this one-shot and cannot start after a partial
   or failed finalization.

Finalization is mandatory and performs all of these operations before runtime startup:

- revoke every bootstrap and migrator-granted creator membership, then install exactly one
  migrator-to-owner membership with `admin_option=false`, `inherit_option=false`, and
  `set_option=true`; no migrator-to-runtime membership may remain;
- grant the concrete runtime login membership only in `geographic_runtime` with
  `admin_option=false`, `inherit_option=true`, and `set_option=false`; inherited read privileges
  are available without allowing role administration or `SET ROLE`, and the login receives no
  direct object grant;
- revoke `CREATEROLE` from `geographic_migrator` and verify `rolcreaterole=false`;
- transfer target database ownership to `geographic_owner` if the bootstrap principal or
  migrator owns it, and retain owner ownership for application schemas, the Flyway history
  schema/table, and application objects;
- retain for `geographic_migrator` only database `CONNECT`, `USAGE` and `CREATE` on the exact
  Flyway history schema, `SELECT`, `INSERT`, `UPDATE`, and `DELETE` on its exact
  `flyway_schema_history` table, and the owner membership needed for `SET ROLE`; and
- verify the migrator has no `geographic_runtime` membership, no direct runtime-view grant, and
  no direct application-schema privileges outside the owner role.

The finalization transaction takes a deployment advisory lock and is safely rerunnable only from
either the fully pre-finalization state or the already validated final state. Any other observed
membership, owner, role attribute, or direct grant aborts for operator investigation rather than
guessing at repair. Failure recovery is transaction rollback followed by a corrected rerun; no
runtime unit or traffic promotion is allowed in between. The `grantor` recorded in
`pg_auth_members` is audit metadata and does not itself confer grant authority; effective
role-grant authority is determined by `admin_option` together with PostgreSQL's `CREATEROLE` and
superuser rules. Passwords and password hashes remain in secret-management/bootstrap operations
and never appear in immutable migrations. Future Flyway runs use `geographic_migrator` without
`CREATEROLE`, bracket owner-level work with `SET ROLE geographic_owner` and `RESET ROLE`, and
write Flyway history as the migration login. Every future migration explicitly grants any new
approved active view; no default future grant exposes it automatically.

### Runtime Login Session Defaults

Role settings attached to a group role are not inherited merely because a login is a member of
that role. After creating the concrete secret-managed runtime login, finalization sets these
database-specific defaults on that login itself:

- `default_transaction_read_only=on`;
- `statement_timeout=5s`; and
- `search_path=geographic_api,pg_catalog`.

The runtime login is the only credential received by the application. The read-only default is
an overridable defense and diagnostic safeguard, not the privilege boundary; explicit object
grants remain authoritative if a session changes it. The five-second timeout bounds actual
runtime statements and maps to the catalog-timeout problem. The fixed search path prevents
implicit resolution through writable or unreviewed schemas.

Revoke database and schema defaults from `PUBLIC`. Runtime receives only database `CONNECT`,
`USAGE` on `geographic_api`, and explicit `SELECT` on the approved active-only views. It receives
no internal-schema or registry-table access, table access, sequences, Flyway history,
temporary-object privilege, grant option, object ownership, or privileged-role membership.
`INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, `REFERENCES`, `TRIGGER`, DDL, ownership changes, and
privilege changes must all fail. `EXECUTE` is revoked on routines in application-owned schemas
and on every mutation-capable routine; it is not revoked indiscriminately from PostgreSQL
built-ins needed to evaluate approved `SELECT` statements. Runtime contains neither migration
credentials nor Flyway or JDBC access.

### Role and Session Tests

PostgreSQL 18 integration and deployment tests query `pg_auth_members` and prove:

- before privileged finalization, PostgreSQL's unchangeable bootstrap-granted automatic admin
  rows exist for both V001-created roles, the additional owner SET path works, and runtime cannot
  start;
- the owner/migrator row has `admin_option=false`, `inherit_option=false`, and `set_option=true`;
- no runtime/migrator membership row exists and no migrator membership has `admin_option=true`;
- the runtime-role/runtime-login row has `admin_option=false`, `inherit_option=true`, and
  `set_option=false`, while the concrete login has `rolinherit=true` and
  `rolcreaterole=false`;
- `geographic_migrator.rolcreaterole=false`, the database owner is `geographic_owner`, and the
  migrator cannot grant either service role;
- the migrator retains only the documented database, history-schema/table, and owner-SET access;
  and the runtime login has only the documented runtime access.

Finalization tests inject failure after each role, ownership, privilege, login-default, and
assertion stage and prove complete transaction rollback. They then rerun successfully, rerun once
more against the accepted final state, and prove the runtime systemd unit cannot start unless the
finalization one-shot has exited successfully. A concurrency test starts two finalizers on the
same database and lock key, proves the second blocks behind the first, observes exactly one state
transition, and then validates the committed final state successfully.

Authenticated runtime-login tests verify `SHOW` results for all three login-specific defaults,
prove a statement exceeding five seconds is cancelled by `statement_timeout`, and repeat the
full positive/negative privilege matrix after explicitly overriding the read-only default. They
also prove that settings placed only on `geographic_runtime` do not replace the required concrete
login settings.

## 19. Corrections to `docs/database/v1-schema.dbml`

The DBML is a design input, not the executable or normative schema. This Phase 1 model makes the
following corrections:

1. Add stable country/division identities, global country-code/division-code/external-identifier
   registries, catalog revision, source, artifact, row-provenance, coverage, validation, scheme,
   and singleton active-pointer entities; scope every snapshot row to a revision and stable
   lineage.
2. Keep snapshot natural keys revision-scoped while global registry PKs and snapshot mapping FKs
   prevent historical reassignment across every retained revision.
3. Change ISO code storage from padded `CHAR` to constrained `varchar`; numeric code remains
   three-character text.
4. Make country independence nullable and null for v1; omit it from public models rather than
   defaulting it to true.
5. Change every temporal end check from `>=` to strict `>` and add generated half-open
   `daterange` columns.
6. Replace the insufficient "one active preferred name" partial unique indexes with temporal
   `btree_gist` exclusion constraints, allowing adjacent historical preferred names.
7. Remove uniqueness on `(country_id, hierarchy_level)`. Two approved public types share level
   three. Use country-and-code uniqueness and the four exact public codes.
8. Replace the proposed deferred hierarchy trigger with composite revision/country/type/level
   and parent-level FKs plus root, prefix, and code checks. The decreasing level proves acyclicity
   declaratively.
9. Add persisted identifier-scheme definitions and scheme-specific normalization, syntax,
   scope, lifecycle, and provenance. Do not treat free-form scheme codes as supported.
10. Require `official_name` for initial divisions because the approved mapping supplies it, and
    constrain DPA code length and suffix by type.
11. Add direct provenance to countries, types, schemes, divisions, names, and identifiers rather
    than repeating loosely related source strings on selected tables.
12. Treat snapshot rows as immutable migration products. Mutable audit/version fields are not
    optimistic concurrency controls and are not exposed if retained physically.
13. Add active-revision views and exact runtime grants so staged revisions and internal tables
    are not readable by the application.
14. Tie indexes to the approved list, resolution, name, hierarchy, metadata, and provenance
    routes; remove speculative default-name and unrestricted name-search indexes.
15. Define the projection digest over manifest-derived logical keys and values only; exclude all
    UUID, migration, principal, timestamp, generated-storage, and physical-identity values.

## 20. Validation Matrix

Activation and migration tests must prove every row below on PostgreSQL 18.

| Validation area | Required assertion | Failure effect |
|---|---|---|
| Derived-manifest digest | RFC 8785 JCS over the exact fixed-root, NFC, explicit-null, sorted derived manifest reproduces the concrete approval-artifact digest; public revision is `sha256:` plus that digest | Roll back; no pointer switch |
| Relational-projection digest | The manifest-only validator and SQL reproduce identical fixed version-2 framed bytes and SHA-256 over all 21 logical sections; public manifest identity, UUIDs, catalog `schema_version`, migration versions, principals, timestamps, generated storage values, and physical order have no effect | Roll back; no pointer switch |
| Country source | Debian archive hash is `5d551f3ddb32548c4321e9011720fd97751af0107592f79ebffc939bd32f2268` | Roll back |
| Country source date | Debian release date and country historical-coverage start are 2026-01-01; no separate country effective date is inferred | Roll back |
| Country artifacts | ISO 3166-1 hash is `f01b812b57fba9f31ff621bf33e7c7570a01964dbeb5be2167e94decf538c89f`; ISO 3166-2 hash is `78c90ef7fc25b5c2631aac5f089bc9ff6ec22c025c05b6ddbc087a1f1be2e46a` | Roll back |
| INEC source | Archive hash is `9a2962bcccd88745dba4d61627e27945714049d97571d088e6c6b294be668a2c` | Roll back |
| INEC artifacts | Detailed hash is `b6648d27906b12a12f310b30ed7a94c9efcdd0249eaa073647013f8b7029ba5d`; compact hash is `0d0ae33a0a0023ed44abcaa6b38a1c173fbda423e368a8f12bedda59d65d8f3a` | Roll back |
| Legal gate | An authorized release reviewer verifies the governed INEC approval record's use/scope, decision, approver, and date; automation proves only that the approved reference is bound to the manifest/migration evidence; Debian notices/license evidence and attribution also exist | Block activation and promotion; no source substitution |
| Country identity cardinality | Exactly 249 stable country identities and 747 global code mappings back the 249 countries; each snapshot's three code FKs resolve to one identity | Roll back or constraint rejection |
| Country mapping | Exactly 433 `en` names: 249 preferred `SHORT`, 173 `OFFICIAL`, 11 `COMMON`; all values follow source fallback rules | Roll back |
| Ecuador mapping | Ecuador resolves identically as `EC`, `ECU`, and `218` | Roll back |
| Type configuration | Exactly `PROVINCE/Province:1`, `CANTON/Canton:2`, `CANTONAL_SEAT_AREA/Cantonal seat area:3`, and `RURAL_PARISH/Rural parish:3`; no level uniqueness assumption | Roll back |
| Division identity cardinality | Exactly 1,293 stable division identities and 1,293 canonical-code registry mappings back 24 provinces, 222 cantons, 222 cantonal-seat areas, and 825 rural parishes | Roll back or constraint rejection |
| Division names | Exactly 1,293 preferred `es` `OFFICIAL` names and no other division-name row | Roll back |
| Initial lifecycle | Every initial country, type, scheme, division, and identifier is `ACTIVE`; no unapproved draft, deprecated, or retired row exists | Roll back |
| DPA identifiers | Exactly 1,293 `EC_INEC_DPA` identifiers and registry mappings; each equals its division canonical code, matches level length, and maps to that stable division identity | Roll back or constraint rejection |
| ISO identifiers | Exactly 24 uppercase valid `ISO_3166_2` identifiers and registry mappings, each attached permanently to one stable province identity | Roll back or constraint rejection |
| Hierarchy | 24 roots, every canton has its prefix province, every level-three row has its prefix canton, no orphan, cross-country edge, skipped level, self-parent, or cycle | Roll back |
| Level-three suffix | Exactly 222 included suffix-`50` areas and 825 included suffix-`51` through `99` rural parishes | Roll back |
| Exclusions | Exactly the five approved exclusion categories are declared, including unavailable country independence; no province `90`, `140190 SINAI-CUCHAENTZA`, study-zone record, or any of the 269 fourth-level urban parishes is loaded | Roll back |
| Unicode and language | Source display values are NFC with no unapproved transformation; `en` and `es` tags are canonical | Roll back |
| Temporal intervals | Every generated range equals its boundaries; equal or reversed finite boundaries are rejected; null boundaries remain open | Constraint rejection |
| Preferred names | Overlapping preferred periods for one owner/language are rejected; adjacent periods are accepted | Exclusion-constraint rejection |
| Identifier history | Duplicate country/scheme/value and overlapping primary owner/scheme periods are rejected | Constraint rejection |
| Cross-revision non-reuse | Conflicting inserts, updates, and deletes against each global registry fail under PK/unique/FK constraints while retained old snapshots exist; a retired or absent key cannot be assigned to a different stable identity | Constraint rejection before activation |
| Provenance | Every snapshot row reaches one same-revision source object and selected pinned artifact; public `sourceDigest` is that artifact SHA-256, while archive SHA-256 remains metadata | Roll back or FK rejection |
| Coverage | Country and division starts are both 2026-01-01; INEC source effective date remains 2025-12-31; all counts, schemes, levels, languages, and exclusions are declared | Roll back |
| Public names and provenance | Country/division name items contain no lifecycle or per-item revision; their envelope carries revision; provenance includes source-record reference and nullable source effective date | Contract/query test failure; no promotion |
| Lifecycle visibility | Current fixtures expose only active/date-valid dependencies; every explicit `asOf`, including a valid future date, applies non-draft interval/lifecycle/dependency semantics; names and identifiers inherit visibility | Query test failure; no promotion |
| Active pointer | Pointer accepts only a same-revision `PASS` validation and changes only after every check passes | FK/check rejection or rollback |
| Snapshot consistency | A concurrent pointer switch yields wholly old or wholly new revision data in one statement, never mixed rows | Integration test failure; no promotion |
| Statement bounds | Each database-backed catalog query and DB-dependent readiness/startup/info observation executes at most one statement; pre-database outcomes, liveness, and metrics execute zero; pages fetch at most 101 rows and ancestor recursion stops at depth two | Integration/plan test failure; no promotion |
| Query plans | Representative route plans remain bounded, expose the intended unique, parent, name, identifier, and metadata indexes, and contain no N+1 pattern; a sequential scan is acceptable when PostgreSQL 18 cost evidence favors it for the fixed small dataset | No promotion until reviewed |
| Active views | Runtime views expose only pointer-selected, non-draft rows and no registry, stable key, staged revision, or Flyway history; views contain no configured expected revision and each DB-backed statement compares its bind value | Privilege/view test failure |
| Role bootstrap | V001 creates both NOLOGIN roles; PostgreSQL's temporary automatic creator ADMIN rows remain until privileged finalization; V001 adds only the owner SET path needed for object work and contains no login/password | Migration test failure; no promotion |
| Role finalization | `pg_auth_members` options match the exact migrator/owner and runtime-login/runtime rows; no migrator/runtime row or migrator ADMIN option exists; migrator has `rolcreaterole=false` and cannot grant service roles | Deployment test failure; no promotion |
| Finalization atomicity and order | Failure injection after every finalization stage rolls back all role/ownership/grant/default changes; a safe rerun succeeds; simultaneous invocations serialize on one advisory-lock key and produce one transition; runtime requires the successful finalization one-shot | Deployment test failure; runtime cannot start |
| Ownership and migrator access | Database, application schemas/objects, and Flyway history schema/table are owned by `geographic_owner`; migrator retains only CONNECT, exact history-schema USAGE/CREATE, history-table SELECT/INSERT/UPDATE/DELETE, and owner SET membership | Deployment test failure; no promotion |
| Runtime session defaults | The concrete runtime login reports read-only default on, five-second statement timeout, and `geographic_api,pg_catalog` search path; group-role settings alone do not satisfy the test | Deployment test failure; no promotion |
| Runtime privileges | Approved `SELECT` and required PostgreSQL built-ins succeed; a statement over five seconds times out; DML, `TRUNCATE`, DDL, ownership, grants, registry/internal access, mutation-capable routine execution, and privilege escalation fail even after overriding the read-only default | No promotion |
| Atomic failure | Injected failure before the final switch leaves the prior pointer and catalog readable; initial failure leaves readiness down | Recovery test failure; no promotion |
| Recovery | Approved restore point/PITR or forward migration restores the expected validated revision without runtime writes, manual pointer edits, or Flyway `repair` | No traffic promotion |

This matrix is both activation evidence and the minimum database migration acceptance boundary.
