-- =============================================================================
-- Geographic Reference Service - V1.0.0 Initial Schema
-- Database: PostgreSQL
-- =============================================================================

-- ─── Custom enum types ──────────────────────────────────────────────────────

CREATE TYPE geographic_record_status AS ENUM (
    'DRAFT', 'ACTIVE', 'DEPRECATED', 'RETIRED'
);

CREATE TYPE geographic_name_type AS ENUM (
    'OFFICIAL', 'COMMON', 'SHORT', 'ALTERNATIVE', 'HISTORICAL'
);

CREATE TYPE geographic_identifier_status AS ENUM (
    'ACTIVE', 'DEPRECATED', 'RETIRED'
);

-- ─── countries ──────────────────────────────────────────────────────────────

CREATE TABLE countries (
    id              UUID            NOT NULL DEFAULT uuidv7(),
    alpha2_code     CHAR(2)         NOT NULL,
    alpha3_code     CHAR(3)         NOT NULL,
    numeric_code    CHAR(3)         NOT NULL,
    default_name    VARCHAR(150)    NOT NULL,
    official_name   VARCHAR(250)    NOT NULL,
    is_independent  BOOLEAN         NOT NULL DEFAULT TRUE,
    status          geographic_record_status NOT NULL DEFAULT 'DRAFT',
    valid_from      DATE,
    valid_until     DATE,
    source_authority VARCHAR(128)   NOT NULL,
    source_reference VARCHAR(300),
    source_revision  VARCHAR(64),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      VARCHAR(128)    NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by      VARCHAR(128)    NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_countries                    PRIMARY KEY (id),
    CONSTRAINT uq_countries_alpha2             UNIQUE (alpha2_code),
    CONSTRAINT uq_countries_alpha3             UNIQUE (alpha3_code),
    CONSTRAINT uq_countries_numeric            UNIQUE (numeric_code),
    CONSTRAINT ck_countries_alpha2             CHECK (alpha2_code  ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_countries_alpha3             CHECK (alpha3_code  ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_countries_numeric            CHECK (numeric_code ~ '^[0-9]{3}$'),
    CONSTRAINT ck_countries_validity           CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from),
    CONSTRAINT ck_countries_nonnegative_version CHECK (version >= 0)
);

CREATE INDEX ix_countries_status_alpha2  ON countries (status, alpha2_code);
CREATE INDEX ix_countries_default_name   ON countries (default_name);

COMMENT ON TABLE countries IS 'Global catalog of countries and ISO-recognized territories.';

-- ─── country_names ──────────────────────────────────────────────────────────

CREATE TABLE country_names (
    id            UUID            NOT NULL DEFAULT uuidv7(),
    country_id    UUID            NOT NULL,
    language_tag  VARCHAR(35)     NOT NULL,
    name_type     geographic_name_type NOT NULL,
    name          VARCHAR(250)    NOT NULL,
    is_preferred  BOOLEAN         NOT NULL DEFAULT FALSE,
    valid_from    DATE,
    valid_until   DATE,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by    VARCHAR(128)    NOT NULL,
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by    VARCHAR(128)    NOT NULL,
    version       BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_country_names                    PRIMARY KEY (id),
    CONSTRAINT fk_country_names_country            FOREIGN KEY (country_id) REFERENCES countries (id) ON DELETE RESTRICT,
    CONSTRAINT uq_country_names_identity           UNIQUE (country_id, language_tag, name_type, name),
    CONSTRAINT ck_country_names_validity           CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from),
    CONSTRAINT ck_country_names_nonnegative_version CHECK (version >= 0)
);

CREATE INDEX ix_country_names_preferred ON country_names (country_id, language_tag, is_preferred);
CREATE INDEX ix_country_names_name      ON country_names (name);

-- Partial unique index: at most one active preferred name per country + language.
CREATE UNIQUE INDEX uq_country_names_active_preferred
    ON country_names (country_id, language_tag)
    WHERE is_preferred = TRUE;

COMMENT ON TABLE country_names IS 'Localized, alternative and historical country names.';

-- ─── administrative_division_types ──────────────────────────────────────────

CREATE TABLE administrative_division_types (
    id               UUID            NOT NULL DEFAULT uuidv7(),
    country_id       UUID            NOT NULL,
    code             VARCHAR(64)     NOT NULL,
    name             VARCHAR(150)    NOT NULL,
    hierarchy_level  SMALLINT        NOT NULL,
    status           geographic_record_status NOT NULL DEFAULT 'DRAFT',
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by       VARCHAR(128)    NOT NULL,
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by       VARCHAR(128)    NOT NULL,
    version          BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_administrative_division_types                    PRIMARY KEY (id),
    CONSTRAINT fk_administrative_division_types_country            FOREIGN KEY (country_id) REFERENCES countries (id) ON DELETE RESTRICT,
    CONSTRAINT uq_administrative_division_types_country_id         UNIQUE (country_id, id),
    CONSTRAINT uq_administrative_division_types_code               UNIQUE (country_id, code),
    CONSTRAINT uq_administrative_division_types_level              UNIQUE (country_id, hierarchy_level),
    CONSTRAINT ck_administrative_division_types_level              CHECK (hierarchy_level > 0),
    CONSTRAINT ck_administrative_division_types_nonnegative_version CHECK (version >= 0)
);

CREATE INDEX ix_administrative_division_types_status
    ON administrative_division_types (country_id, status);

COMMENT ON TABLE administrative_division_types IS 'Country-specific administrative level definitions (e.g. Province L1, Canton L2, Parish L3).';

-- ─── administrative_divisions ───────────────────────────────────────────────

CREATE TABLE administrative_divisions (
    id                  UUID            NOT NULL DEFAULT uuidv7(),
    country_id          UUID            NOT NULL,
    division_type_id    UUID            NOT NULL,
    parent_division_id  UUID,
    canonical_code      VARCHAR(64)     NOT NULL,
    default_name        VARCHAR(200)    NOT NULL,
    official_name       VARCHAR(300),
    status              geographic_record_status NOT NULL DEFAULT 'DRAFT',
    valid_from          DATE,
    valid_until         DATE,
    source_authority    VARCHAR(128)    NOT NULL,
    source_reference    VARCHAR(300),
    source_revision     VARCHAR(64),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by          VARCHAR(128)    NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by          VARCHAR(128)    NOT NULL,
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_administrative_divisions                    PRIMARY KEY (id),
    CONSTRAINT fk_administrative_divisions_country            FOREIGN KEY (country_id) REFERENCES countries (id) ON DELETE RESTRICT,
    CONSTRAINT fk_administrative_divisions_type               FOREIGN KEY (country_id, division_type_id) REFERENCES administrative_division_types (country_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_administrative_divisions_parent             FOREIGN KEY (country_id, parent_division_id) REFERENCES administrative_divisions (country_id, id) ON DELETE RESTRICT,
    CONSTRAINT uq_administrative_divisions_country_id         UNIQUE (country_id, id),
    CONSTRAINT uq_administrative_divisions_canonical_code     UNIQUE (country_id, canonical_code),
    CONSTRAINT ck_administrative_divisions_no_self_parent      CHECK (parent_division_id IS NULL OR parent_division_id <> id),
    CONSTRAINT ck_administrative_divisions_validity            CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from),
    CONSTRAINT ck_administrative_divisions_nonnegative_version CHECK (version >= 0)
);

CREATE INDEX ix_administrative_divisions_type_status
    ON administrative_divisions (country_id, division_type_id, status);
CREATE INDEX ix_administrative_divisions_parent_status
    ON administrative_divisions (country_id, parent_division_id, status);
CREATE INDEX ix_administrative_divisions_default_name
    ON administrative_divisions (country_id, default_name);

COMMENT ON TABLE administrative_divisions IS 'Hierarchical political-administrative divisions within a country.';

-- ─── administrative_division_identifiers ────────────────────────────────────

CREATE TABLE administrative_division_identifiers (
    id                UUID            NOT NULL DEFAULT uuidv7(),
    country_id        UUID            NOT NULL,
    division_id       UUID            NOT NULL,
    scheme_code       VARCHAR(64)     NOT NULL,
    identifier_value  VARCHAR(128)    NOT NULL,
    is_primary        BOOLEAN         NOT NULL DEFAULT FALSE,
    status            geographic_identifier_status NOT NULL DEFAULT 'ACTIVE',
    valid_from        DATE,
    valid_until       DATE,
    source_authority  VARCHAR(128)    NOT NULL,
    source_reference  VARCHAR(300),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by        VARCHAR(128)    NOT NULL,
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by        VARCHAR(128)    NOT NULL,
    version           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_administrative_division_identifiers                    PRIMARY KEY (id),
    CONSTRAINT fk_administrative_division_identifiers_division           FOREIGN KEY (country_id, division_id) REFERENCES administrative_divisions (country_id, id) ON DELETE RESTRICT,
    CONSTRAINT uq_administrative_division_identifier                     UNIQUE (country_id, scheme_code, identifier_value),
    CONSTRAINT ck_administrative_division_identifiers_validity           CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from),
    CONSTRAINT ck_administrative_division_identifiers_nonnegative_version CHECK (version >= 0)
);

CREATE INDEX ix_administrative_division_identifiers_division
    ON administrative_division_identifiers (country_id, division_id, scheme_code, status);
CREATE INDEX ix_administrative_division_identifiers_primary
    ON administrative_division_identifiers (country_id, division_id, is_primary);

-- Partial unique index: at most one active primary identifier per division + scheme.
CREATE UNIQUE INDEX uq_admin_div_identifiers_active_primary
    ON administrative_division_identifiers (country_id, division_id, scheme_code)
    WHERE is_primary = TRUE AND status = 'ACTIVE';

COMMENT ON TABLE administrative_division_identifiers IS 'External identifiers (ISO 3166-2, national statistical codes, etc.) assigned to a division.';

-- ─── administrative_division_names ──────────────────────────────────────────

CREATE TABLE administrative_division_names (
    id            UUID            NOT NULL DEFAULT uuidv7(),
    country_id    UUID            NOT NULL,
    division_id   UUID            NOT NULL,
    language_tag  VARCHAR(35)     NOT NULL,
    name_type     geographic_name_type NOT NULL,
    name          VARCHAR(300)    NOT NULL,
    is_preferred  BOOLEAN         NOT NULL DEFAULT FALSE,
    valid_from    DATE,
    valid_until   DATE,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by    VARCHAR(128)    NOT NULL,
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by    VARCHAR(128)    NOT NULL,
    version       BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_administrative_division_names                    PRIMARY KEY (id),
    CONSTRAINT fk_administrative_division_names_division           FOREIGN KEY (country_id, division_id) REFERENCES administrative_divisions (country_id, id) ON DELETE RESTRICT,
    CONSTRAINT uq_administrative_division_names_identity           UNIQUE (country_id, division_id, language_tag, name_type, name),
    CONSTRAINT ck_administrative_division_names_validity           CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from),
    CONSTRAINT ck_administrative_division_names_nonnegative_version CHECK (version >= 0)
);

CREATE INDEX ix_administrative_division_names_preferred
    ON administrative_division_names (country_id, division_id, language_tag, is_preferred);
CREATE INDEX ix_administrative_division_names_name
    ON administrative_division_names (country_id, name);

-- Partial unique index: at most one active preferred name per division + language.
CREATE UNIQUE INDEX uq_admin_div_names_active_preferred
    ON administrative_division_names (country_id, division_id, language_tag)
    WHERE is_preferred = TRUE;

COMMENT ON TABLE administrative_division_names IS 'Localized, alternative and historical names of an administrative division.';

-- ─── Deferred constraint trigger: hierarchy validation ──────────────────────
-- Validates on INSERT/UPDATE of administrative_divisions:
--   1. Parent belongs to the same country  (already enforced by composite FK).
--   2. Parent hierarchy_level is immediately above child level.
--   3. Root divisions must use the first (lowest) configured hierarchy level.
--   4. Cycles are prohibited.

CREATE OR REPLACE FUNCTION fn_check_admin_division_hierarchy()
RETURNS TRIGGER AS $$
DECLARE
    child_level  SMALLINT;
    parent_level SMALLINT;
    min_level    SMALLINT;
    ancestor_id  UUID;
    depth        INT := 0;
BEGIN
    -- Resolve the child's hierarchy level from its division type.
    SELECT hierarchy_level INTO STRICT child_level
      FROM administrative_division_types
     WHERE id = NEW.division_type_id
       AND country_id = NEW.country_id;

    -- Determine the top-most hierarchy level configured for this country.
    SELECT MIN(hierarchy_level) INTO min_level
      FROM administrative_division_types
     WHERE country_id = NEW.country_id;

    IF NEW.parent_division_id IS NULL THEN
        -- Rule 3: root divisions must sit at the first hierarchy level.
        IF child_level <> min_level THEN
            RAISE EXCEPTION
                'Root division must have hierarchy level %, but got %',
                min_level, child_level;
        END IF;
    ELSE
        -- Rule 2: parent level must be exactly one above the child level.
        SELECT adt.hierarchy_level INTO STRICT parent_level
          FROM administrative_divisions ad
          JOIN administrative_division_types adt
            ON adt.id = ad.division_type_id AND adt.country_id = ad.country_id
         WHERE ad.id = NEW.parent_division_id
           AND ad.country_id = NEW.country_id;

        IF parent_level <> child_level - 1 THEN
            RAISE EXCEPTION
                'Parent hierarchy level must be %, but got %',
                child_level - 1, parent_level;
        END IF;

        -- Rule 4: walk up ancestors to detect cycles.
        ancestor_id := NEW.parent_division_id;
        WHILE ancestor_id IS NOT NULL LOOP
            IF ancestor_id = NEW.id THEN
                RAISE EXCEPTION 'Cycle detected in administrative division hierarchy';
            END IF;
            depth := depth + 1;
            IF depth > 100 THEN
                RAISE EXCEPTION 'Maximum hierarchy depth exceeded — possible cycle';
            END IF;
            SELECT parent_division_id INTO ancestor_id
              FROM administrative_divisions
             WHERE id = ancestor_id
               AND country_id = NEW.country_id;
        END LOOP;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_check_admin_division_hierarchy
    AFTER INSERT OR UPDATE ON administrative_divisions
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION fn_check_admin_division_hierarchy();
