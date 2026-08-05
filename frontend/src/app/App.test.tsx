import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { App } from './App';

const loginRedirect = vi.fn();
const logoutRedirect = vi.fn();
const acquireTokenSilent = vi.fn();
const getActiveAccount = vi.fn();

let authenticated = false;
let accounts: { homeAccountId: string }[] = [];
let adminSession = false;
let readonlySession = false;

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

describe('App shell', () => {
  beforeEach(() => {
    authenticated = false;
    adminSession = false;
    readonlySession = false;
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
                principalKey: 'tenant-1:object-1',
                globalCapabilities: readonlySession
                  ? []
                  : adminSession
                    ? [
                        'PROJECT_CREATE',
                        'PROJECT_MANAGE_USERS',
                        'PROJECT_MANAGE_SUITES',
                        'PROJECT_MANAGE_CYCLES'
                      ]
                    : [
                        'PROJECT_MANAGE_USERS',
                        'PROJECT_MANAGE_SUITES',
                        'PROJECT_MANAGE_CYCLES',
                        'REQUIREMENT_APPROVE',
                        'PREDEFINED_CASE_GENERATE'
                      ]
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
                  }
                ]
              })
          } as Response);
        }
        if (pathname === '/api/v1/projects/project-1/cycles') {
          if (init?.method === 'POST') {
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
                  }
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
                    status: 'Draft',
                    sourceType: 'MANUAL',
                    createdDate: '2026-07-28T00:00:00Z',
                    approvedAt: null,
                    approvedBy: null,
                    version: 0
                  }
                ]
              })
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
                    role: 'ADMINISTRATOR',
                    status: 'ACTIVE',
                    projectIds: []
                  }
                ]
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
    expect(
      screen.getByRole('img', { name: /Smart WFM - Tailoring UKG Solutions/i })
    ).toBeInTheDocument();
    expect(screen.getByText(/AI-Powered Workforce Management/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Sign in$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Sign in with SSO/i })).not.toBeInTheDocument();
    expect(screen.getByText(/© 2026 Smart WFM AI Hub/i)).toBeInTheDocument();
  });

  it('renders the Administrator shell with all-project navigation and Create Project', async () => {
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
    expect(screen.getByRole('button', { name: /Create Project/i })).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
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
      'Reports',
      'Users',
      'Settings',
      'Through Requirements',
      'Adhoc Test Cases',
      'Pre Defined Test Cases',
      'View / Export',
      'Generate Requirements',
      'Add Manually',
      'View Requirements'
    ]) {
      expect(within(navigation).getByText(label)).toBeInTheDocument();
    }
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
    expect(screen.getByRole('checkbox', { name: /Manage Assignments/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
  });

  it('renders requirement-management tabs and approves a Draft requirement', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });
    const user = userEvent.setup();

    renderApp('/requirements/view');

    expect(await screen.findByRole('heading', { name: /View Requirements/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Generate Requirements/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Add Manually/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /View Requirements/i })).toHaveAttribute(
      'aria-selected',
      'true'
    );
    const table = await screen.findByRole('table', { name: /Requirements table/i });
    expect(within(table).getByText('REQ-001')).toBeInTheDocument();

    await user.click(within(table).getByRole('button', { name: /Approve/i }));

    const approveCall = vi
      .mocked(fetch)
      .mock.calls.find(
        ([url]) => url === '/api/v1/requirements/requirement-1:approve?projectId=project-1'
      );
    expect(approveCall?.[1]?.method).toBe('POST');
    expect(new Headers(approveCall?.[1]?.headers).get('If-Match')).toBe('0');
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
  });

  it('renders UI-13 View / Export with selectable server-side grid primitives', async () => {
    authenticated = true;
    adminSession = true;
    accounts = [{ homeAccountId: 'home-account' }];
    getActiveAccount.mockReturnValue(accounts[0]);
    acquireTokenSilent.mockResolvedValue({ accessToken: 'token' });

    renderApp('/test-cases/view-export');

    expect(
      await screen.findByRole('heading', { name: /View \/ Export Test Cases/i })
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Project/i)).toBeInTheDocument();
    expect(
      screen.getByRole('table', { name: /View \/ Export Test Cases grid/i })
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Export as PDF/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Export as CSV/i })).toBeInTheDocument();
  });

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
