SELECT ensure_default_access_roles(entra_tenant_id)
FROM approved_tenant
WHERE active = TRUE;

WITH user_tenants AS (
    SELECT user_id, tenant_id FROM local_user_credential
    UNION
    SELECT user_id, tenant_id FROM project_membership
)
INSERT INTO user_access_role_assignment (tenant_id, user_id, role_id, created_by, updated_by)
SELECT DISTINCT ut.tenant_id, ut.user_id, role.id, assignment.assigned_by, assignment.assigned_by
FROM global_administrator_assignment assignment
JOIN user_tenants ut ON ut.user_id = assignment.user_id
JOIN access_role role ON role.tenant_id = ut.tenant_id AND role.administrator_role = TRUE
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
