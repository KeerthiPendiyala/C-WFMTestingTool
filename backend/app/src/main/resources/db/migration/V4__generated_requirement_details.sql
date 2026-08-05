ALTER TABLE requirement
    ADD COLUMN acceptance_criteria TEXT NOT NULL DEFAULT '',
    ADD COLUMN assumptions TEXT NOT NULL DEFAULT '',
    ADD COLUMN dependencies TEXT NOT NULL DEFAULT '';
