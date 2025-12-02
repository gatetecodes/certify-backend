-- Convert certificate_templates.placeholders_json from text back to jsonb
-- now that the JPA mapping uses Hibernate's JSON type.

ALTER TABLE certificate_templates
    ALTER COLUMN placeholders_json
    TYPE jsonb
    USING placeholders_json::jsonb;
