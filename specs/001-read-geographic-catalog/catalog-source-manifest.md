# Initial Catalog Source Manifest

**Feature**: [Read Geographic Catalog API](spec.md)
**Pinned**: 2026-07-25
**Status**: Approved for specification and planning; production use remains subject to the
legal gates below.

## Country Catalog

- **Dataset**: `iso-codes` 4.20.1, ISO 3166 data files
- **Publisher**: Debian `iso-codes` project
- **Standard basis**: ISO 3166-1 and ISO 3166-2
- **Release date**: 2026-01-01
- **Source archive**:
  `https://deb.debian.org/debian/pool/main/i/iso-codes/iso-codes_4.20.1.orig.tar.xz`
- **Source archive SHA-256**:
  `5d551f3ddb32548c4321e9011720fd97751af0107592f79ebffc939bd32f2268`
- **ISO 3166-1 artifact**: `data/iso_3166-1.json`
- **ISO 3166-1 artifact SHA-256**:
  `f01b812b57fba9f31ff621bf33e7c7570a01964dbeb5be2167e94decf538c89f`
- **ISO 3166-2 artifact**: `data/iso_3166-2.json`
- **ISO 3166-2 artifact SHA-256**:
  `78c90ef7fc25b5c2631aac5f089bc9ff6ec22c025c05b6ddbc087a1f1be2e46a`
- **License**: LGPL-2.1-or-later; retain required notices and source attribution.
- **Authority note**: This is an ISO-aligned open dataset, not an official ISO publication.
  ISO Online Browsing Platform data is used for manual verification only and MUST NOT be
  systematically extracted or redistributed.
- **Expected country records**: 249 officially assigned country and territory codes.
- **Country name coverage**: English only. The pinned archive contains no approved Spanish
  translation artifact, so v1 MUST NOT invent or import Spanish country names from another
  source.
- **Country field mapping**:
  - `alpha_2`, `alpha_3`, and `numeric` map unchanged to their corresponding public ISO codes.
  - `name` maps to `default_name` and one preferred `en` name of type `SHORT`.
  - `official_name`, when present, maps to `official_name` and one non-preferred `en` name of
    type `OFFICIAL`; otherwise `official_name` falls back to `name` without creating a second
    name record.
  - `common_name`, when present, maps to one non-preferred `en` name of type `COMMON`.
  - No `ALTERNATIVE` or `HISTORICAL` country name is loaded in v1.
- **Expected country name records**: 433 total: 249 preferred `SHORT`, 173 `OFFICIAL`, and
  11 `COMMON` records.
- **Independence coverage**: The pinned source does not establish whether each entry is an
  independent state. V1 omits `isIndependent` from public representations and stores no
  inferred independence value; the schema design MUST allow the value to remain unknown.
- **Ecuador country codes**: `EC`, `ECU`, and `218`.
- **Ecuador ISO 3166-2 coverage**: 24 province identifiers.
- **Historical coverage start**: 2026-01-01. Dates before this boundary are outside the
  declared country snapshot coverage; the source does not establish earlier validity.

## Ecuador Administrative Catalog

- **Dataset**: Clasificador Geografico Estadistico 2026
- **Authority and publisher**: Instituto Nacional de Estadistica y Censos del Ecuador (INEC)
- **Effective date**: 2025-12-31
- **Retrieved**: 2026-07-25
- **Source archive**:
  `https://www.ecuadorencifras.gob.ec/documentos/web-inec/Cartografia/Clasificador_Geografico/CLASIFICADOR_GEOGRAFICO_2026.zip`
- **Source archive SHA-256**:
  `9a2962bcccd88745dba4d61627e27945714049d97571d088e6c6b294be668a2c`
- **Detailed classifier artifact SHA-256**:
  `b6648d27906b12a12f310b30ed7a94c9efcdd0249eaa073647013f8b7029ba5d`
- **Compact workbook artifact SHA-256**:
  `0d0ae33a0a0023ed44abcaa6b38a1c173fbda423e368a8f12bedda59d65d8f3a`
- **Division name mapping**: Each of the 1,293 included records contributes exactly one source
  name. After Unicode NFC normalization, that value maps to `default_name`, `official_name`,
  and one preferred `es` name of type `OFFICIAL`.
- **Expected division name records**: 1,293. V1 loads no `COMMON`, `SHORT`, `ALTERNATIVE`,
  `HISTORICAL`, translated, or second preferred division-name record.
- **API historical coverage start**: 2026-01-01. Division responses also depend on the Ecuador
  country record, so the effective query boundary is the later country-catalog boundary.
  `asOf=2025-12-31` and all earlier dates return `AS_OF_OUTSIDE_CATALOG_COVERAGE` for every
  Ecuador division capability.

### Included Records

- 24 official provinces with two-digit `PP` DPA codes.
- 222 official cantons with four-digit `PPCC` DPA codes.
- 1,047 primary six-digit `PPCCSS` level-three records:
  - 222 cantonal-seat areas whose suffix is `50`.
  - 825 rural parishes whose suffix is between `51` and `99`.
- Total administrative division records: 1,293.
- 1,293 `EC_INEC_DPA` identifiers, one for every included division.
- 24 `ISO_3166_2` identifiers, one for every included province, verified against the pinned
  `data/iso_3166-2.json` artifact.

### Excluded Records

The exact consumer-visible exclusion categories are defined below. In source-record terms, the
Ecuador exclusions include 269 named urban parishes nested below 59 cantonal-seat areas;
province code `90`; `140190 SINAI-CUCHAENTZA`; every other study-zone or non-administrative
record; and every alternative, translated, historical, or second preferred division name not
supplied by the pinned source.

### Extraction Rules

- The authoritative `PARROQUIAS` sheet and detailed classifier take precedence when the
  compact `CODIGOS` sheet is incomplete. This includes valid record `160167 SHUAR PASTAZA`.
- Codes are text and preserve leading zeroes.
- Province and canton relationships are derived from the code prefix. A six-digit record's
  canton is its first four digits, and a canton's province is its first two digits.
- Source records are not silently corrected, renamed, or supplemented. Any discrepancy is
  recorded in validation evidence and resolved through a new approved manifest and migration.

## Coverage Exclusion Categories

The initial catalog has exactly these five exclusion categories. `excludedCount` is null when
the excluded item class is unavailable rather than a finite set of rejected source rows.

| Coverage | Exclusion code | Excluded count | Normative description |
|---|---|---:|---|
| `EC_ADMINISTRATIVE_DIVISIONS` | `EC_NESTED_URBAN_PARISHES` | 269 | Named urban parishes below 59 cantonal-seat areas require a fourth hierarchy level or another approved model. |
| `EC_ADMINISTRATIVE_DIVISIONS` | `EC_STUDY_ZONE_NON_ADMINISTRATIVE_RECORDS` | null | Province code `90`, `140190 SINAI-CUCHAENTZA`, and every other study-zone or non-administrative record are excluded. |
| `EC_ADMINISTRATIVE_DIVISIONS` | `EC_UNSUPPLIED_DIVISION_NAMES` | null | Alternative, translated, historical, and second preferred division names not supplied by INEC are excluded. |
| `ISO_COUNTRIES` | `COUNTRY_INDEPENDENCE_UNAVAILABLE` | 249 | The pinned country source establishes no independence indicator. V1 stores null and exposes no `isIndependent` field. |
| `ISO_COUNTRIES` | `COUNTRY_UNAPPROVED_LOCALIZED_NAMES` | null | Spanish, alternative, and historical country names have no approved pinned v1 source and are not invented or imported. |

## Legal Gates

- This planning manifest may retain source titles, URLs, cryptographic hashes, aggregate counts,
  classification rules, and the minimal anomaly/exclusion references required to make scope and
  validation unambiguous. Those facts are validation evidence, not a redistributable catalog.
  The legal gate below prohibits committing the bulk extracted row set, generated catalog SQL,
  source-derived response samples, or a distributable migration/runtime artifact before
  approval; it does not erase the evidence needed to review this plan.
- The catalog owner MUST verify all hashes after retrieval and retain the source artifacts,
  attribution, license evidence, extraction report, and derived-record manifest.
- Debian `iso-codes` notices and LGPL obligations MUST be satisfied before distribution.
- INEC's published policy permits public communication and attributed derivative works but
  restricts transfer or commercialization without authorization. The responsible legal or
  data-governance owner MUST approve the intended service use in writing before INEC-derived
  data is committed to a distributable artifact, deployed, or promoted to production.
- This is a manual governance gate, not a cryptographic assertion made true by a non-empty
  configuration value. The governed approval record MUST identify the approved use and scope,
  decision, approver, and decision date. An authorized release reviewer verifies that record;
  automation checks only that its approved reference is retained and bound to the derived
  manifest and migration evidence.
- Failure to obtain or record required approval blocks catalog activation and traffic
  promotion; it does not authorize replacing the source silently.

## Derived Manifest Format

The approved extraction produces one JSON artifact named
`catalog-derived-manifest-v1.json`. Its hash is independent of JSON writer behavior and source
file iteration order.

### Canonicalization Rules

1. The root object contains exactly these members, with no additional member:
   `schemaVersion`, `catalogScope`, `sources`, `coverages`, `exclusions`, `countries`,
   `countryNames`, `divisionTypes`, `divisions`, `divisionNames`, `identifierSchemes`, and
   `divisionIdentifiers`.
2. `schemaVersion` is the fixed string
   `geographic-reference-derived-manifest/v1`; `catalogScope` is the fixed string
   `ISO_3166_1_4.20.1_EC_INEC_2026_V1`.
3. Every object contains every field declared below. Nullable values are represented by the JSON
   literal `null`; fields are never omitted. Unknown fields cause validation failure.
4. Before comparison or serialization, every JSON member name and string value is normalized to
   Unicode NFC. Display names receive no other transformation. Technical code normalization is
   limited to the approved uppercase or exact rules stated in this manifest.
5. Dates are strings in strict `YYYY-MM-DD` form. SHA-256 values are 64 lowercase hexadecimal
   characters without a `sha256:` prefix. Hierarchy levels and counts are JSON integers.
   Preferred, primary, and case-sensitivity values are JSON booleans. No floating-point number
   is permitted.
6. Strings are compared for array sorting by Unicode scalar-value sequence after NFC, with no
   locale, collation, case folding, or natural-number interpretation. Tuple keys are compared
   left to right; integers compare numerically; `true` sorts before `false` only where a key
   explicitly uses preferred-first order. Duplicate complete sort keys are invalid.
7. Every array is sorted by the exact key below before serialization. RFC 8785 JSON
   Canonicalization Scheme (JCS) then determines object-member ordering, escaping, primitive
   serialization, and the final whitespace-free representation.
8. The hash input is exactly the RFC 8785 canonical JSON encoded as UTF-8, with no BOM and no
   trailing newline. The derived-manifest digest is lowercase hexadecimal SHA-256 of those
   bytes. The public catalog revision is the exact string `sha256:` followed by that digest.

### Shared Provenance Object

Every country, name, type, division, scheme, and identifier record contains `provenance` with
exactly these fields:

| Field | Type | Rule |
|---|---|---|
| `sourceCode` | string | References one `sources.sourceCode` |
| `artifactCode` | string | References one artifact under that source |
| `sourceRecordReference` | string | Stable JSON object, workbook sheet/one-based row, or manifest-rule reference; never a local filesystem path |
| `mappingRuleCode` | string | `ISO_3166_1_COUNTRY_V1`, `ISO_3166_1_NAME_V1`, `EC_DIVISION_TYPE_V1`, `EC_INEC_DIVISION_V1`, `EC_INEC_DIVISION_NAME_V1`, `EC_IDENTIFIER_SCHEME_V1`, `EC_INEC_DPA_IDENTIFIER_V1`, or `EC_ISO_3166_2_IDENTIFIER_V1` as applicable |
| `sourceValueSha256` | null | Fixed to explicit null in v1; artifact hashes and the stable record reference are the source evidence |

ISO JSON references use `<artifact-reference>#alpha_2=<alpha2>` for country objects and
`<artifact-reference>#code=<subdivision-code>` for ISO 3166-2 objects. Workbook references use
`<artifact-code>#sheet=<RFC3986-percent-encoded-sheet-name>&row=<one-based-row-number>`. A
manifest-defined type or scheme uses
`catalog-source-manifest.md#<public-code>`. These forms are ASCII and are generated identically
by extraction and independent validation.

Provenance source/artifact selection is exact:

| Record | `sourceCode` | `artifactCode` |
|---|---|---|
| Country or country name | `DEBIAN_ISO_CODES_4_20_1` | `ISO_3166_1_JSON` |
| Division type, division, division name, or `EC_INEC_DPA` identifier | `EC_INEC_CLASSIFIER_2026` | `DETAILED_CLASSIFIER` |
| `EC_INEC_DPA` scheme definition | `EC_INEC_CLASSIFIER_2026` | `DETAILED_CLASSIFIER` |
| `ISO_3166_2` scheme definition or identifier | `DEBIAN_ISO_CODES_4_20_1` | `ISO_3166_2_JSON` |

### Root Record Categories

The ten record categories below are root arrays. Field lists are exact. Fields shown as nullable
are always present with either their value or JSON `null`.

| Category | Exact fields | Array sort key |
|---|---|---|
| `countries` | `alpha2Code`, `alpha3Code`, `numericCode`, `defaultName`, `officialName`, `isIndependent` (null in v1), `status`, `validFrom` (nullable), `validUntil` (nullable), `provenance` | `alpha2Code` |
| `countryNames` | `countryAlpha2`, `languageTag`, `nameType`, `name`, `preferred`, `validFrom` (nullable), `validUntil` (nullable), `provenance` | `(countryAlpha2, languageTag, nameTypeRank, preferredRank, name)` |
| `divisionTypes` | `countryAlpha2`, `code`, `name`, `hierarchyLevel`, `status`, `provenance` | `(countryAlpha2, hierarchyLevel, code)` |
| `divisions` | `countryAlpha2`, `canonicalCode`, `typeCode`, `parentCanonicalCode` (nullable), `defaultName`, `officialName`, `status`, `validFrom` (nullable), `validUntil` (nullable), `provenance` | `(countryAlpha2, canonicalCode)` |
| `divisionNames` | `countryAlpha2`, `divisionCanonicalCode`, `languageTag`, `nameType`, `name`, `preferred`, `validFrom` (nullable), `validUntil` (nullable), `provenance` | `(countryAlpha2, divisionCanonicalCode, languageTag, nameTypeRank, preferredRank, name)` |
| `identifierSchemes` | `countryAlpha2`, `schemeCode`, `name`, `normalization`, `validationPattern`, `characterSet`, `caseSensitive`, `uniquenessScope`, `status`, `provenance` | `(countryAlpha2, schemeCode)` |
| `divisionIdentifiers` | `countryAlpha2`, `divisionCanonicalCode`, `schemeCode`, `value`, `primary`, `status`, `validFrom` (nullable), `validUntil` (nullable), `provenance` | `(countryAlpha2, schemeCode, value)` |
| `coverages` | `coverageCode`, `kind`, `countryAlpha2` (nullable), `sourceCode`, `historicalCoverageStart`, `sourceEffectiveDate` (nullable), `description`, `counts`, `languages`, `includedDivisionTypes`, `identifierSchemes` | `coverageCode` |
| `sources` | `sourceCode`, `datasetName`, `authority`, `publisher`, `sourceRevision`, `sourceUri`, `releaseDate` (nullable), `effectiveDate` (nullable), `retrievedOn` (nullable), `licenseExpression`, `attribution`, `authorityNote` (nullable), `licenseEvidenceReference`, `legalApprovalReference` (nullable), `artifacts` | `sourceCode` |
| `exclusions` | `coverageCode`, `exclusionCode`, `excludedCount` (nullable), `description` | `(coverageCode, exclusionCode)` |

`nameTypeRank` is `OFFICIAL=1`, `COMMON=2`, `SHORT=3`, `ALTERNATIVE=4`, and
`HISTORICAL=5`. `preferredRank` is `0` for `true` and `1` for `false`.

Nested arrays have these exact orders:

- each source `artifacts` array contains objects with exactly `artifactCode`,
  `artifactReference`, and `sha256`, sorted by `artifactCode`;
- coverage `counts` contains objects with exactly `countCode` and `expectedCount`, sorted by
  `countCode`;
- coverage `languages` is sorted by language tag;
- coverage `includedDivisionTypes` contains exactly `code` and `hierarchyLevel`, sorted by
  `(hierarchyLevel, code)`; and
- coverage `identifierSchemes` is sorted by scheme code.

The source codes are fixed as `DEBIAN_ISO_CODES_4_20_1` and
`EC_INEC_CLASSIFIER_2026`. Their artifact codes are respectively
`SOURCE_ARCHIVE`, `ISO_3166_1_JSON`, `ISO_3166_2_JSON` and
`SOURCE_ARCHIVE`, `DETAILED_CLASSIFIER`, `COMPACT_WORKBOOK`. Artifact references and hashes are
the pinned URI/path and digest values in this document; the two INEC inner artifact references
are their exact archive-member references recorded by the approved extraction report.
`licenseEvidenceReference` is concrete for both sources. The production INEC source record also
has a concrete `legalApprovalReference`; null is permitted only in a planning artifact that is
ineligible for digest approval, migration generation, activation, or promotion.

The two coverage records have these fixed values:

| `coverageCode` | `kind` | `countryAlpha2` | `sourceCode` | `historicalCoverageStart` | `sourceEffectiveDate` | `description` |
|---|---|---|---|---|---|---|
| `EC_ADMINISTRATIVE_DIVISIONS` | `ADMINISTRATIVE_DIVISION` | `EC` | `EC_INEC_CLASSIFIER_2026` | `2026-01-01` | `2025-12-31` | `Ecuador provinces, cantons, and primary level-three areas in the approved three-level v1 hierarchy.` |
| `ISO_COUNTRIES` | `COUNTRY` | null | `DEBIAN_ISO_CODES_4_20_1` | `2026-01-01` | null | `249 officially assigned ISO 3166-1 country and territory codes with approved English names.` |

`ISO_COUNTRIES.languages` is exactly `['en']`; both included-type and identifier-scheme arrays
are empty. `EC_ADMINISTRATIVE_DIVISIONS.languages` is exactly `['es']`; its included types are
the four fixed type records in hierarchy/code order and its schemes are exactly
`['EC_INEC_DPA', 'ISO_3166_2']` in scalar-value order. The single quotes here are documentation
notation; the artifact is JSON and uses JSON strings.

Coverage count codes and values are exact:

| Coverage | `countCode` | `expectedCount` |
|---|---|---:|
| `EC_ADMINISTRATIVE_DIVISIONS` | `CANTONAL_SEAT_AREAS` | 222 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `CANTONS` | 222 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `DIVISIONS` | 1293 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `DIVISION_IDENTIFIERS` | 1317 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `DIVISION_NAMES` | 1293 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `EC_INEC_DPA_IDENTIFIERS` | 1293 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `IDENTIFIER_SCHEMES` | 2 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `ISO_3166_2_IDENTIFIERS` | 24 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `LEVEL_THREE_AREAS` | 1047 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `PROVINCES` | 24 |
| `EC_ADMINISTRATIVE_DIVISIONS` | `RURAL_PARISHES` | 825 |
| `ISO_COUNTRIES` | `COUNTRIES` | 249 |
| `ISO_COUNTRIES` | `COUNTRY_COMMON_NAMES` | 11 |
| `ISO_COUNTRIES` | `COUNTRY_NAMES` | 433 |
| `ISO_COUNTRIES` | `COUNTRY_OFFICIAL_NAMES` | 173 |
| `ISO_COUNTRIES` | `COUNTRY_SHORT_NAMES` | 249 |

### Fixed V1 Record Values

All initial nullable record validity fields are explicit null because the sources provide no
per-record boundary; coverage starts prevent unsupported historical inference. Every record with
a lifecycle field is `ACTIVE`.

The four division-type records use the exact `(code, name, hierarchyLevel)` values
`(PROVINCE, Province, 1)`, `(CANTON, Canton, 2)`,
`(CANTONAL_SEAT_AREA, Cantonal seat area, 3)`, and
`(RURAL_PARISH, Rural parish, 3)`. `name` is the OpenAPI public technical display field.

The two scheme records use these exact definitions:

| Scheme | `name` | `normalization` | `validationPattern` | `characterSet` | `caseSensitive` | `uniquenessScope` |
|---|---|---|---|---|---:|---|
| `EC_INEC_DPA` | `INEC DPA` | `EXACT` | `^(?:[0-9]{2}\|[0-9]{4}\|[0-9]{6})$` | `ASCII_DIGITS` | false | `COUNTRY_AND_SCHEME` |
| `ISO_3166_2` | `ISO 3166-2` | `UPPERCASE` | `^EC-[A-Z]{1,2}$` | `ASCII_UPPERCASE_LETTERS_AND_HYPHEN` | false | `COUNTRY_AND_SCHEME` |

The escaped vertical bars shown in the Markdown table are literal regex alternation characters
in JSON. Each included division has one primary `EC_INEC_DPA` identifier equal to its canonical
code. Each province additionally has one primary uppercase `ISO_3166_2` identifier.

Coverage `counts` contains the exact metrics and values stated in this document: 249 countries;
433 country names split into 249 `SHORT`, 173 `OFFICIAL`, and 11 `COMMON`; 24 provinces; 222
cantons; 1,047 level-three areas split into 222 cantonal-seat areas and 825 rural parishes;
1,293 divisions; 1,293 division names; 1,293 DPA identifiers; 24 ISO 3166-2 identifiers; and
1,317 identifiers in total. The `exclusions` array contains exactly the five rows in Coverage
Exclusion Categories with descriptions copied byte-for-byte after NFC.

## Derived Manifest Digest Approval Artifact

The extractor and an independent validator each perform the canonicalization above and must
produce the same digest. The approval process then generates
`catalog-derived-manifest-v1.approval.json`, which is not part of its own hash input. It contains
exactly `schemaVersion`, `derivedManifestArtifact`, `derivedManifestSha256`, and
`catalogRevision`. `schemaVersion` is
`geographic-reference-derived-manifest-approval/v1`, `derivedManifestArtifact` is
`catalog-derived-manifest-v1.json`, `derivedManifestSha256` is the computed concrete lowercase
digest, and `catalogRevision` is the concrete `sha256:` value derived from it.

This generated approval artifact, not this prose and not a template value, is the normative
source of the final public revision. Empty, zero-filled, example, placeholder, or non-matching
values are invalid and block migration generation and activation.

## Deterministic Validation

- Verify source and selected artifact hashes before extraction.
- Validate the exact root, fields, explicit nulls, NFC strings, fixed values, array ordering,
  RFC 8785 bytes, digest, approval artifact, and public revision independently.
- Produce exactly one derived manifest containing every public code, source name, parent code,
  public type code and name, source artifact and row/object reference, scheme, identifier,
  coverage, source, and exclusion.
- Verify exactly 249 countries, 433 country name records, 24 provinces, 222 cantons, 1,047
  level-three areas, 1,293 division name records, 1,293 DPA identifiers, and 24 ISO 3166-2
  province identifiers.
- Reject duplicate category sort keys, duplicate country codes, duplicate country-scoped DPA
  codes, orphaned parents, invalid code lengths, excluded study records, and any unapproved
  fourth-level urban parish.
