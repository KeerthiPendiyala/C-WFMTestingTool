import type {
  AuthSessionResponse,
  Capability,
  ProjectListResponse,
  ProjectMembershipSummary
} from '../api/client';

export type ShellRoleFixture = 'administrator' | 'test-manager';

export interface SuiteFixture {
  id: string;
  name: string;
  projectId: string;
}

export interface CycleFixture {
  id: string;
  name: string;
  projectId: string;
  startDate: string;
  endDate: string;
  description: string;
}

export interface ShellFixture {
  role: ShellRoleFixture;
  session: AuthSessionResponse;
  projects: ProjectListResponse;
  memberships: Record<string, ProjectMembershipSummary[]>;
  suites: SuiteFixture[];
  cycles: CycleFixture[];
}

export const allCapabilities: Capability[] = [
  'USER_ACCESS_MANAGE',
  'PROJECT_CREATE',
  'PROJECT_VIEW',
  'PROJECT_MANAGE_USERS',
  'PROJECT_MANAGE_SUITES',
  'PROJECT_MANAGE_CYCLES',
  'REQUIREMENT_CREATE',
  'REQUIREMENT_APPROVE',
  'REQUIREMENT_DELETE_UNLINKED',
  'TEST_CASE_CREATE',
  'TEST_CASE_ASSIGN',
  'TEST_CASE_DELETE_DRAFT',
  'PREDEFINED_CASE_GENERATE',
  'PREDEFINED_CASE_DELETE',
  'TEST_CASE_VIEW_EXPORT',
  'REPORT_VIEW',
  'UPLOAD_ACCESS',
  'GENERATION_JOB_ACCESS',
  'EXPORT_DOWNLOAD',
  'AUDIT_VIEW',
  'EVIDENCE_ACCESS'
];

const managerCapabilities: Capability[] = allCapabilities.filter(
  (capability) => capability !== 'PROJECT_CREATE' && capability !== 'AUDIT_VIEW'
);

const abcProject = {
  id: '4f4092d5-e1bb-4db5-905e-b0420f025e27',
  projectKey: 'ABC',
  name: 'Australian Broadcasting Corporation',
  description: 'Timekeeping, Integration, and Personas validation',
  active: true,
  suiteCount: 3,
  cycleCount: 2,
  userCount: 3
};

const austinProject = {
  id: '226ff5c2-6ffc-4c7d-8a5e-9a39e140cb7e',
  projectKey: 'AUSTIN_HEALTH',
  name: 'Austin Health',
  description: 'Healthcare workforce QA pilot',
  active: true,
  suiteCount: 1,
  cycleCount: 1,
  userCount: 1
};

const suites: SuiteFixture[] = [
  { id: 'suite-timekeeping', name: 'Timekeeping', projectId: abcProject.id },
  { id: 'suite-integration', name: 'Integration', projectId: abcProject.id },
  { id: 'suite-personas', name: 'Personas', projectId: abcProject.id },
  { id: 'suite-austin-timekeeping', name: 'Timekeeping', projectId: austinProject.id }
];

const cycles: CycleFixture[] = [
  {
    id: 'cycle-sprint-1',
    name: 'Cycle 1 - Timekeeping Baseline',
    projectId: abcProject.id,
    startDate: '2026-08-01',
    endDate: '2026-08-31',
    description: 'Initial Timekeeping regression cycle.'
  },
  {
    id: 'cycle-sprint-2',
    name: 'Cycle 2 - Integration Regression',
    projectId: abcProject.id,
    startDate: '2026-09-01',
    endDate: '2026-09-30',
    description: 'Integration regression cycle.'
  },
  {
    id: 'cycle-austin-readiness',
    name: 'Cycle 1 - Personas Smoke',
    projectId: austinProject.id,
    startDate: '2026-08-01',
    endDate: '2026-08-15',
    description: 'Persona smoke testing cycle.'
  }
];

function session(globalAdministrator: boolean, capabilities: Capability[]): AuthSessionResponse {
  return {
    userId: globalAdministrator
      ? '11111111-1111-4111-8111-111111111111'
      : '22222222-2222-4222-8222-222222222222',
    tenantId: 'contoso-tenant',
    objectId: globalAdministrator ? 'admin-object' : 'manager-object',
    firstName: globalAdministrator ? 'Avery' : 'Mina',
    lastName: globalAdministrator ? 'Administrator' : 'Manager',
    contactEmail: globalAdministrator ? 'avery.admin@example.test' : 'mina.manager@example.test',
    globalAdministrator,
    principalKey: globalAdministrator
      ? 'contoso-tenant:admin-object'
      : 'contoso-tenant:manager-object',
    globalCapabilities: capabilities
  };
}

const memberships: Record<string, ProjectMembershipSummary[]> = {
  [abcProject.id]: [
    {
      id: '9a2660c6-2da7-4026-98d9-403010000001',
      userId: '9a2660c6-2da7-4026-98d9-403010000101',
      firstName: 'Mina',
      lastName: 'Manager',
      email: 'mina.manager@example.test',
      projectRole: 'Test Manager',
      membershipStatus: 'ACTIVE',
      invitationStatus: 'ACCEPTED',
      entraBound: true
    },
    {
      id: '9a2660c6-2da7-4026-98d9-403010000002',
      userId: '9a2660c6-2da7-4026-98d9-403010000102',
      firstName: 'Alex',
      lastName: 'Johnson',
      email: 'alex.johnson@example.test',
      projectRole: 'Test Lead',
      membershipStatus: 'ACTIVE',
      invitationStatus: 'INVITED',
      entraBound: false
    },
    {
      id: '9a2660c6-2da7-4026-98d9-403010000003',
      userId: '9a2660c6-2da7-4026-98d9-403010000103',
      firstName: 'Beth',
      lastName: 'Smith',
      email: 'beth.smith@example.test',
      projectRole: 'Test Analyst',
      membershipStatus: 'ACTIVE',
      invitationStatus: 'INVITED',
      entraBound: false
    }
  ],
  [austinProject.id]: [
    {
      id: '9a2660c6-2da7-4026-98d9-403010000004',
      userId: '9a2660c6-2da7-4026-98d9-403010000104',
      firstName: 'Priya',
      lastName: 'Nair',
      email: 'priya.nair@example.test',
      projectRole: 'Test Manager',
      membershipStatus: 'ACTIVE',
      invitationStatus: 'ACCEPTED',
      entraBound: true
    }
  ]
};

export const shellFixtures: Record<ShellRoleFixture, ShellFixture> = {
  administrator: {
    role: 'administrator',
    session: session(true, allCapabilities),
    projects: {
      scopeLabel: 'All Projects',
      allProjects: true,
      canCreateProject: true,
      globalCapabilities: allCapabilities,
      projects: [abcProject, austinProject]
    },
    memberships,
    suites,
    cycles
  },
  'test-manager': {
    role: 'test-manager',
    session: session(false, managerCapabilities),
    projects: {
      scopeLabel: 'My Projects',
      allProjects: false,
      canCreateProject: false,
      globalCapabilities: managerCapabilities,
      projects: [abcProject]
    },
    memberships: {
      [abcProject.id]: memberships[abcProject.id] ?? []
    },
    suites: suites.filter((suite) => suite.projectId === abcProject.id),
    cycles: cycles.filter((cycle) => cycle.projectId === abcProject.id)
  }
};

export function fixtureFromSearch(search: string): ShellFixture | null {
  if (import.meta.env.VITE_ENABLE_SHELL_FIXTURES !== 'true') {
    return null;
  }
  const requested = new URLSearchParams(search).get('shellRole');
  if (requested === 'administrator' || requested === 'test-manager') {
    return shellFixtures[requested];
  }
  return null;
}
