import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { App } from './App';
import { allCapabilities } from './shellFixtures';

const loginRedirect = vi.fn();
const logoutRedirect = vi.fn();
const acquireTokenSilent = vi.fn();
const getActiveAccount = vi.fn();

let authenticated = false;
let accounts: { homeAccountId: string }[] = [];
let adminSession = false;
let readonlySession = false;
let managerPermissions = ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'MANAGE_ASSIGNMENTS'];
let personasSuiteCreated = false;
let secondCycleCreated = false;

vi.mock('@azure/msal-react', () => ({
  useIsAuthenticated: () => authenticated,
  useMsal: () => ({
    accounts,
    inProgress: 'none',
    instance: {
      loginRedirect,
      logoutRedirect,
      acquireTokenSilent,
      getActiveAccount
    }
  })
}));

function renderApp(route = '/') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } }
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function requestUrl(input: RequestInfo | URL) {
  return typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
}

describe('App shell', () => {
  beforeEach(() => {
    authenticated = false;
    adminSession = false;
    readonlySession = false;
    managerPermissions = ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'MANAGE_ASSIGNMENTS'];
    personasSuiteCreated = false;
    secondCycleCreated = false;
    accounts = [];
    loginRedirect.mockReset();
    logoutRedirect.mockReset();
    acquireTokenSilent.mockReset();
    getActiveAccount.mockReset();
    vi.unstubAllGlobals();
    vi.stubGlobal(
      'fetch',
      vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
        const path =
          typeof input === 'string' ? input : input instanceof URL ? input.pathname : input.url;
        const pathname = path.split('?')[0] ?? path;
        if (path === '/api/v1/auth/me') {
          if (!authenticated) {
            return Promise.resolve({
              ok: false,
              status: 401,
              json: () => Promise.resolve({ title: 'Unauthorized', status: 401 })
            } as Response);
          }
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                userId: '11111111-1111-4111-8111-111111111111',
                tenantId: 'tenant-1',
                objectId: 'object-1',
                firstName: adminSession ? 'Avery' : 'Mina',
                lastName: adminSession ? 'Administrator' : 'Manager',
                contactEmail: adminSession ? 'avery@example.test' : 'mina@example.test',
                globalAdministrator: adminSession,
                roleName: adminSession ? 'Admin' : 'Test Manager',
                principalKey: 'tenant-1:object-1',
                globalCapabilities: readonlySession
                  ? []
                  : adminSession
                    ? allCapabilities
                    : [
                        'PROJECT_MANAGE_USERS',
                        'PROJECT_MANAGE_SUITES',
                        'PROJECT_MANAGE_CYCLES',
                        'REQUIREMENT_APPROVE',
                        'PREDEFINED_CASE_GENERATE'
                      ],
                permissions: adminSession
                  ? ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'APPROVE_REQUIREMENTS', 'MANAGE_ASSIGNMENTS']
                  : readonlySession
                    ? ['VIEW']
                    : managerPermissions,
                projectPermissions: adminSession
                  ? {}
                  : { 'project-1': readonlySession ? ['VIEW'] : managerPermissions }
              })
          } as Response);
        }
        if (path === '/api/v1/projects') {
          if (init?.method === 'POST') {
            return Promise.resolve({
              ok: true,
              json: () =>
                Promise.resolve({
                  id: 'project-2',
                  projectKey: 'AUSTIN_HEALTH',
                  name: 'Austin Health',
                  description: 'Healthcare workforce QA pilot',
                  active: true,
                  suiteCount: 0,
                  cycleCount: 0,
                  userCount: 0
                })
            } as Response);
          }
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                scopeLabel: adminSession ? 'All Projects' : 'My Projects',
                allProjects: adminSession,
                canCreateProject: adminSession,
                globalCapabilities: adminSession
                  ? ['PROJECT_CREATE']
                  : readonlySession
                    ? ['PROJECT_VIEW']
                    : [
                        'PROJECT_VIEW',
                        'PROJECT_MANAGE_USERS',
                        'PROJECT_MANAGE_SUITES',
                        'PROJECT_MANAGE_CYCLES',
                        'REQUIREMENT_APPROVE',
                        'PREDEFINED_CASE_GENERATE'
                      ],
                projects: [
                  {
                    id: 'project-1',
                    projectKey: 'ABC',
                    name: 'Australian Broadcasting Corporation',
                    description: 'Timekeeping',
                    active: true,
                    suiteCount: 3,
                    cycleCount: 2,
                    userCount: 4
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/suites') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                suites: [
                  {
                    id: 'suite-1',
                    suiteKey: 'TIMEKEEPING',
                    name: 'Timekeeping',
                    description: 'Core time capture',
                    active: true,
                    version: 0
                  },
                  {
                    id: 'suite-2',
                    suiteKey: 'INTEGRATION',
                    name: 'Integration',
                    description: 'Integration testing',
                    active: true,
                    version: 0
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/suites/suite-1') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                id: 'suite-1',
                suiteKey: 'TIMEKEEPING',
                name: 'Timekeeping',
                description: 'Updated',
                active: true,
                version: 1
              })
          } as Response);
        }
        if (pathname === '/api/v1/projects/project-1/suite-assignments') {
          if (init?.method === 'POST') {
            personasSuiteCreated = true;
            return Promise.resolve({
              ok: true,
              json: () =>
                Promise.resolve({
                  id: 'assignment-3',
                  projectId: 'project-1',
                  suiteId: 'suite-3',
                  suiteKey: 'PERSONAS',
                  name: 'Personas',
                  description: 'Persona testing',
                  active: true,
                  version: 0,
                  suiteVersion: 0
                })
            } as Response);
          }
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                assignments: [
                  {
                    id: 'assignment-1',
                    projectId: 'project-1',
                    suiteId: 'suite-1',
                    suiteKey: 'TIMEKEEPING',
                    name: 'Timekeeping',
                    description: 'Core time capture',
                    active: true,
                    version: 0,
                    suiteVersion: 0
                  },
                  ...(personasSuiteCreated
                    ? [
                        {
                          id: 'assignment-3',
                          projectId: 'project-1',
                          suiteId: 'suite-3',
                          suiteKey: 'PERSONAS',
                          name: 'Personas',
                          description: 'Persona testing',
                          active: true,
                          version: 0,
                          suiteVersion: 0
                        }
                      ]
                    : [])
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/projects/project-1/cycles') {
          if (init?.method === 'POST') {
            secondCycleCreated = true;
            return Promise.resolve({
              ok: true,
              json: () =>
                Promise.resolve({
                  id: 'cycle-2',
                  projectId: 'project-1',
                  name: 'Cycle 2',
                  startDate: '2026-09-01',
                  endDate: '2026-09-30',
                  description: 'Regression',
                  active: true,
                  version: 0
                })
            } as Response);
          }
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                cycles: [
                  {
                    id: 'cycle-1',
                    projectId: 'project-1',
                    name: 'Cycle 1',
                    startDate: '2026-08-01',
                    endDate: '2026-08-31',
                    description: 'Baseline',
                    active: true,
                    version: 0
                  },
                  ...(secondCycleCreated
                    ? [
                        {
                          id: 'cycle-2',
                          projectId: 'project-1',
                          name: 'Cycle 2',
                          startDate: '2026-09-01',
                          endDate: '2026-09-30',
                          description: 'Regression',
                          active: true,
                          version: 0
                        }
                      ]
                    : [])
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/projects/project-1/memberships') {
          if (init?.method === 'POST') {
            return Promise.resolve({
              ok: true,
              json: () =>
                Promise.resolve({
                  id: 'membership-3',
                  userId: 'user-3',
                  firstName: 'Sam',
                  lastName: 'Taylor',
                  email: 'sam.taylor@example.test',
                  projectRole: 'Test Analyst',
                  membershipStatus: 'ACTIVE',
                  invitationStatus: 'INVITED',
                  entraBound: false
                })
            } as Response);
          }
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                memberships: [
                  {
                    id: 'membership-1',
                    userId: 'user-1',
                    firstName: 'Mina',
                    lastName: 'Manager',
                    email: 'mina.manager@example.test',
                    projectRole: 'Test Manager',
                    membershipStatus: 'ACTIVE',
                    invitationStatus: 'ACCEPTED',
                    entraBound: true
                  },
                  {
                    id: 'membership-2',
                    userId: 'user-2',
                    firstName: 'Alex',
                    lastName: 'Johnson',
                    email: 'alex.johnson@example.test',
                    projectRole: 'Test Lead',
                    membershipStatus: 'ACTIVE',
                    invitationStatus: 'INVITED',
                    entraBound: false
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/projects/project-1') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                project: {
                  id: 'project-1',
                  projectKey: 'ABC',
                  name: 'Australian Broadcasting Corporation',
                  description: 'Timekeeping',
                  active: true,
                  suiteCount: 3,
                  cycleCount: 2,
                  userCount: 4
                },
                capabilities: [
                  'PROJECT_VIEW',
                  'TEST_CASE_CREATE',
                  'TEST_CASE_EDIT',
                  'TEST_CASE_ASSIGN',
                  'TEST_CASE_DELETE_DRAFT'
                ],
                permissions: readonlySession ? ['VIEW'] : managerPermissions,
                memberships: [
                  {
                    id: 'membership-1',
                    userId: 'user-1',
                    firstName: 'Mina',
                    lastName: 'Manager',
                    email: 'mina.manager@example.test',
                    projectRole: 'Test Manager',
                    membershipStatus: 'ACTIVE',
                    invitationStatus: 'ACCEPTED',
                    entraBound: true
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/requirements') {
          if (init?.method === 'POST') {
            return Promise.resolve({
              ok: true,
              json: () =>
                Promise.resolve({
                  id: 'requirement-2',
                  projectId: 'project-1',
                  projectSuiteAssignmentId: 'assignment-1',
                  suiteId: 'suite-1',
                  suiteName: 'Timekeeping',
                  testCycleId: 'cycle-1',
                  cycleName: 'Cycle 1',
                  reqId: 'REQ-002',
                  header: 'Validate overtime',
                  description: 'Validate overtime calculation.',
                  acceptanceCriteria: '',
                  assumptions: '',
                  dependencies: '',
                  status: 'Draft',
                  sourceType: 'MANUAL',
                  createdDate: '2026-07-28T00:00:00Z',
                  approvedAt: null,
                  approvedBy: null,
                  version: 0
                })
            } as Response);
          }
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                requirements: [
                  {
                    id: 'requirement-1',
                    projectId: 'project-1',
                    projectSuiteAssignmentId: 'assignment-1',
                    suiteId: 'suite-1',
                    suiteName: 'Timekeeping',
                    testCycleId: 'cycle-1',
                    cycleName: 'Cycle 1',
                    reqId: 'REQ-001',
                    header: 'Validate clock-in',
                    description: 'Confirm an active employee can clock in.',
                    acceptanceCriteria: 'Clock-in is captured.',
                    assumptions: '',
                    dependencies: '',
                    status: 'Draft',
                    sourceType: 'MANUAL',
                    createdDate: '2026-07-28T00:00:00Z',
                    approvedAt: null,
                    approvedBy: null,
                    version: 0
                  },
                  {
                    id: 'requirement-approved',
                    projectId: 'project-1',
                    projectSuiteAssignmentId: 'assignment-1',
                    suiteId: 'suite-1',
                    suiteName: 'Timekeeping',
                    testCycleId: 'cycle-1',
                    cycleName: 'Cycle 1',
                    reqId: 'REQ-002',
                    header: 'Approved scheduling',
                    description: 'Approved requirement for scheduling validation.',
                    acceptanceCriteria: '',
                    assumptions: '',
                    dependencies: '',
                    status: 'Approved',
                    sourceType: 'MANUAL',
                    createdDate: '2026-07-29T00:00:00Z',
                    approvedAt: '2026-07-29T01:00:00Z',
                    approvedBy: '11111111-1111-4111-8111-111111111111',
                    version: 1
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/test-cases/adhoc') {
          if (init?.method === 'POST') {
            return Promise.resolve({
              ok: true,
              json: () =>
                Promise.resolve({
                  id: 'adhoc-test-case-2',
                  projectId: 'project-1',
                  projectName: 'ABC Payroll Modernisation',
                  projectSuiteAssignmentId: 'assignment-1',
                  suiteId: 'suite-1',
                  suiteName: 'Timekeeping',
                  testCycleId: 'cycle-1',
                  cycleName: 'Cycle 1',
                  requirementId: null,
                  reqId: null,
                  requirementHeader: null,
                  requirementDescription: null,
                  testCaseId: 'TC-002',
                  header: 'Ad hoc payroll export',
                  description: 'Validate export without requirement linkage.',
                  status: 'Draft',
                  sourceType: 'MANUAL_ADHOC',
                  createdDate: '2026-08-07T00:00:00Z',
                  dueDate: null,
                  assigneeMembershipId: null,
                  assigneeName: null,
                  version: 0
                })
            } as Response);
          }
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                testCases: [
                  {
                    id: 'adhoc-test-case-1',
                    projectId: 'project-1',
                    projectName: 'ABC Payroll Modernisation',
                    projectSuiteAssignmentId: 'assignment-1',
                    suiteId: 'suite-1',
                    suiteName: 'Timekeeping',
                    testCycleId: 'cycle-1',
                    cycleName: 'Cycle 1',
                    requirementId: null,
                    reqId: null,
                    requirementHeader: null,
                    requirementDescription: null,
                    testCaseId: 'TC-001',
                    header: 'Ad hoc clock audit',
                    description: 'Validate time audit without requirement linkage.',
                    status: 'Draft',
                    sourceType: 'MANUAL_ADHOC',
                    createdDate: '2026-08-07T00:00:00Z',
                    dueDate: null,
                    assigneeMembershipId: null,
                    assigneeName: null,
                    version: 0
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/test-cases/adhoc:import-csv') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                jobId: 'adhoc-generation-job-1',
                importedCount: 1,
                testCases: []
              })
          } as Response);
        }
        if (pathname === '/api/v1/test-cases/adhoc-test-case-1') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                id: 'adhoc-test-case-1',
                projectId: 'project-1',
                projectName: 'ABC Payroll Modernisation',
                projectSuiteAssignmentId: 'assignment-1',
                suiteId: 'suite-1',
                suiteName: 'Timekeeping',
                testCycleId: 'cycle-1',
                cycleName: 'Cycle 1',
                requirementId: null,
                reqId: null,
                requirementHeader: null,
                requirementDescription: null,
                testCaseId: 'TC-001',
                header: 'Updated ad hoc clock audit',
                description: 'Updated no requirement linkage.',
                status: 'Draft',
                sourceType: 'MANUAL_ADHOC',
                createdDate: '2026-08-07T00:00:00Z',
                dueDate: null,
                assigneeMembershipId: null,
                assigneeName: null,
                version: 1
              })
          } as Response);
        }
        if (pathname === '/api/v1/test-cases') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                testCases: [
                  {
                    id: 'linked-test-case-1',
                    projectId: 'project-1',
                    projectName: 'ABC Payroll Modernisation',
                    projectSuiteAssignmentId: 'assignment-1',
                    suiteId: 'suite-1',
                    suiteName: 'Timekeeping',
                    testCycleId: 'cycle-1',
                    cycleName: 'Cycle 1',
                    requirementId: 'requirement-1',
                    reqId: 'REQ-001',
                    requirementHeader: 'Validate clock-in',
                    requirementDescription: 'Confirm an active employee can clock in.',
                    testCaseId: 'TC-003',
                    header: 'Validate clock-in test',
                    description: 'Confirm employee clock-in is tested.',
                    status: 'Draft',
                    sourceType: 'MANUAL',
                    createdDate: '2026-08-07T00:00:00Z',
                    dueDate: '2026-08-15',
                    assigneeMembershipId: 'membership-1',
                    assigneeName: 'Mina Manager',
                    version: 0
                  },
                  {
                    id: 'adhoc-test-case-1',
                    projectId: 'project-1',
                    projectName: 'ABC Payroll Modernisation',
                    projectSuiteAssignmentId: 'assignment-1',
                    suiteId: 'suite-1',
                    suiteName: 'Timekeeping',
                    testCycleId: 'cycle-1',
                    cycleName: 'Cycle 1',
                    requirementId: null,
                    reqId: null,
                    requirementHeader: null,
                    requirementDescription: null,
                    testCaseId: 'TC-001',
                    header: 'Ad hoc clock audit',
                    description: 'Validate time audit without requirement linkage.',
                    status: 'Draft',
                    sourceType: 'MANUAL_ADHOC',
                    createdDate: '2026-08-07T00:00:00Z',
                    dueDate: null,
                    assigneeMembershipId: null,
                    assigneeName: null,
                    version: 0
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/roles') {
          const savedRole =
            typeof init?.body === 'string'
              ? (JSON.parse(init.body) as {
                  name: string;
                  description: string;
                  permissions: string[];
                  version: number;
                })
              : null;
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve(
                init?.method === 'POST' && savedRole
                  ? {
                      id: 'role-custom',
                      ...savedRole,
                      administratorRole: false
                    }
                  : {
                      roles: [
                        {
                          id: 'role-admin',
                          name: 'Admin',
                          description: 'Full administrative access.',
                          administratorRole: true,
                          permissions: ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'APPROVE_REQUIREMENTS', 'MANAGE_ASSIGNMENTS'],
                          version: 0
                        },
                        {
                          id: 'role-manager',
                          name: 'Test Manager',
                          description: 'Manages testing work and assignments.',
                          administratorRole: false,
                          permissions: ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'APPROVE_REQUIREMENTS', 'MANAGE_ASSIGNMENTS'],
                          version: 0
                        },
                        {
                          id: 'role-tester',
                          name: 'Tester',
                          description: 'Tests the application.',
                          administratorRole: false,
                          permissions: ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE'],
                          version: 0
                        },
                        {
                          id: 'role-viewer',
                          name: 'Viewer',
                          description: 'Read-only access.',
                          administratorRole: false,
                          permissions: ['VIEW'],
                          version: 0
                        }
                      ]
                    }
              )
          } as Response);
        }
        if (pathname === '/api/v1/roles/role-viewer' && init?.method === 'PATCH') {
          if (typeof init.body !== 'string') {
            throw new Error('Expected a JSON role request body.');
          }
          const body = JSON.parse(init.body) as {
            name: string;
            description: string;
            permissions: string[];
            version: number;
          };
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ id: 'role-viewer', ...body, administratorRole: false, version: 1 })
          } as Response);
        }
        if (pathname === '/api/v1/users') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                users: [
                  {
                    id: '11111111-1111-4111-8111-111111111111',
                    firstName: 'Avery',
                    lastName: 'Administrator',
                    email: 'avery@example.test',
                    roleId: 'role-admin',
                    roleName: 'Admin',
                    administratorRole: true,
                    status: 'ACTIVE',
                    projectIds: [],
                    permissions: ['VIEW']
                  },
                  {
                    id: 'user-2',
                    firstName: 'Alex',
                    lastName: 'Analyst',
                    email: 'alex.analyst@example.test',
                    roleId: 'role-viewer',
                    roleName: 'Viewer',
                    administratorRole: false,
                    status: 'ACTIVE',
                    projectIds: ['project-1'],
                    permissions: ['VIEW', 'EDIT']
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/users/user-2' && init?.method === 'PATCH') {
          if (typeof init.body !== 'string') {
            throw new Error('Expected a JSON request body.');
          }
          const body = JSON.parse(init.body) as {
            firstName: string;
            lastName: string;
            email: string;
            roleId: string;
            status: string;
            projectIds: string[];
            newPassword?: string;
            confirmNewPassword?: string;
          };
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                id: 'user-2',
                firstName: body.firstName,
                lastName: body.lastName,
                email: body.email,
                roleId: body.roleId,
                roleName: body.roleId === 'role-manager' ? 'Test Manager' : 'Viewer',
                administratorRole: false,
                status: body.status === 'ACTIVE' ? 'ACTIVE' : 'DISABLED',
                projectIds: body.projectIds,
                permissions: body.roleId === 'role-manager'
                  ? ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE', 'APPROVE_REQUIREMENTS', 'MANAGE_ASSIGNMENTS']
                  : ['VIEW']
              })
          } as Response);
        }
        if (pathname === '/api/v1/requirements/requirement-1' && init?.method === 'PATCH') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                id: 'requirement-1',
                projectId: 'project-1',
                projectSuiteAssignmentId: 'assignment-1',
                suiteId: 'suite-1',
                suiteName: 'Timekeeping',
                testCycleId: 'cycle-1',
                cycleName: 'Cycle 1',
                reqId: 'REQ-001',
                header: 'Updated clock-in',
                description: 'Updated employee clock-in description.',
                acceptanceCriteria: 'Updated acceptance criteria.',
                assumptions: 'Updated assumptions.',
                dependencies: 'Updated dependencies.',
                status: 'Draft',
                sourceType: 'MANUAL',
                createdDate: '2026-07-28T00:00:00Z',
                approvedAt: null,
                approvedBy: null,
                version: 1
              })
          } as Response);
        }
        if (pathname === '/api/v1/requirements/requirement-1:approve') {
          return Promise.resolve({
            ok: true,
            json: () =>
              Promise.resolve({
                id: 'requirement-1',
                projectId: 'project-1',
                projectSuiteAssignmentId: 'assignment-1',
                suiteId: 'suite-1',
                suiteName: 'Timekeeping',
                testCycleId: 'cycle-1',
                cycleName: 'Cycle 1',
                reqId: 'REQ-001',
                header: 'Validate clock-in',
                description: 'Confirm an active employee can clock in.',
                acceptanceCriteria: 'Clock-in is captured.',
                assumptions: '',
                dependencies: '',
                status: 'Approved',
                sourceType: 'MANUAL',
                createdDate: '2026-07-28T00:00:00Z',
                approvedAt: '2026-07-28T01:00:00Z',
                approvedBy: '11111111-1111-4111-8111-111111111111',
                version: 1
              })
          } as Response);
        }
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              status: 'UP',
              check: 'health',
              service: 'ukg-qa-test-management',
              timestamp: new Date().toISOString()
            })
        } as Response);
      })
    );
  });

  it('renders UI-01 with username and password as the protected-route entry point', async () => {
    renderApp();

    expect(await screen.findByRole('heading', { name: /Welcome Back/i })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Smart WFM/i })).toBeInTheDocument();
    expect(screen.getByText(/AI-Powered Workforce Management/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Sign in$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Sign in with SSO/i })).not.toBeInTheDocument();
    expect(screen.getByText(/© 2026 Smart WFM AI Hub/i)).toBeInTheDocument();
  });

  it('renders the Administrator shell with all-project navigation and Create Project', async () => {
    const user = userEvent.setup();
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });

    renderApp('/projects');

    expect(await screen.findByRole('heading', { name: /All Projects/i })).toBeInTheDocument();
    expect(screen.getByText('Avery Administrator')).toBeInTheDocument();
    expect(screen.getByLabelText('Avery Administrator profile')).toHaveTextContent('AA');
    expect(screen.getByRole('button', { name: /Sign out/i })).toBeInTheDocument();
    expect(screen.getByText('Smart QA Assure')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Visit the Smart WFM website/i })).toHaveAttribute(
      'href',
      'https://www.smartwfm.com/'
    );
    expect(screen.getByRole('button', { name: /Create Project/i })).toBeInTheDocument();
    expect(screen.getAllByText('3').length).toBeGreaterThan(0);
    expect(screen.getAllByText('2').length).toBeGreaterThan(0);
    expect(screen.getAllByText('4').length).toBeGreaterThan(0);
    const dashboardGrid = screen.getByRole('table', { name: /Project dashboard grid/i });
    expect(within(dashboardGrid).getByRole('link', { name: /^View$/i })).toHaveAttribute(
      'href',
      '/projects/users?projectId=project-1'
    );
    const navigation = screen.getByRole('navigation', { name: /Primary/i });
    for (const label of [
      'Projects',
      'Test Suites',
      'Test Cycles',
      'Requirements',
      'Test Cases',
      'Users',
      'Through Requirements',
      'Adhoc Test Cases',
      'Pre Defined Test Cases',
      'View / Export',
      'Generate Requirements',
      'Add Manually',
      'Manage Requirements',
      'Roles & Permissions'
    ]) {
      expect(within(navigation).getByText(label)).toBeInTheDocument();
    }
    expect(within(navigation).queryByText('Audit Logs')).not.toBeInTheDocument();
    expect(within(navigation).queryByText('Reports')).not.toBeInTheDocument();
    expect(within(navigation).queryByText('Settings')).not.toBeInTheDocument();
    const navigationLabels = within(navigation)
      .getAllByRole('link')
      .map((link) => link.textContent?.trim());
    expect(navigationLabels.indexOf('Roles & Permissions') + 1).toBe(
      navigationLabels.indexOf('Users')
    );

    await user.click(within(navigation).getByRole('button', { name: /Collapse navigation/i }));
    expect(within(navigation).getByRole('button', { name: /Expand navigation/i })).toBeVisible();
    expect(within(navigation).getByRole('link', { name: 'Projects' })).toBeVisible();
  });

  it('renders the Test Manager shell as My Projects without Create Project', async () => {
    authenticated = true;
    adminSession = false;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });

    renderApp('/projects');

    expect(await screen.findByRole('heading', { name: /My Projects/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Create Project/i })).not.toBeInTheDocument();
    expect(screen.getByText(/Australian Broadcasting Corporation/i)).toBeInTheDocument();
    expect(screen.getByText('Test Manager')).toBeInTheDocument();
    expect(
      within(screen.getByRole('navigation', { name: /Primary/i })).queryByText('Users')
    ).not.toBeInTheDocument();
    expect(
      within(screen.getByRole('navigation', { name: /Primary/i })).queryByText(
        'Roles & Permissions'
      )
    ).not.toBeInTheDocument();
  });

  it('rejects direct Roles & Permissions access for a non-Administrator', async () => {
    authenticated = true;
    adminSession = false;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });

    renderApp('/roles-permissions');

    expect(
      await screen.findByText(/This route is not available for the current session/i)
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /^Roles & Permissions$/i })).not.toBeInTheDocument();
  });

  it('opens the Administrator-only Create User drawer with access controls', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/users');

    expect(await screen.findByRole('heading', { name: /^Users$/i })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Create User/i }));
    expect(screen.getByRole('heading', { name: /Create User/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/First Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Last Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Password/i)).toHaveAttribute('type', 'password');
    expect(screen.getByLabelText(/^Confirm Password/i, { selector: 'input' })).toHaveAttribute(
      'type',
      'password'
    );
    expect(screen.getByRole('combobox', { name: /Role/i })).toBeRequired();
    expect(screen.queryByRole('checkbox', { name: /Manage Assignments/i })).not.toBeInTheDocument();
    expect(screen.getByText(/Permissions are inherited from Viewer/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
  });

  it('creates and edits centrally inherited roles', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/roles-permissions');

    expect(await screen.findByRole('heading', { name: /Roles & Permissions/i })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Viewer Read-only access/i }));
    await user.click(screen.getByRole('checkbox', { name: /^Create$/i }));
    await user.click(screen.getByRole('button', { name: /Save Role/i }));
    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/roles/role-viewer',
        expect.objectContaining({ method: 'PATCH' })
      );
    });

    await user.click(screen.getByRole('button', { name: /New Role/i }));
    fireEvent.change(screen.getByLabelText(/Role Name/i), { target: { value: 'Release Lead' } });
    fireEvent.change(screen.getByLabelText(/Role Description/i), {
      target: { value: 'Coordinates release validation.' }
    });
    await user.click(screen.getByRole('checkbox', { name: /Select All/i }));
    await user.click(screen.getByRole('button', { name: /Save Role/i }));
    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/roles',
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  it('lets an Administrator edit a user from the Users table and refreshes the list', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/users');

    const table = await screen.findByRole('table', { name: /Users table/i });
    expect(within(table).getByRole('columnheader', { name: /Actions/i })).toBeInTheDocument();
    await user.click(within(table).getByRole('button', { name: /Edit Alex Analyst/i }));

    expect(screen.getByRole('heading', { name: /Edit User/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/First Name/i)).toHaveValue('Alex');
    expect(screen.getByLabelText(/Last Name/i)).toHaveValue('Analyst');
    expect(screen.getByLabelText(/^Email/i)).toHaveValue('alex.analyst@example.test');
    expect(screen.getByRole('combobox', { name: /Role/i })).toHaveTextContent('Viewer');
    expect(screen.getByRole('combobox', { name: /Status/i })).toHaveTextContent('Active');
    expect(screen.getByRole('combobox', { name: /Projects/i })).toHaveTextContent(
      'Australian Broadcasting Corporation'
    );
    expect(screen.getByText(/Reset Password \(optional\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^New Password/i)).toHaveValue('');
    expect(screen.getByLabelText(/^Confirm New Password/i)).toHaveValue('');
    expect(screen.queryByRole('checkbox', { name: /^View$/i })).not.toBeInTheDocument();
    expect(screen.getByText(/Permissions are inherited from Viewer/i)).toBeInTheDocument();

    await user.clear(screen.getByLabelText(/First Name/i));
    await user.type(screen.getByLabelText(/First Name/i), 'Alexa');
    await user.click(screen.getByRole('combobox', { name: /Role/i }));
    await user.click(await screen.findByRole('option', { name: 'Test Manager' }));
    expect(screen.getByText(/Permissions are inherited from Test Manager/i)).toBeInTheDocument();
    await user.type(screen.getByLabelText(/^New Password/i), 'Updated1!Password');
    await user.type(screen.getByLabelText(/^Confirm New Password/i), 'Updated1!Password');
    await user.click(screen.getByRole('button', { name: /Save Changes/i }));

    expect(
      await screen.findByText('Alexa Analyst was updated and the password was reset successfully.')
    ).toBeInTheDocument();
    const updateCall = vi
      .mocked(fetch)
      .mock.calls.find(
        ([input, init]) => requestUrl(input) === '/api/v1/users/user-2' && init?.method === 'PATCH'
      );
    expect(updateCall).toBeDefined();
    const updateBody = updateCall?.[1]?.body;
    if (typeof updateBody !== 'string') {
      throw new Error('Expected the update request to contain a JSON body.');
    }
    expect(JSON.parse(updateBody)).toEqual({
      firstName: 'Alexa',
      lastName: 'Analyst',
      email: 'alex.analyst@example.test',
      roleId: 'role-manager',
      status: 'ACTIVE',
      projectIds: ['project-1'],
      newPassword: 'Updated1!Password',
      confirmNewPassword: 'Updated1!Password'
    });
    await waitFor(() => {
      const listCalls = vi
        .mocked(fetch)
        .mock.calls.filter(([input]) => requestUrl(input) === '/api/v1/users');
      expect(listCalls.length).toBeGreaterThan(1);
    });
  }, 10000);

  it('keeps Edit User save disabled when optional reset passwords do not match', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/users');

    const table = await screen.findByRole('table', { name: /Users table/i });
    await user.click(within(table).getByRole('button', { name: /Edit Alex Analyst/i }));
    await user.type(screen.getByLabelText(/^New Password/i), 'Updated1!Password');
    await user.type(screen.getByLabelText(/^Confirm New Password/i), 'Different1!Password');

    expect(screen.getByText('Passwords do not match.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Save Changes/i })).toBeDisabled();
  });

  it('does not expose the Users page or Edit User action to a non-Administrator', async () => {
    authenticated = true;
    adminSession = false;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });

    renderApp('/users');

    expect(
      await screen.findByText(/This route is not available for the current session/i)
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Edit/i })).not.toBeInTheDocument();
  });

  it('renders requirement-management tabs and edits a Draft requirement', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/requirements/view');

    expect(
      await screen.findByRole('heading', { name: /Manage Requirements/i })
    ).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Generate Requirements/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Add Manually/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Manage Requirements/i })).toHaveAttribute(
      'aria-selected',
      'true'
    );
    const standaloneLink = screen.getByRole('link', {
      name: /Open Manage Requirements in a new tab/i
    });
    expect(standaloneLink).toHaveAttribute(
      'href',
      '/requirements/view?standalone=true&projectId=project-1'
    );
    expect(standaloneLink).toHaveAttribute('target', '_blank');
    expect(standaloneLink.closest('[role="tab"]')).toBe(
      screen.getByRole('tab', { name: /Manage Requirements/i })
    );
    const table = await screen.findByRole('table', { name: /Requirements table/i });
    expect(within(table).getByText('REQ-001')).toBeInTheDocument();
    const suiteFilter = screen.getByRole('combobox', { name: /^Test Suite$/i });
    expect(suiteFilter).toHaveTextContent('All Test Suites');
    await user.click(suiteFilter);
    await user.click(await screen.findByRole('option', { name: /^Timekeeping$/i }));
    expect(suiteFilter).toHaveTextContent('Timekeeping');
    await user.click(suiteFilter);
    await user.click(await screen.findByRole('option', { name: /^All Test Suites$/i }));
    expect(suiteFilter).toHaveTextContent('All Test Suites');
    expect(screen.getByRole('combobox', { name: /^Test Cycle$/i })).toHaveTextContent(
      'All Test Cycles'
    );
    expect(screen.getByLabelText(/^Status/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /^Status$/i })).toHaveTextContent('All Statuses');

    await user.click(screen.getByLabelText(/^Status/i));
    await user.click(await screen.findByRole('option', { name: /^Approved$/i }));
    expect(within(table).queryByText('REQ-001')).not.toBeInTheDocument();
    expect(within(table).getByText('REQ-002')).toBeInTheDocument();

    await user.click(screen.getByLabelText(/^Status/i));
    await user.click(await screen.findByRole('option', { name: /^All Statuses$/i }));
    expect(within(table).getByText('REQ-001')).toBeInTheDocument();

    const editButtons = within(table).getAllByRole('button', { name: /^Edit$/i });
    expect(editButtons.length).toBeGreaterThan(0);
    const editButton = editButtons[0];
    if (!editButton) {
      throw new Error('Expected an edit button in the requirements table.');
    }
    await user.click(editButton);
    expect(screen.getByRole('heading', { name: /Edit Requirement/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/ReqID/i)).toBeDisabled();
    expect(screen.getByLabelText(/Acceptance Criteria/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/Assumptions/i)).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Dependencies/i)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/^Header/i), {
      target: { value: 'Updated clock-in' }
    });
    fireEvent.change(screen.getByLabelText(/^Description/i), {
      target: { value: 'Updated employee clock-in description.' }
    });
    fireEvent.change(screen.getByLabelText(/Acceptance Criteria/i), {
      target: { value: 'Updated acceptance criteria.' }
    });
    fireEvent.change(screen.getByLabelText(/Dependencies/i), {
      target: { value: 'Updated dependencies.' }
    });
    await user.click(screen.getByRole('button', { name: /^Save$/i }));

    const updateCall = vi
      .mocked(fetch)
      .mock.calls.find(
        ([url, init]) =>
          url === '/api/v1/requirements/requirement-1?projectId=project-1' &&
          init?.method === 'PATCH'
      );
    expect(updateCall).toBeDefined();
    if (!updateCall) {
      throw new Error('Expected the requirement update request to be sent.');
    }
    const updateInit = updateCall[1];
    expect(new Headers(updateInit?.headers).get('If-Match')).toBe('0');
    expect(typeof updateInit?.body).toBe('string');
    const updateBody = JSON.stringify(JSON.parse(updateInit?.body as string));
    expect(updateBody).toContain('Updated clock-in');
    expect(updateBody).toContain('Updated acceptance criteria.');
    expect(updateBody).toContain('Updated dependencies.');
    expect(updateBody).not.toContain('REQ-001');
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(await screen.findByText(/Requirement updated/i)).toBeInTheDocument();
  });

  it('creates a manual requirement with acceptance criteria and dependencies', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/requirements/add');

    expect(
      await screen.findByRole('heading', { name: /Add Requirement Manually/i })
    ).toBeInTheDocument();
    await user.click(screen.getByRole('combobox', { name: /Test Suite/i }));
    await user.click(await screen.findByRole('option', { name: /Timekeeping/i }));
    await user.click(screen.getByRole('combobox', { name: /Test Cycle/i }));
    await user.click(await screen.findByRole('option', { name: /Cycle 1/i }));
    fireEvent.change(screen.getByLabelText(/^Header/i), {
      target: { value: 'Validate training payment' }
    });
    fireEvent.change(screen.getByLabelText(/^Description/i), {
      target: { value: 'Training time must be paid at ordinary rates.' }
    });
    fireEvent.change(screen.getByLabelText(/Acceptance Criteria/i), {
      target: { value: 'Eligible training time is paid at the ordinary rate.' }
    });
    fireEvent.change(screen.getByLabelText(/Dependencies/i), {
      target: { value: 'An active employee and training record exist.' }
    });
    await user.click(screen.getByRole('button', { name: /Save Draft/i }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/requirements',
        expect.objectContaining({ method: 'POST' })
      );
    });
    const createCall = vi
      .mocked(fetch)
      .mock.calls.find(
        ([url, init]) => url === '/api/v1/requirements' && init?.method === 'POST'
      );
    const createBody = createCall?.[1]?.body;
    if (typeof createBody !== 'string') {
      throw new Error('Expected manual requirement creation to send a JSON string body.');
    }
    expect(JSON.parse(createBody)).toMatchObject({
      acceptanceCriteria: 'Eligible training time is paid at the ordinary rate.',
      dependencies: 'An active employee and training record exist.'
    });
  });

  it('renders the standalone requirement manager with its header but without navigation or tabs', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });

    renderApp('/requirements/view?standalone=true&projectId=project-1');

    const table = await screen.findByRole('table', { name: /Requirements table/i });
    expect(await within(table).findByText('REQ-001')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Visit the Smart WFM website/i })).toBeVisible();
    expect(screen.getAllByRole('img', { name: /^Smart WFM$/i }).length).toBeGreaterThan(0);
    expect(screen.getByText('Test Automation Tool')).toBeVisible();
    expect(screen.queryByRole('navigation', { name: /Primary/i })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Manage Requirements/i })).toBeVisible();
    expect(
      screen.getByText(/Project-scoped requirement list with editing, approval/i)
    ).toBeVisible();
    expect(
      screen.queryByRole('tablist', { name: /Requirement Management tabs/i })
    ).not.toBeInTheDocument();
  });

  it('shows only Approved requirements when creating test cases through requirements', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/test-cases/through-requirements');

    expect(
      await screen.findByRole('heading', { name: /Manage Test Cases Through Requirements/i })
    ).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /^Test Suite$/i })).toHaveTextContent(
      'All test suites'
    );
    expect(screen.getByRole('combobox', { name: /^Test Cycle$/i })).toHaveTextContent(
      'All test cycles'
    );
    await user.click(screen.getByRole('combobox', { name: /^Requirement/i }));
    expect(
      await screen.findByRole('option', { name: /REQ-002 - Approved scheduling/i })
    ).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: /REQ-001 - Validate clock-in/i })).toBeNull();
    await user.keyboard('{Escape}');
  });

  it('posts Create Project only from the Administrator dashboard', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/projects');

    await user.click(await screen.findByRole('button', { name: /Create Project/i }));
    await user.type(screen.getByLabelText(/Project Name/i), 'Austin Health');
    await user.type(screen.getByLabelText(/Project Key/i), 'AUSTIN_HEALTH');
    await user.click(screen.getByRole('button', { name: /Save/i }));

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/projects',
      expect.objectContaining({ method: 'POST' })
    );
  });

  it('renders Manage Project & Users with details and active memberships', async () => {
    authenticated = true;
    adminSession = false;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });

    const user = userEvent.setup();

    renderApp('/projects/users?projectId=project-1');

    expect(
      await screen.findByRole('heading', { name: /Manage Project & Users/i })
    ).toBeInTheDocument();
    expect(screen.getByText(/Project Key: ABC/i)).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: /Assign Users/i }));
    expect(screen.getByRole('row', { name: /Mina Manager/i })).toBeInTheDocument();
    expect(screen.getByText(/alex.johnson@example.test/i)).toBeInTheDocument();
  });

  it('adds a pre-provisioned project user from Assign Users', async () => {
    authenticated = true;
    adminSession = false;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/projects/users?projectId=project-1');
    await user.click(await screen.findByRole('tab', { name: /Assign Users/i }));
    await user.type(screen.getByLabelText(/First Name/i), 'Sam');
    await user.type(screen.getByLabelText(/Last Name/i), 'Taylor');
    await user.type(screen.getByLabelText(/^Email/i), 'sam.taylor@example.test');
    await user.click(screen.getByRole('button', { name: /Add User/i }));

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/projects/project-1/memberships',
      expect.objectContaining({ method: 'POST' })
    );
  });

  it('renders UI-04 suite assignments and assigns a new suite for a Test Manager', async () => {
    authenticated = true;
    adminSession = false;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/test-suites');

    expect(await screen.findByRole('heading', { name: /Manage Test Suites/i })).toBeInTheDocument();
    expect(await screen.findAllByText(/Timekeeping/i)).not.toHaveLength(0);
    await user.clear(screen.getByLabelText(/Suite Name/i));
    await user.type(screen.getByLabelText(/Suite Name/i), 'Personas');
    await user.type(screen.getByLabelText(/^Description/i), 'Persona testing');
    await user.click(screen.getByRole('button', { name: /Assign Suite/i }));

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/projects/project-1/suite-assignments',
      expect.objectContaining({ method: 'POST' })
    );
    await waitFor(
      () => {
        const assignmentReads = vi
          .mocked(fetch)
          .mock.calls.filter(
            ([input, init]) =>
              requestUrl(input) === '/api/v1/projects/project-1/suite-assignments' &&
              init?.method !== 'POST'
          );
        expect(assignmentReads.length).toBeGreaterThan(1);
      },
      { timeout: 3000 }
    );
    const suitesTable = screen.getByRole('table', { name: /Assigned suites table/i });
    expect(await within(suitesTable).findByText('Personas')).toBeInTheDocument();
  });

  it('assigns an existing reusable suite without editing the catalog', async () => {
    authenticated = true;
    adminSession = false;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/test-suites');

    await screen.findByRole('heading', { name: /Manage Test Suites/i });
    await user.click(screen.getByLabelText(/Reusable Suite/i));
    await user.click(await screen.findByRole('option', { name: /Integration/i }));
    await user.click(screen.getByRole('button', { name: /Assign Suite/i }));

    const postCall = vi
      .mocked(fetch)
      .mock.calls.find(
        ([url, init]) =>
          url === '/api/v1/projects/project-1/suite-assignments' && init?.method === 'POST'
      );
    expect(postCall).toBeDefined();
    const postBody = postCall?.[1]?.body;
    expect(typeof postBody).toBe('string');
    expect(JSON.parse(postBody as string)).toMatchObject({
      suiteId: 'suite-2',
      name: 'Integration'
    });
  });

  it('enforces the Test Manager permission set per project on suite actions', async () => {
    authenticated = true;
    adminSession = false;
    managerPermissions = ['VIEW', 'CREATE', 'EDIT', 'EXECUTE', 'DELETE'];
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/test-suites');

    expect(await screen.findByRole('heading', { name: /Manage Test Suites/i })).toBeInTheDocument();
    await user.type(screen.getByLabelText(/Suite Name/i), 'Wooles Regression');
    expect(screen.getByRole('button', { name: /Assign Suite/i })).toBeEnabled();
    expect(await screen.findByRole('button', { name: /^Edit$/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /^Delete$/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /^Unassign$/i })).toBeDisabled();
  });

  it('preserves Administrator full suite access without project permission entries', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/test-suites');

    await user.type(await screen.findByLabelText(/Suite Name/i), 'Administrator Suite');
    expect(screen.getByRole('button', { name: /Assign Suite/i })).toBeEnabled();
    expect(await screen.findByRole('button', { name: /^Edit$/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /^Delete$/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /^Unassign$/i })).toBeEnabled();
  });

  it('renders UI-04 and UI-05 read-only for project members without suite or cycle management', async () => {
    authenticated = true;
    readonlySession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });

    const { unmount } = renderApp('/test-suites');

    expect(await screen.findByRole('heading', { name: /Manage Test Suites/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Assign Suite/i })).toBeDisabled();
    expect(await screen.findAllByText(/Timekeeping/i)).not.toHaveLength(0);

    unmount();
    renderApp('/test-cycles');

    expect(await screen.findByRole('heading', { name: /Manage Test Cycles/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Create Cycle/i })).toBeDisabled();
    expect(await screen.findByText(/Cycle 1/i)).toBeInTheDocument();
  });

  it('renders UI-05 cycles and creates a valid project cycle for a Test Manager', async () => {
    authenticated = true;
    adminSession = false;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/test-cycles');

    expect(await screen.findByRole('heading', { name: /Manage Test Cycles/i })).toBeInTheDocument();
    expect(await screen.findByText(/Cycle 1/i)).toBeInTheDocument();
    await user.type(screen.getByLabelText(/Cycle Name/i), 'Cycle 2');
    await user.type(screen.getByLabelText(/Start Date/i), '2026-09-01');
    await user.type(screen.getByLabelText(/End Date/i), '2026-09-30');
    await user.type(screen.getByLabelText(/^Description/i), 'Regression');
    await user.click(screen.getByRole('button', { name: /Create Cycle/i }));

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/projects/project-1/cycles',
      expect.objectContaining({ method: 'POST' })
    );
    await waitFor(() => {
      const cycleReads = vi
        .mocked(fetch)
        .mock.calls.filter(
          ([input, init]) =>
            requestUrl(input) === '/api/v1/projects/project-1/cycles' && init?.method !== 'POST'
        );
      expect(cycleReads.length).toBeGreaterThan(1);
    });
    const cyclesTable = screen.getByRole('table', { name: /Test cycles table/i });
    expect(await within(cyclesTable).findByText('Cycle 2')).toBeInTheDocument();
  });

  it('renders UI-13 View / Export search and exports only selected test cases', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();
    const createObjectUrl = vi.fn((blob: Blob) => {
      void blob;
      return 'blob:export';
    });
    const revokeObjectUrl = vi.fn();
    const readBlob = (blob: Blob) =>
      new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onerror = () => {
          reject(reader.error ?? new Error('Failed to read export blob.'));
        };
        reader.onload = () => {
          resolve(typeof reader.result === 'string' ? reader.result : '');
        };
        reader.readAsText(blob);
      });
    const downloads: string[] = [];
    const anchorClick = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement
    ) {
      downloads.push(this.download);
    });
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectUrl, configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectUrl, configurable: true });

    renderApp('/test-cases/view-export');

    expect(
      await screen.findByRole('heading', { name: /View \/ Export Test Cases/i })
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Project/i)).toBeInTheDocument();
    await user.click(screen.getByRole('combobox', { name: /Test Suite/i }));
    expect(await screen.findByRole('option', { name: /Timekeeping/i })).toBeInTheDocument();
    await user.keyboard('{Escape}');

    expect(screen.getByRole('button', { name: /Export as PDF/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Export as CSV/i })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: /^Search$/i }));

    expect(await screen.findByText(/Validate clock-in test/i)).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Test Case ID' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Test Case Header' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Description' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'ReqID' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Req Description' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Test Suite' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Test Cycle' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Project' })).toBeInTheDocument();
    expect(screen.getByText(/Confirm an active employee can clock in/i)).toBeInTheDocument();
    expect(screen.getAllByText(/ABC Payroll Modernisation/i).length).toBeGreaterThan(0);
    expect(screen.getByRole('columnheader', { name: 'Status' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Assign To' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Due Date' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'REQ-001' }));
    expect(screen.getByRole('heading', { name: /Requirement REQ-001/i })).toBeInTheDocument();
    expect(screen.getByText('Validate clock-in')).toBeInTheDocument();
    expect(screen.getAllByText(/Confirm an active employee can clock in/i).length).toBeGreaterThan(
      0
    );
    await user.click(screen.getByRole('button', { name: /^Close$/i }));
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    await user.click(screen.getByLabelText(/Select TC-003/i));
    expect(screen.getByRole('button', { name: /Export as PDF/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /Export as CSV/i })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: /Export as CSV/i }));
    await user.click(screen.getByRole('button', { name: /Export as PDF/i }));
    expect(createObjectUrl).toHaveBeenCalledTimes(2);
    expect(anchorClick).toHaveBeenCalledTimes(2);
    expect(downloads).toHaveLength(2);
    expect(downloads[0]).toMatch(
      /^Australian Broadcasting Corporation_TestCases_\d{8}_\d{6}\.csv$/
    );
    expect(downloads[1]).toMatch(
      /^Australian Broadcasting Corporation_TestCases_\d{8}_\d{6}\.pdf$/
    );
    const csvBlob = createObjectUrl.mock.calls.at(0)?.[0];
    const pdfBlob = createObjectUrl.mock.calls.at(1)?.[0];
    if (!csvBlob || !pdfBlob) {
      throw new Error('Expected CSV and PDF export blobs.');
    }
    await expect(readBlob(csvBlob)).resolves.toContain('Req Description');
    await expect(readBlob(csvBlob)).resolves.toContain('ABC Payroll Modernisation');
    const pdfText = await readBlob(pdfBlob);
    expect(pdfText).not.toContain('Selected Test Cases');
    expect(pdfText).toContain('Test Case ID');
    expect(pdfText).toContain('Req Description');
    expect(pdfText).toContain(' re S');
    expect(pdfText).not.toContain('Test Case ID | Test Case Header');

    await user.click(screen.getByRole('button', { name: /^Reset$/i }));
    expect(screen.getByRole('button', { name: /Export as PDF/i })).toBeDisabled();
  });

  it('renders UI-11 ad hoc creation without requirement linkage and uploads CSV to ad hoc endpoint', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();
    const { container } = renderApp('/test-cases/adhoc');

    expect(
      await screen.findByRole('heading', { name: /Manage Adhoc Test Cases/i })
    ).toBeInTheDocument();
    expect(screen.getByText(/not linked to any requirement/i)).toBeInTheDocument();

    await user.click(screen.getByRole('combobox', { name: /Test Suite/i }));
    await user.click(await screen.findByRole('option', { name: /Timekeeping/i }));
    await user.click(screen.getByRole('combobox', { name: /Test Cycle/i }));
    await user.click(await screen.findByRole('option', { name: /Cycle 1/i }));
    expect(await screen.findByText(/Ad hoc clock audit/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Add Manually/i }));
    await user.type(screen.getByLabelText(/Test Case Header/i), 'Ad hoc payroll export');
    await user.type(
      screen.getByLabelText(/Test Case Description/i),
      'Validate export without requirement linkage.'
    );
    await user.click(screen.getByRole('button', { name: /Save Draft/i }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/test-cases/adhoc',
        expect.objectContaining({ method: 'POST' })
      );
    });
    const manualCreateCall = vi
      .mocked(fetch)
      .mock.calls.find(
        ([url, init]) => url === '/api/v1/test-cases/adhoc' && init?.method === 'POST'
      );
    const manualCreateBody = manualCreateCall?.[1]?.body;
    if (typeof manualCreateBody !== 'string') {
      throw new Error('Expected ad hoc manual create to send a JSON string body.');
    }
    expect(manualCreateBody).not.toContain('requirementId');

    const fileInput = container.querySelector('input[type="file"]');
    expect(fileInput).not.toBeNull();
    await user.upload(
      fileInput as HTMLInputElement,
      new File(['Test Case Header,Description\r\nCSV ad hoc,No requirement\r\n'], 'adhoc.csv', {
        type: 'text/csv'
      })
    );

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/test-cases/adhoc:import-csv?projectId=project-1&projectSuiteAssignmentId=assignment-1&testCycleId=cycle-1',
        expect.objectContaining({ method: 'POST' })
      );
    });
  }, 10000);

  it('edits an ad hoc test case without sending a Test Case ID', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/test-cases/adhoc');

    await screen.findByRole('heading', { name: /Manage Adhoc Test Cases/i });
    await user.click(screen.getByRole('combobox', { name: /Test Suite/i }));
    await user.click(await screen.findByRole('option', { name: /Timekeeping/i }));
    await user.click(screen.getByRole('combobox', { name: /Test Cycle/i }));
    await user.click(await screen.findByRole('option', { name: /Cycle 1/i }));
    expect(await screen.findByText(/Ad hoc clock audit/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /^Edit$/i }));
    const headerInput = screen.getByLabelText(/Test Case Header/i);
    await user.clear(headerInput);
    await user.type(headerInput, 'Updated ad hoc clock audit');
    const descriptionInput = screen.getByLabelText(/^Description/i);
    await user.clear(descriptionInput);
    await user.type(descriptionInput, 'Updated no requirement linkage.');
    await user.click(screen.getByRole('button', { name: /^Save$/i }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/test-cases/adhoc-test-case-1?projectId=project-1',
        expect.objectContaining({ method: 'PATCH' })
      );
    });
    const patchCall = vi
      .mocked(fetch)
      .mock.calls.find(
        ([url, init]) =>
          url === '/api/v1/test-cases/adhoc-test-case-1?projectId=project-1' &&
          init?.method === 'PATCH'
      );
    const body = patchCall?.[1]?.body;
    if (typeof body !== 'string') {
      throw new Error('Expected edit to send JSON string body.');
    }
    expect(body).toContain('Updated ad hoc clock audit');
    expect(body).not.toContain('testCaseId');
  }, 10000);

  it('moves focus from the skip link into the main shell', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/projects');
    await screen.findByRole('heading', { name: /All Projects/i });

    const skipLink = screen.getByRole('link', { name: /Skip to main content/i });
    await user.click(skipLink);
    expect(document.querySelector('#main-content')).toHaveFocus();
  });

  it('renders access denied callback state', () => {
    renderApp('/auth/callback?error=access_denied&error_description=Denied');

    expect(screen.getByRole('alert')).toHaveTextContent(/Access was denied/i);
  });
});
