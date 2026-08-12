ALTER TABLE user_project_permission
    DROP CONSTRAINT ck_user_project_permission_name;

ALTER TABLE user_project_permission
    ADD CONSTRAINT ck_user_project_permission_name CHECK (
        permission_name IN (
            'VIEW',
            'CREATE',
            'EDIT',
            'EXECUTE',
            'DELETE',
            'APPROVE_REQUIREMENTS',
            'MANAGE_ASSIGNMENTS'
        )
    );
