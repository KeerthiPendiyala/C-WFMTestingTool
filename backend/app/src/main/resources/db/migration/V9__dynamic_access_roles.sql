CREATE TABLE access_role (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    administrator_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ux_access_role_tenant_id UNIQUE (tenant_id, id)
);

CREATE UNIQUE INDEX ux_access_role_tenant_name
    ON access_role (tenant_id, lower(name));
CREATE UNIQUE INDEX ux_access_role_tenant_administrator
    ON access_role (tenant_id)
    WHERE administrator_role = TRUE;

CREATE TABLE access_role_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES access_role (id) ON DELETE CASCADE,
    permission_name VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_access_role_permission_name CHECK (
        permission_name IN (
            'VIEW',
            'CREATE',
            'EDIT',
            'EXECUTE',
            'DELETE',
            'APPROVE_REQUIREMENTS',
            'MANAGE_ASSIGNMENTS'
        )
    ),
    CONSTRAINT ux_access_role_permission UNIQUE (role_id, permission_name)
);

CREATE INDEX idx_access_role_permission_role ON access_role_permission (role_id);

CREATE TABLE user_access_role_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    user_id UUID NOT NULL REFERENCES application_user (id) ON DELETE CASCADE,
    role_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES application_user (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES application_user (id),
    CONSTRAINT fk_user_access_role_tenant_role
        FOREIGN KEY (tenant_id, role_id) REFERENCES access_role (tenant_id, id),
    CONSTRAINT ux_user_access_role_assignment UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_user_access_role_assignment_lookup
    ON user_access_role_assignment (tenant_id, user_id, role_id);

CREATE OR REPLACE FUNCTION ensure_default_access_roles(p_tenant_id VARCHAR)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    admin_role_id UUID := (
        substr(md5(p_tenant_id || ':Admin'), 1, 8) || '-' ||
        substr(md5(p_tenant_id || ':Admin'), 9, 4) || '-' ||
        substr(md5(p_tenant_id || ':Admin'), 13, 4) || '-' ||
        substr(md5(p_tenant_id || ':Admin'), 17, 4) || '-' ||
        substr(md5(p_tenant_id || ':Admin'), 21, 12)
    )::UUID;
    manager_role_id UUID := (
        substr(md5(p_tenant_id || ':Test Manager'), 1, 8) || '-' ||
        substr(md5(p_tenant_id || ':Test Manager'), 9, 4) || '-' ||
        substr(md5(p_tenant_id || ':Test Manager'), 13, 4) || '-' ||
        substr(md5(p_tenant_id || ':Test Manager'), 17, 4) || '-' ||
        substr(md5(p_tenant_id || ':Test Manager'), 21, 12)
    )::UUID;
    tester_role_id UUID := (
        substr(md5(p_tenant_id || ':Tester'), 1, 8) || '-' ||
        substr(md5(p_tenant_id || ':Tester'), 9, 4) || '-' ||
        substr(md5(p_tenant_id || ':Tester'), 13, 4) || '-' ||
        substr(md5(p_tenant_id || ':Tester'), 17, 4) || '-' ||
        substr(md5(p_tenant_id || ':Tester'), 21, 12)
    )::UUID;
    viewer_role_id UUID := (
        substr(md5(p_tenant_id || ':Viewer'), 1, 8) || '-' ||
        substr(md5(p_tenant_id || ':Viewer'), 9, 4) || '-' ||
        substr(md5(p_tenant_id || ':Viewer'), 13, 4) || '-' ||
        substr(md5(p_tenant_id || ':Viewer'), 17, 4) || '-' ||
        substr(md5(p_tenant_id || ':Viewer'), 21, 12)
    )::UUID;
BEGIN
    INSERT INTO access_role (id, tenant_id, name, description, administrator_role)
    VALUES
        (admin_role_id, p_tenant_id, 'Admin', 'Full administrative access.', TRUE),
        (manager_role_id, p_tenant_id, 'Test Manager', 'Manages testing work and assignments.', FALSE),
        (tester_role_id, p_tenant_id, 'Tester', 'Creates, edits, executes and deletes test assets.', FALSE),
        (viewer_role_id, p_tenant_id, 'Viewer', 'Read-only access.', FALSE)
    ON CONFLICT DO NOTHING;

    INSERT INTO access_role_permission (role_id, permission_name)
    SELECT role_id, permission_name
    FROM (
        SELECT admin_role_id AS role_id, unnest(ARRAY[
            'VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'APPROVE_REQUIREMENTS', 'MANAGE_ASSIGNMENTS'
        ]) AS permission_name
        UNION ALL
        SELECT manager_role_id, unnest(ARRAY[
            'VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'APPROVE_REQUIREMENTS', 'MANAGE_ASSIGNMENTS'
        ])
        UNION ALL
        SELECT tester_role_id, unnest(ARRAY['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE'])
        UNION ALL
        SELECT viewer_role_id, 'VIEW'
    ) defaults
    ON CONFLICT (role_id, permission_name) DO NOTHING;
END;
$$;

SELECT ensure_default_access_roles(tenant_id)
FROM (
    SELECT entra_tenant_id AS tenant_id FROM approved_tenant
    UNION
    SELECT tenant_id FROM project
    UNION
    SELECT tenant_id FROM local_user_credential
) tenants
WHERE tenant_id IS NOT NULL;

WITH user_tenants AS (
    SELECT user_id, tenant_id FROM local_user_credential
    UNION
    SELECT user_id, tenant_id FROM project_membership
    UNION
    SELECT id AS user_id, entra_tenant_id AS tenant_id
    FROM application_user
    WHERE entra_tenant_id IS NOT NULL
)
INSERT INTO user_access_role_assignment (tenant_id, user_id, role_id, created_by, updated_by)
SELECT DISTINCT ut.tenant_id, ut.user_id, role.id, assignment.assigned_by, assignment.assigned_by
FROM global_administrator_assignment assignment
JOIN user_tenants ut ON ut.user_id = assignment.user_id
JOIN access_role role
    ON role.tenant_id = ut.tenant_id
   AND role.administrator_role = TRUE
WHERE assignment.deleted_at IS NULL
ON CONFLICT (tenant_id, user_id) DO NOTHING;

INSERT INTO user_access_role_assignment (tenant_id, user_id, role_id, created_by, updated_by)
SELECT DISTINCT ON (membership.tenant_id, membership.user_id)
    membership.tenant_id,
    membership.user_id,
    role.id,
    membership.assigned_by,
    membership.assigned_by
FROM project_membership membership
JOIN access_role role
    ON role.tenant_id = membership.tenant_id
   AND role.name = CASE membership.project_role
        WHEN 'Test Manager' THEN 'Test Manager'
        WHEN 'Test Lead' THEN 'Tester'
        ELSE 'Tester'
   END
WHERE membership.deleted_at IS NULL
  AND membership.membership_status = 'ACTIVE'
ORDER BY membership.tenant_id, membership.user_id, membership.assigned_at
ON CONFLICT (tenant_id, user_id) DO NOTHING;
