ALTER TABLE application_user
    ADD COLUMN assignment_scoped BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE local_user_credential (
    user_id UUID PRIMARY KEY REFERENCES application_user (id) ON DELETE CASCADE,
    tenant_id VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_local_user_credential_hash CHECK (password_hash LIKE '$2%')
);

CREATE TABLE user_project_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    user_id UUID NOT NULL REFERENCES application_user (id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    permission_name VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    CONSTRAINT fk_user_project_permission_scope FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT ck_user_project_permission_name CHECK (
        permission_name IN ('VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'MANAGE_ASSIGNMENTS')
    ),
    CONSTRAINT ux_user_project_permission UNIQUE (user_id, project_id, permission_name)
);

CREATE TABLE user_suite_scope (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    user_id UUID NOT NULL REFERENCES application_user (id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    project_suite_assignment_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    CONSTRAINT fk_user_suite_scope_project FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT fk_user_suite_scope_assignment FOREIGN KEY (project_id, project_suite_assignment_id)
        REFERENCES project_suite_assignment (project_id, id),
    CONSTRAINT ux_user_suite_scope UNIQUE (user_id, project_suite_assignment_id)
);

CREATE TABLE user_cycle_scope (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    user_id UUID NOT NULL REFERENCES application_user (id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    test_cycle_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    CONSTRAINT fk_user_cycle_scope_project FOREIGN KEY (tenant_id, project_id)
        REFERENCES project (tenant_id, id),
    CONSTRAINT fk_user_cycle_scope_cycle FOREIGN KEY (project_id, test_cycle_id)
        REFERENCES project_test_cycle (project_id, id),
    CONSTRAINT ux_user_cycle_scope UNIQUE (user_id, test_cycle_id)
);

CREATE INDEX idx_user_project_permission_lookup
    ON user_project_permission (tenant_id, user_id, project_id);
CREATE INDEX idx_user_suite_scope_lookup
    ON user_suite_scope (tenant_id, user_id, project_id);
CREATE INDEX idx_user_cycle_scope_lookup
    ON user_cycle_scope (tenant_id, user_id, project_id);
