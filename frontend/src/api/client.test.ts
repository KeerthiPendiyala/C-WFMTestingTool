import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  deleteProjectCycle,
  deleteSuite,
  generateRequirementsFromDocument,
  localAdminLogin,
  updateUser
} from './client';

describe('API client local auth', () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
  });

  it('posts the environment-provided local Administrator credential to the dev-only endpoint', async () => {
    const runtimePassword = crypto.randomUUID();

    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              userId: '11111111-1111-4111-8111-111111111111',
              tenantId: 'dev-tenant',
              objectId: 'local-admin',
              firstName: 'Avery',
              lastName: 'Administrator',
              contactEmail: 'avery.admin@example.test',
              globalAdministrator: true,
              principalKey: 'dev-tenant:local-admin',
              globalCapabilities: ['PROJECT_CREATE'],
              permissions: ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE'],
              projectPermissions: {}
            })
        } as Response)
      )
    );

    await localAdminLogin({
      username: 'avery.admin@example.test',
      password: runtimePassword
    });

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/auth/local-login',
      expect.objectContaining({
        method: 'POST',
        credentials: 'same-origin',
        body: JSON.stringify({
          username: 'avery.admin@example.test',
          password: runtimePassword
        })
      })
    );
  });

  it('posts requirement generation as multipart data to a relative API URL', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              jobId: 'job-1',
              documentName: 'requirements.csv',
              generatedRequirementCount: 4
            })
        } as Response)
      )
    );

    await generateRequirementsFromDocument(
      'access-token',
      new File(['header,description'], 'requirements.csv', { type: 'text/csv' }),
      {
        projectId: 'project-1',
        projectSuiteAssignmentId: 'suite-assignment-1',
        testCycleId: 'cycle-1'
      }
    );

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/generation-jobs',
      expect.objectContaining({
        method: 'POST',
        credentials: 'same-origin'
      })
    );
    const request = vi.mocked(fetch).mock.calls[0]?.[1];
    expect(request?.body).toBeInstanceOf(FormData);
    const formData = request?.body as FormData;
    expect(formData.get('projectId')).toBe('project-1');
    expect(formData.get('projectSuiteAssignmentId')).toBe('suite-assignment-1');
    expect(formData.get('testCycleId')).toBe('cycle-1');
    expect(formData.has('generationOptions')).toBe(false);
    expect(new Headers(request?.headers).get('Authorization')).toBe('Bearer access-token');
  });

  it('surfaces safe backend problem details for generation failures', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 502,
          json: () =>
            Promise.resolve({
              title: 'Requirement generation failed',
              detail: 'OpenAI could not generate requirements right now. Try again shortly.'
            })
        } as Response)
      )
    );

    await expect(
      generateRequirementsFromDocument(null, new File(['content'], 'requirements.csv'), {
        projectId: 'project-1',
        projectSuiteAssignmentId: 'suite-assignment-1',
        testCycleId: 'cycle-1'
      })
    ).rejects.toThrow('OpenAI could not generate requirements right now. Try again shortly.');
  });

  it('surfaces the backend conflict reason when a suite cannot be deleted', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 409,
          json: () =>
            Promise.resolve({
              title: 'Conflict',
              detail: 'Suite assignment is referenced by requirements or test cases.'
            })
        } as Response)
      )
    );

    await expect(deleteSuite('access-token', 'project-1', 'suite-1', 0)).rejects.toThrow(
      'Suite assignment is referenced by requirements or test cases.'
    );
  });

  it('surfaces the reference details when a cycle cannot be deleted', async () => {
    const detail =
      'Cycle cannot be deleted because it is referenced by 1 requirement and 0 test cases. Reassign or remove those references before deleting the cycle.';
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 409,
          json: () => Promise.resolve({ title: 'Conflict', detail })
        } as Response)
      )
    );

    await expect(deleteProjectCycle('access-token', 'project-1', 'cycle-1', 0)).rejects.toThrow(
      detail
    );
  });

  it('patches an application user with the Administrator access token', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              id: 'user-1',
              firstName: 'Alex',
              lastName: 'Analyst',
              email: 'alex@example.test',
              roleId: 'role-viewer',
              roleName: 'Viewer',
              administratorRole: false,
              status: 'ACTIVE',
              projectIds: ['project-1'],
              permissions: ['VIEW', 'EDIT']
            })
        } as Response)
      )
    );

    await updateUser('admin-token', 'user-1', {
      firstName: 'Alex',
      lastName: 'Analyst',
      email: 'alex@example.test',
      roleId: 'role-viewer',
      status: 'ACTIVE',
      projectIds: ['project-1'],
      newPassword: 'Updated1!Password',
      confirmNewPassword: 'Updated1!Password'
    });

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/users/user-1',
      expect.objectContaining({
        method: 'PATCH',
        credentials: 'same-origin',
        body: JSON.stringify({
          firstName: 'Alex',
          lastName: 'Analyst',
          email: 'alex@example.test',
          roleId: 'role-viewer',
          status: 'ACTIVE',
          projectIds: ['project-1'],
          newPassword: 'Updated1!Password',
          confirmNewPassword: 'Updated1!Password'
        })
      })
    );
    const request = vi.mocked(fetch).mock.calls[0]?.[1];
    expect(new Headers(request?.headers).get('Authorization')).toBe('Bearer admin-token');
  });
});
