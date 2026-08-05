DO $$
DECLARE
    tenant TEXT := 'dev-tenant';
    admin_id UUID := '00000000-0000-0000-0000-000000000001';
    manager_id UUID := '00000000-0000-0000-0000-000000000002';
    lead_id UUID := '00000000-0000-0000-0000-000000000003';
    analyst_id UUID := '00000000-0000-0000-0000-000000000004';
    abc_project_id UUID := '10000000-0000-0000-0000-000000000001';
    austin_project_id UUID := '10000000-0000-0000-0000-000000000002';
    timekeeping_suite_id UUID := '20000000-0000-0000-0000-000000000001';
    integration_suite_id UUID := '20000000-0000-0000-0000-000000000002';
    personas_suite_id UUID := '20000000-0000-0000-0000-000000000003';
    abc_timekeeping_assignment_id UUID := '30000000-0000-0000-0000-000000000001';
    abc_integration_assignment_id UUID := '30000000-0000-0000-0000-000000000002';
    abc_personas_assignment_id UUID := '30000000-0000-0000-0000-000000000003';
BEGIN
    INSERT INTO approved_tenant (entra_tenant_id, display_name, active)
    VALUES (tenant, 'Development Tenant', TRUE)
    ON CONFLICT DO NOTHING;

    INSERT INTO application_user (id, first_name, last_name, normalized_contact_email, access_status, pre_provisioning_status)
    VALUES
        (admin_id, 'Avery', 'Administrator', 'avery.admin@example.test', 'ACTIVE', 'PRE_PROVISIONED'),
        (manager_id, 'Mia', 'Manager', 'mia.manager@example.test', 'ACTIVE', 'PRE_PROVISIONED'),
        (lead_id, 'Leo', 'Lead', 'leo.lead@example.test', 'ACTIVE', 'PRE_PROVISIONED'),
        (analyst_id, 'Tara', 'Analyst', 'tara.analyst@example.test', 'ACTIVE', 'PRE_PROVISIONED')
    ON CONFLICT DO NOTHING;

    INSERT INTO global_administrator_assignment (user_id, assigned_by)
    VALUES (admin_id, admin_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO project (id, tenant_id, project_key, name, description, created_by)
    VALUES
        (abc_project_id, tenant, 'ABC', 'Australian Broadcasting Corporation', 'Development seed project for requirements and test management.', admin_id),
        (austin_project_id, tenant, 'AUSTIN', 'Austin Health', 'Development seed project for healthcare workforce testing.', admin_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO project_membership (tenant_id, project_id, user_id, project_role, assigned_by, created_by)
    VALUES
        (tenant, abc_project_id, manager_id, 'Test Manager', admin_id, admin_id),
        (tenant, abc_project_id, lead_id, 'Test Lead', manager_id, manager_id),
        (tenant, abc_project_id, analyst_id, 'Test Analyst', manager_id, manager_id),
        (tenant, austin_project_id, manager_id, 'Test Manager', admin_id, admin_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO test_suite (id, tenant_id, suite_key, name, description, created_by)
    VALUES
        (timekeeping_suite_id, tenant, 'TIMEKEEPING', 'Timekeeping', 'Core time capture and schedule validation.', admin_id),
        (integration_suite_id, tenant, 'INTEGRATION', 'Integration', 'Inbound and outbound integration testing.', admin_id),
        (personas_suite_id, tenant, 'PERSONAS', 'Personas', 'Persona-driven access and workflow testing.', admin_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO project_suite_assignment (id, tenant_id, project_id, suite_id, display_order, created_by)
    VALUES
        (abc_timekeeping_assignment_id, tenant, abc_project_id, timekeeping_suite_id, 1, manager_id),
        (abc_integration_assignment_id, tenant, abc_project_id, integration_suite_id, 2, manager_id),
        (abc_personas_assignment_id, tenant, abc_project_id, personas_suite_id, 3, manager_id),
        ('30000000-0000-0000-0000-000000000004', tenant, austin_project_id, timekeeping_suite_id, 1, manager_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO project_test_cycle (tenant_id, project_id, name, start_date, end_date, description, created_by)
    VALUES
        (tenant, abc_project_id, 'Cycle 1 - Timekeeping Baseline', DATE '2026-08-01', DATE '2026-08-31', 'Initial Timekeeping regression cycle.', manager_id),
        (tenant, abc_project_id, 'Cycle 2 - Integration Regression', DATE '2026-09-01', DATE '2026-09-30', 'Integration regression cycle.', manager_id),
        (tenant, austin_project_id, 'Cycle 1 - Personas Smoke', DATE '2026-08-01', DATE '2026-08-15', 'Persona smoke testing cycle.', manager_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO predefined_test_case_template (tenant_id, suite_id, template_key, header, description, source, created_by)
    VALUES
        (tenant, timekeeping_suite_id, 'TIMEKEEPING-CLOCK-IN', 'Validate employee clock-in', 'Confirm time entry is captured for an active employee.', 'Development seed', manager_id),
        (tenant, integration_suite_id, 'INTEGRATION-EXPORT', 'Validate outbound integration export', 'Confirm generated export metadata is available for downstream validation.', 'Development seed', manager_id),
        (tenant, personas_suite_id, 'PERSONA-ACCESS', 'Validate persona access boundary', 'Confirm persona-specific access is scoped to expected workflows.', 'Development seed', manager_id)
    ON CONFLICT DO NOTHING;
END $$;
