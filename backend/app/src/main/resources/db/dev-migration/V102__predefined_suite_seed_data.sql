DO $$
DECLARE
    tenant TEXT := 'dev-tenant';
    admin_id UUID := '00000000-0000-0000-0000-000000000001';
    manager_id UUID := '00000000-0000-0000-0000-000000000002';
    requested_pd_timekeeping_suite_id UUID := '20000000-0000-0000-0000-000000000004';
    pd_timekeeping_suite_id UUID;
BEGIN
    INSERT INTO test_suite (id, tenant_id, suite_key, name, description, created_by)
    VALUES (
        requested_pd_timekeeping_suite_id,
        tenant,
        'PD_TIMEKEEPING',
        'PD-Timekeeping',
        'Pre Defined Timekeeping validation templates.',
        admin_id
    )
    ON CONFLICT DO NOTHING;

    SELECT id
    INTO pd_timekeeping_suite_id
    FROM test_suite
    WHERE tenant_id = tenant
      AND suite_key = 'PD_TIMEKEEPING'
      AND deleted_at IS NULL
    ORDER BY created_at
    LIMIT 1;

    IF pd_timekeeping_suite_id IS NULL THEN
        RAISE EXCEPTION 'PD_TIMEKEEPING suite was not available for predefined seed data.';
    END IF;

    INSERT INTO predefined_test_case_template (
        tenant_id,
        suite_id,
        template_key,
        header,
        description,
        source,
        created_by
    )
    VALUES
        (
            tenant,
            pd_timekeeping_suite_id,
            'PD_TIMEKEEPING_CLOCK_IN',
            'Validate employee clock-in',
            'Confirm time entry is captured for an active employee.',
            'Development seed',
            manager_id
        ),
        (
            tenant,
            pd_timekeeping_suite_id,
            'PD_TIMEKEEPING_CLOCK_OUT',
            'Validate employee clock-out',
            'Confirm an active employee can clock out successfully.',
            'Development seed',
            manager_id
        )
    ON CONFLICT DO NOTHING;
END $$;
