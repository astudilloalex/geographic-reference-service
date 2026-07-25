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

- 269 named urban parishes nested below 59 cantonal-seat areas. They require a fourth level
  or another explicitly approved hierarchy model and are outside v1.
- Province code `90`, `140190 SINAI-CUCHAENTZA`, and every other study-zone or
  non-administrative record.
- Alternative, translated, or historical Ecuador division names not supplied by this pinned
  source.

### Extraction Rules

- The authoritative `PARROQUIAS` sheet and detailed classifier take precedence when the
  compact `CODIGOS` sheet is incomplete. This includes valid record `160167 SHUAR PASTAZA`.
- Codes are text and preserve leading zeroes.
- Province and canton relationships are derived from the code prefix. A six-digit record's
  canton is its first four digits, and a canton's province is its first two digits.
- Source records are not silently corrected, renamed, or supplemented. Any discrepancy is
  recorded in validation evidence and resolved through a new approved manifest and migration.

## Legal Gates

- The catalog owner MUST verify all hashes after retrieval and retain the source artifacts,
  attribution, license evidence, extraction report, and derived-record manifest.
- Debian `iso-codes` notices and LGPL obligations MUST be satisfied before distribution.
- INEC's published policy permits public communication and attributed derivative works but
  restricts transfer or commercialization without authorization. The responsible legal or
  data-governance owner MUST approve the intended service use in writing before INEC-derived
  data is committed to a distributable artifact, deployed, or promoted to production.
- Failure to obtain or record required approval blocks catalog activation and traffic
  promotion; it does not authorize replacing the source silently.

## Deterministic Validation

- Verify source and selected artifact hashes before extraction.
- Produce exactly one derived manifest containing every public code, Unicode NFC source name,
  parent code, public type code, source artifact and row/object reference, and identifier.
  Source text is preserved except for Unicode NFC normalization; no trimming, translation,
  spelling correction, accent removal, or case conversion is applied to display names.
- Verify exactly 249 countries, 433 country name records, 24 provinces, 222 cantons, 1,047
  level-three areas, 1,293 division name records, 1,293 DPA identifiers, and 24 ISO 3166-2
  province identifiers.
- Reject duplicate country codes, duplicate country-scoped DPA codes, orphaned parents,
  invalid code lengths, excluded study records, and any unapproved fourth-level urban parish.
- Record a checksum of the derived manifest as the immutable catalog revision input.
