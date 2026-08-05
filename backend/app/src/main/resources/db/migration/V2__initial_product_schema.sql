CREATE TABLE approved_tenant (
    entra_tenant_id VARCHAR(120) PRIMARY KEY,
    display_name VARCHAR(240) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE application_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    normalized_contact_email VARCHAR(320) NOT NULL,
    pre_provisioning_status VARCHAR(40) NOT NULL DEFAULT 'PRE_PROVISIONED',
    access_status VARCHAR(40) NOT NULL DEFAULT 'INVITED',
    entra_tenant_id VARCHAR(120),
    entra_object_id VARCHAR(120),
    first_login_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_application_user_email_normalized CHECK (normalized_contact_email = lower(normalized_contact_email)),
    CONSTRAINT ck_application_user_pre_provisioning_status CHECK (pre_provisioning_status IN ('PRE_PROVISIONED', 'BOUND')),
    CONSTRAINT ck_application_user_access_status CHECK (access_status IN ('INVITED', 'ACTIVE', 'DISABLED')),
    CONSTRAINT ck_application_user_entra_binding CHECK (
        (entra_tenant_id IS NULL AND entra_object_id IS NULL)
        OR (entra_tenant_id IS NOT NULL AND entra_object_id IS NOT NULL)
    ),
    CONSTRAINT fk_application_user_approved_tenant FOREIGN KEY (entra_tenant_id) REFERENCES approved_tenant (entra_tenant_id)
);

CREATE UNIQUE INDEX ux_application_user_email_active
    ON application_user (normalized_contact_email)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_application_user_entra_binding
    ON application_user (entra_tenant_id, entra_object_id)
    WHERE deleted_at IS NULL AND entra_tenant_id IS NOT NULL AND entra_object_id IS NOT NULL;
CREATE INDEX idx_application_user_access_status ON application_user (access_status);

CREATE TABLE global_administrator_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES application_user (id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES application_user (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_global_admin_user_active
    ON global_administrator_assignment (user_id)
    WHERE deleted_at IS NULL;

CREATE TABLE project (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_key VARCHAR(80) NOT NULL,
    name VARCHAR(240) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_project_key_normalized CHECK (project_key = upper(project_key))
);

CREATE UNIQUE INDEX ux_project_tenant_key_active
    ON project (tenant_id, project_key)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_project_tenant_name_active
    ON project (tenant_id, lower(name))
    WHERE deleted_at IS NULL;
CREATE INDEX idx_project_tenant_active ON project (tenant_id, active) WHERE deleted_at IS NULL;
ALTER TABLE project ADD CONSTRAINT ux_project_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE project_membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    user_id UUID NOT NULL REFERENCES application_user (id),
    project_role VARCHAR(40) NOT NULL,
    membership_status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES application_user (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_project_membership_role CHECK (project_role IN ('Test Manager', 'Test Lead', 'Test Analyst')),
    CONSTRAINT ck_project_membership_status CHECK (membership_status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT fk_project_membership_tenant_project FOREIGN KEY (tenant_id, project_id) REFERENCES project (tenant_id, id)
);

ALTER TABLE project_membership ADD CONSTRAINT ux_project_membership_project_id_id UNIQUE (project_id, id);
CREATE UNIQUE INDEX ux_project_membership_project_user_active
    ON project_membership (project_id, user_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_project_membership_user_active ON project_membership (user_id, membership_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_project_membership_project_role ON project_membership (project_id, project_role) WHERE deleted_at IS NULL;

CREATE TABLE test_suite (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120),
    suite_key VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_test_suite_key_normalized CHECK (suite_key = upper(suite_key))
);

CREATE UNIQUE INDEX ux_test_suite_tenant_key_active
    ON test_suite (coalesce(tenant_id, ''), suite_key)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_test_suite_tenant_active ON test_suite (tenant_id, active) WHERE deleted_at IS NULL;

CREATE TABLE project_suite_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    suite_id UUID NOT NULL REFERENCES test_suite (id),
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_project_suite_assignment_tenant_project FOREIGN KEY (tenant_id, project_id) REFERENCES project (tenant_id, id)
);

ALTER TABLE project_suite_assignment ADD CONSTRAINT ux_project_suite_assignment_project_id_id UNIQUE (project_id, id);
CREATE UNIQUE INDEX ux_project_suite_assignment_active
    ON project_suite_assignment (project_id, suite_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_project_suite_assignment_project ON project_suite_assignment (project_id, active) WHERE deleted_at IS NULL;

CREATE OR REPLACE FUNCTION assert_project_suite_assignment_scope()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM test_suite ts
        WHERE ts.id = NEW.suite_id
          AND ts.deleted_at IS NULL
          AND (ts.tenant_id IS NULL OR ts.tenant_id = NEW.tenant_id)
    ) THEN
        RAISE EXCEPTION 'project suite assignment must use a global suite or same-tenant suite'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_project_suite_assignment_scope
    BEFORE INSERT OR UPDATE OF tenant_id, suite_id
    ON project_suite_assignment
    FOR EACH ROW
    EXECUTE FUNCTION assert_project_suite_assignment_scope();

CREATE TABLE project_test_cycle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    name VARCHAR(160) NOT NULL,
    start_date DATE,
    end_date DATE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_project_test_cycle_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT fk_project_test_cycle_tenant_project FOREIGN KEY (tenant_id, project_id) REFERENCES project (tenant_id, id)
);

ALTER TABLE project_test_cycle ADD CONSTRAINT ux_project_test_cycle_project_id_id UNIQUE (project_id, id);
CREATE UNIQUE INDEX ux_project_test_cycle_name_active
    ON project_test_cycle (project_id, lower(name))
    WHERE deleted_at IS NULL;
CREATE INDEX idx_project_test_cycle_project_dates ON project_test_cycle (project_id, start_date, end_date) WHERE deleted_at IS NULL;

CREATE TABLE project_identifier_counter (
    project_id UUID NOT NULL REFERENCES project (id),
    identifier_type VARCHAR(40) NOT NULL,
    next_value INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (project_id, identifier_type),
    CONSTRAINT ck_project_identifier_counter_type CHECK (identifier_type IN ('REQ', 'TC')),
    CONSTRAINT ck_project_identifier_counter_next_value CHECK (next_value > 0)
);

CREATE OR REPLACE FUNCTION allocate_project_identifier(p_project_id UUID, p_identifier_type VARCHAR)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    allocated INTEGER;
BEGIN
    INSERT INTO project_identifier_counter (project_id, identifier_type, next_value)
    VALUES (p_project_id, p_identifier_type, 2)
    ON CONFLICT (project_id, identifier_type)
    DO UPDATE SET
        next_value = project_identifier_counter.next_value + 1,
        updated_at = now(),
        version = project_identifier_counter.version + 1
    RETURNING next_value - 1 INTO allocated;

    RETURN allocated;
END;
$$;

CREATE TABLE uploaded_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    project_suite_assignment_id UUID NOT NULL,
    test_cycle_id UUID NOT NULL,
    original_filename VARCHAR(512) NOT NULL,
    content_type VARCHAR(160) NOT NULL,
    byte_size BIGINT NOT NULL,
    document_status VARCHAR(40) NOT NULL DEFAULT 'UPLOADED',
    source_type VARCHAR(40) NOT NULL,
    storage_provider VARCHAR(80),
    object_reference VARCHAR(512),
    content_hash VARCHAR(128),
    uploaded_by UUID REFERENCES application_user (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_uploaded_document_project_suite
        FOREIGN KEY (project_id, project_suite_assignment_id)
        REFERENCES project_suite_assignment (project_id, id),
    CONSTRAINT fk_uploaded_document_project_cycle
        FOREIGN KEY (project_id, test_cycle_id)
        REFERENCES project_test_cycle (project_id, id),
    CONSTRAINT fk_uploaded_document_tenant_project
        FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT ck_uploaded_document_status CHECK (document_status IN ('UPLOADED', 'PROCESSING', 'PROCESSED', 'FAILED')),
    CONSTRAINT ck_uploaded_document_source_type CHECK (source_type IN ('PDF', 'DOCX', 'DOC', 'CSV')),
    CONSTRAINT ck_uploaded_document_byte_size CHECK (byte_size >= 0)
);

CREATE INDEX idx_uploaded_document_project_created ON uploaded_document (project_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE TABLE generation_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID REFERENCES project (id),
    job_type VARCHAR(80) NOT NULL,
    source_type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(160),
    provider_kind VARCHAR(80),
    model_name VARCHAR(160),
    prompt_hash VARCHAR(128),
    source_document_id UUID REFERENCES uploaded_document (id),
    requested_by UUID REFERENCES application_user (id),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    result_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_generation_job_type CHECK (job_type IN ('REQUIREMENT_EXTRACTION', 'TEST_CASE_GENERATION', 'PREDEFINED_TEST_CASE_GENERATION')),
    CONSTRAINT ck_generation_job_source_type CHECK (source_type IN ('UPLOAD', 'MANUAL', 'AI', 'PREDEFINED')),
    CONSTRAINT ck_generation_job_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT fk_generation_job_tenant_project
        FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id)
);

CREATE UNIQUE INDEX ux_generation_job_idempotency
    ON generation_job (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_generation_job_project_status ON generation_job (project_id, status, created_at DESC);

CREATE TABLE requirement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    project_suite_assignment_id UUID NOT NULL,
    test_cycle_id UUID NOT NULL,
    req_sequence INTEGER NOT NULL,
    req_id VARCHAR(40) NOT NULL,
    header VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'Draft',
    source_type VARCHAR(40) NOT NULL,
    source_document_id UUID REFERENCES uploaded_document (id),
    generation_job_id UUID REFERENCES generation_job (id),
    approved_at TIMESTAMPTZ,
    approved_by UUID REFERENCES application_user (id),
    created_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_requirement_project_suite
        FOREIGN KEY (project_id, project_suite_assignment_id)
        REFERENCES project_suite_assignment (project_id, id),
    CONSTRAINT fk_requirement_project_cycle
        FOREIGN KEY (project_id, test_cycle_id)
        REFERENCES project_test_cycle (project_id, id),
    CONSTRAINT fk_requirement_tenant_project
        FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT ck_requirement_status CHECK (status IN ('Draft', 'Approved')),
    CONSTRAINT ck_requirement_source_type CHECK (source_type IN ('MANUAL', 'PDF', 'DOCX', 'DOC', 'CSV', 'AI')),
    CONSTRAINT ck_requirement_req_id_format CHECK (req_id ~ '^REQ-[0-9]{3,}$'),
    CONSTRAINT ck_requirement_approval_audit CHECK (
        (status = 'Approved' AND approved_at IS NOT NULL AND approved_by IS NOT NULL)
        OR (status = 'Draft' AND approved_at IS NULL)
    )
);

ALTER TABLE requirement ADD CONSTRAINT ux_requirement_project_id_id UNIQUE (project_id, id);
ALTER TABLE requirement ADD CONSTRAINT ux_requirement_project_id_id_suite_cycle
    UNIQUE (project_id, id, project_suite_assignment_id, test_cycle_id);
CREATE UNIQUE INDEX ux_requirement_project_req_id_active
    ON requirement (project_id, req_id)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_requirement_project_sequence_active
    ON requirement (project_id, req_sequence)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_requirement_project_suite_cycle_status
    ON requirement (project_id, project_suite_assignment_id, test_cycle_id, status)
    WHERE deleted_at IS NULL;

CREATE TABLE predefined_test_case_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120),
    suite_id UUID REFERENCES test_suite (id),
    template_key VARCHAR(120) NOT NULL,
    header VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    source VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_predefined_template_key_active
    ON predefined_test_case_template (coalesce(tenant_id, ''), template_key)
    WHERE deleted_at IS NULL;

CREATE TABLE test_case (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    requirement_id UUID,
    test_case_sequence INTEGER NOT NULL,
    test_case_id VARCHAR(40) NOT NULL,
    project_suite_assignment_id UUID NOT NULL,
    test_cycle_id UUID NOT NULL,
    assignee_membership_id UUID,
    header VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'Draft',
    created_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_date DATE,
    source_type VARCHAR(40) NOT NULL,
    predefined_template_id UUID REFERENCES predefined_test_case_template (id),
    generation_job_id UUID REFERENCES generation_job (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_test_case_project_requirement
        FOREIGN KEY (project_id, requirement_id)
        REFERENCES requirement (project_id, id),
    CONSTRAINT fk_test_case_project_suite
        FOREIGN KEY (project_id, project_suite_assignment_id)
        REFERENCES project_suite_assignment (project_id, id),
    CONSTRAINT fk_test_case_project_cycle
        FOREIGN KEY (project_id, test_cycle_id)
        REFERENCES project_test_cycle (project_id, id),
    CONSTRAINT fk_test_case_project_assignee
        FOREIGN KEY (project_id, assignee_membership_id)
        REFERENCES project_membership (project_id, id),
    CONSTRAINT fk_test_case_tenant_project
        FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT fk_test_case_requirement_scope
        FOREIGN KEY (project_id, requirement_id, project_suite_assignment_id, test_cycle_id)
        REFERENCES requirement (project_id, id, project_suite_assignment_id, test_cycle_id),
    CONSTRAINT ck_test_case_status CHECK (status IN ('Draft', 'Inprogress', 'Defect', 'Resolved', 'Not applicable', 'Retest')),
    CONSTRAINT ck_test_case_source_type CHECK (source_type IN ('MANUAL', 'CSV', 'AI', 'PREDEFINED')),
    CONSTRAINT ck_test_case_id_format CHECK (test_case_id ~ '^TC-[0-9]{3,}$'),
    CONSTRAINT ck_test_case_predefined_source CHECK (
        (source_type = 'PREDEFINED' AND requirement_id IS NULL AND predefined_template_id IS NOT NULL)
        OR (source_type <> 'PREDEFINED' AND predefined_template_id IS NULL)
    )
);

ALTER TABLE test_case ADD CONSTRAINT ux_test_case_project_id_id UNIQUE (project_id, id);
CREATE UNIQUE INDEX ux_test_case_project_test_case_id_active
    ON test_case (project_id, test_case_id)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_test_case_project_sequence_active
    ON test_case (project_id, test_case_sequence)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_test_case_project_filters
    ON test_case (project_id, project_suite_assignment_id, test_cycle_id, status, due_date)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_test_case_requirement ON test_case (requirement_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_test_case_assignee ON test_case (assignee_membership_id) WHERE deleted_at IS NULL;

CREATE OR REPLACE FUNCTION assert_active_test_case_assignee()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.assignee_membership_id IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM project_membership pm
        WHERE pm.id = NEW.assignee_membership_id
          AND pm.project_id = NEW.project_id
          AND pm.membership_status = 'ACTIVE'
          AND pm.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'test case assignee must be an active member of the same project'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_test_case_active_assignee
    BEFORE INSERT OR UPDATE OF assignee_membership_id, project_id
    ON test_case
    FOR EACH ROW
    EXECUTE FUNCTION assert_active_test_case_assignee();

CREATE OR REPLACE FUNCTION prevent_requirement_delete_when_linked()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' OR (NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL) THEN
        IF EXISTS (
            SELECT 1
            FROM test_case tc
            WHERE tc.requirement_id = OLD.id
              AND tc.deleted_at IS NULL
        ) THEN
            RAISE EXCEPTION 'requirement cannot be deleted while linked test cases exist'
                USING ERRCODE = '23503';
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_requirement_prevent_invalid_delete
    BEFORE DELETE OR UPDATE OF deleted_at
    ON requirement
    FOR EACH ROW
    EXECUTE FUNCTION prevent_requirement_delete_when_linked();

CREATE OR REPLACE FUNCTION prevent_non_draft_test_case_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF (TG_OP = 'DELETE' OR (NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL)) AND OLD.status <> 'Draft' THEN
        RAISE EXCEPTION 'test case can be deleted only while Draft'
            USING ERRCODE = '23514';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_test_case_prevent_invalid_delete
    BEFORE DELETE OR UPDATE OF deleted_at
    ON test_case
    FOR EACH ROW
    EXECUTE FUNCTION prevent_non_draft_test_case_delete();

CREATE TABLE feature_flag (
    flag_key VARCHAR(120) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0
);

INSERT INTO feature_flag (flag_key, enabled, description)
VALUES
    ('execution.contracts.enabled', FALSE, 'Enables future execution contract APIs and workers.'),
    ('evidence.artifacts.enabled', FALSE, 'Enables future evidence artifact APIs and provider writes.');

CREATE TABLE execution_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    feature_flag_key VARCHAR(120) NOT NULL DEFAULT 'execution.contracts.enabled' REFERENCES feature_flag (flag_key),
    status VARCHAR(40) NOT NULL DEFAULT 'DISABLED',
    requested_by UUID REFERENCES application_user (id),
    idempotency_key VARCHAR(160),
    run_context JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_execution_run_status CHECK (status IN ('DISABLED', 'REQUESTED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT fk_execution_run_tenant_project
        FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id)
);

CREATE UNIQUE INDEX ux_execution_run_idempotency
    ON execution_run (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_execution_run_project_status ON execution_run (project_id, status, created_at DESC) WHERE deleted_at IS NULL;

CREATE TABLE test_case_execution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    execution_run_id UUID NOT NULL REFERENCES execution_run (id),
    test_case_id UUID NOT NULL,
    feature_flag_key VARCHAR(120) NOT NULL DEFAULT 'execution.contracts.enabled' REFERENCES feature_flag (flag_key),
    status VARCHAR(40) NOT NULL DEFAULT 'DISABLED',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    result_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_test_case_execution_project_case
        FOREIGN KEY (project_id, test_case_id)
        REFERENCES test_case (project_id, id),
    CONSTRAINT fk_test_case_execution_tenant_project
        FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT ck_test_case_execution_status CHECK (status IN ('DISABLED', 'QUEUED', 'RUNNING', 'PASSED', 'FAILED', 'BLOCKED', 'CANCELLED')),
    CONSTRAINT ck_test_case_execution_dates CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);

CREATE INDEX idx_test_case_execution_run ON test_case_execution (execution_run_id);
CREATE INDEX idx_test_case_execution_project_case ON test_case_execution (project_id, test_case_id);

CREATE TABLE evidence_artifact (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    feature_flag_key VARCHAR(120) NOT NULL DEFAULT 'evidence.artifacts.enabled' REFERENCES feature_flag (flag_key),
    evidence_type VARCHAR(80) NOT NULL,
    storage_provider VARCHAR(80) NOT NULL,
    object_reference VARCHAR(512) NOT NULL,
    content_hash VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_evidence_artifact_tenant_project
        FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT ck_evidence_artifact_type CHECK (evidence_type IN ('SCREENSHOT', 'TRACE', 'API', 'LOG', 'EXPORT', 'UPLOAD'))
);

CREATE INDEX idx_evidence_artifact_project_created ON evidence_artifact (project_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE TABLE evidence_artifact_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    project_id UUID NOT NULL REFERENCES project (id),
    evidence_artifact_id UUID NOT NULL REFERENCES evidence_artifact (id),
    requirement_id UUID,
    test_case_id UUID,
    test_case_execution_id UUID REFERENCES test_case_execution (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    CONSTRAINT fk_evidence_link_project_requirement
        FOREIGN KEY (project_id, requirement_id)
        REFERENCES requirement (project_id, id),
    CONSTRAINT fk_evidence_link_project_test_case
        FOREIGN KEY (project_id, test_case_id)
        REFERENCES test_case (project_id, id),
    CONSTRAINT fk_evidence_link_tenant_project
        FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT ck_evidence_link_has_target CHECK (
        requirement_id IS NOT NULL
        OR test_case_id IS NOT NULL
        OR test_case_execution_id IS NOT NULL
    )
);

CREATE INDEX idx_evidence_link_artifact ON evidence_artifact_link (evidence_artifact_id);
CREATE INDEX idx_evidence_link_project_case ON evidence_artifact_link (project_id, test_case_id);

CREATE OR REPLACE FUNCTION assert_execution_feature_enabled()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status <> 'DISABLED' AND NOT EXISTS (
        SELECT 1
        FROM feature_flag ff
        WHERE ff.flag_key = NEW.feature_flag_key
          AND ff.enabled = TRUE
    ) THEN
        RAISE EXCEPTION 'execution contract status changes require an enabled feature flag'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_execution_run_feature_flag
    BEFORE INSERT OR UPDATE OF status, feature_flag_key
    ON execution_run
    FOR EACH ROW
    EXECUTE FUNCTION assert_execution_feature_enabled();

CREATE TRIGGER trg_test_case_execution_feature_flag
    BEFORE INSERT OR UPDATE OF status, feature_flag_key
    ON test_case_execution
    FOR EACH ROW
    EXECUTE FUNCTION assert_execution_feature_enabled();

CREATE OR REPLACE FUNCTION assert_evidence_feature_enabled()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM feature_flag ff
        WHERE ff.flag_key = NEW.feature_flag_key
          AND ff.enabled = TRUE
    ) THEN
        RAISE EXCEPTION 'evidence artifact writes require an enabled feature flag'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_evidence_artifact_feature_flag
    BEFORE INSERT OR UPDATE OF feature_flag_key
    ON evidence_artifact
    FOR EACH ROW
    EXECUTE FUNCTION assert_evidence_feature_enabled();

ALTER TABLE audit_event
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE outbox_event
    ADD CONSTRAINT ck_outbox_event_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED', 'DEAD_LETTER'));

ALTER TABLE async_job
    ADD CONSTRAINT ck_async_job_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'DEAD_LETTER'));
