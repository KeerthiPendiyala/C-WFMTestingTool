ALTER TABLE test_case DROP CONSTRAINT ck_test_case_source_type;

ALTER TABLE test_case
    ADD CONSTRAINT ck_test_case_source_type CHECK (
        source_type IN ('MANUAL', 'CSV', 'AI', 'PREDEFINED', 'MANUAL_ADHOC', 'CSV_ADHOC')
    );
