-- Convert certificate_templates.placeholders_json from jsonb to text
-- to align with the JPA mapping (String).

ALTER TABLE certificate_templates
    ALTER COLUMN placeholders_json
    TYPE text
    USING placeholders_json::text;
