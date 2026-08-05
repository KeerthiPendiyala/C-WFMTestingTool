ALTER TABLE application_user
    ADD COLUMN last_claim_email VARCHAR(320),
    ADD COLUMN last_claim_preferred_username VARCHAR(320),
    ADD COLUMN last_claim_name VARCHAR(240);

ALTER TABLE application_user
    ADD CONSTRAINT ck_application_user_last_claim_email_normalized
        CHECK (last_claim_email IS NULL OR last_claim_email = lower(last_claim_email)),
    ADD CONSTRAINT ck_application_user_last_claim_preferred_username_normalized
        CHECK (
            last_claim_preferred_username IS NULL
            OR last_claim_preferred_username = lower(last_claim_preferred_username)
        );
