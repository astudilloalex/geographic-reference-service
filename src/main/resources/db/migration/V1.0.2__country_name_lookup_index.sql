CREATE INDEX ix_country_names_language_type
    ON country_names (LOWER(language_tag), name_type, country_id);
