import {
  apiPaths,
  type AddProjectMemberRequest,
  type AdhocSelectionContext,
  type AssignSuiteRequest,
  type AuthSessionResponse,
  type ChangeProjectMemberRoleRequest,
  type CreateAdhocTestCaseRequest,
  type CreateManualRequirementRequest,
  type CreateManualTestCaseRequest,
  type CreateProjectRequest,
  type CreateUserRequest,
  type PredefinedTemplateListResponse,
  type PredefinedTemplateSummary,
  type ProjectDetailResponse,
  type ProjectCycleListResponse,
  type ProjectCycleSummary,
  type ProjectListResponse,
  type ProjectMembershipListResponse,
  type ProjectMembershipSummary,
  type ProjectSuiteAssignmentListResponse,
  type ProjectSuiteAssignmentSummary,
  type SaveCycleRequest,
  type SavePredefinedTemplateRequest,
  type SuiteCatalogResponse,
  type SuiteCatalogSummary,
  type ProjectSummary,
  type RequirementListResponse,
  type RequirementSummary,
  type RequirementSelectionContext,
  type RoleListResponse,
  type RoleSummary,
  type SaveRoleRequest,
  type LocalLoginRequest,
  type TestCaseGenerationResult,
  type TestCaseListResponse,
  type TestCaseSummary,
  type UpdateRequirementRequest,
  type UpdateUserRequest,
  type UpdateTestCaseRequest,
  type UpdateSuiteRequest,
  type SystemStatusResponse,
  type UserListResponse,
  type UserSummary
} from './generated';

export type {
  AdhocSelectionContext,
  AuthSessionResponse,
  Capability,
  CreateAdhocTestCaseRequest,
  CreateProjectRequest,
  CreateManualRequirementRequest,
  CreateManualTestCaseRequest,
  CreateUserRequest,
  PredefinedTemplateListResponse,
  PredefinedTemplateSummary,
  ProjectDetailResponse,
  ProjectCycleListResponse,
  ProjectCycleSummary,
  ProjectListResponse,
  ProjectMembershipListResponse,
  ProjectMembershipSummary,
  ProjectRole,
  ProjectSuiteAssignmentListResponse,
  ProjectSuiteAssignmentSummary,
  SuiteCatalogResponse,
  SuiteCatalogSummary,
  ProjectSummary,
  RequirementListResponse,
  RequirementSummary,
  RequirementSelectionContext,
  RoleListResponse,
  RoleSummary,
  SaveRoleRequest,
  SavePredefinedTemplateRequest,
  LocalLoginRequest,
  SystemStatusResponse,
  TestCaseGenerationResult,
  TestCaseListResponse,
  TestCaseStatus,
  TestCaseSummary,
  UpdateRequirementRequest,
  UpdateUserRequest,
  UpdateTestCaseRequest,
  UserListResponse,
  UserStatus,
  UserSummary,
  AccessPermission
} from './generated';

export interface RequirementGenerationResult {
  jobId: string;
  documentName: string;
  generatedRequirementCount: number;
}

export interface TestCaseFilterContext {
  projectId: string;
  projectSuiteAssignmentId?: string | null;
  testCycleId?: string | null;
  requirementId?: string | null;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set('Accept', 'application/json');

  const response = await fetch(path, {
    ...init,
    headers,
    credentials: init?.credentials ?? 'same-origin'
  });

  if (!response.ok) {
    throw new Error(await responseErrorDetail(response));
  }

  return (await response.json()) as T;
}

async function responseErrorDetail(response: Response): Promise<string> {
  let detail = `Request failed with ${String(response.status)}`;
  try {
    const problem = (await response.json()) as { detail?: unknown; rowErrors?: unknown };
    if (typeof problem.detail === 'string' && problem.detail.trim()) {
      detail = problem.detail;
    }
    if (Array.isArray(problem.rowErrors) && problem.rowErrors.length > 0) {
      const rowDetails = problem.rowErrors
        .filter((item): item is string => typeof item === 'string')
        .join(' ');
      if (rowDetails) {
        detail = `${detail} ${rowDetails}`;
      }
    }
  } catch {
    // Preserve the status-based fallback for non-JSON error responses.
  }
  return detail;
}

async function authorizedRequest<T>(
  path: string,
  accessToken: string | null,
  init?: RequestInit
): Promise<T> {
  const headers = new Headers(init?.headers);
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  return request<T>(path, {
    ...init,
    headers
  });
}

export function getHealth(): Promise<SystemStatusResponse> {
  return request<SystemStatusResponse>(apiPaths.health);
}

export function getReadiness(): Promise<SystemStatusResponse> {
  return request<SystemStatusResponse>(apiPaths.ready);
}

export function localAdminLogin(body: LocalLoginRequest): Promise<AuthSessionResponse> {
  return request<AuthSessionResponse>(apiPaths.authLocalLogin, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function getAuthSession(accessToken: string | null): Promise<AuthSessionResponse> {
  return authorizedRequest<AuthSessionResponse>(apiPaths.authMe, accessToken);
}

export function getUsers(accessToken: string | null): Promise<UserListResponse> {
  return authorizedRequest<UserListResponse>(apiPaths.users, accessToken);
}

export function getRoles(accessToken: string | null): Promise<RoleListResponse> {
  return authorizedRequest<RoleListResponse>(apiPaths.roles, accessToken);
}

export function createRole(
  accessToken: string | null,
  body: SaveRoleRequest
): Promise<RoleSummary> {
  return authorizedRequest<RoleSummary>(apiPaths.roles, accessToken, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function updateRole(
  accessToken: string | null,
  roleId: string,
  body: SaveRoleRequest
): Promise<RoleSummary> {
  return authorizedRequest<RoleSummary>(apiPaths.role(roleId), accessToken, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function createUser(
  accessToken: string | null,
  body: CreateUserRequest
): Promise<UserSummary> {
  return authorizedRequest<UserSummary>(apiPaths.users, accessToken, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function updateUser(
  accessToken: string | null,
  userId: string,
  body: UpdateUserRequest
): Promise<UserSummary> {
  return authorizedRequest<UserSummary>(apiPaths.user(userId), accessToken, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function getProjects(accessToken: string | null): Promise<ProjectListResponse> {
  return authorizedRequest<ProjectListResponse>(apiPaths.projects, accessToken);
}

export function getRequirements(
  accessToken: string | null,
  projectId: string
): Promise<RequirementListResponse> {
  return authorizedRequest<RequirementListResponse>(apiPaths.requirements(projectId), accessToken);
}

export function createManualRequirement(
  accessToken: string | null,
  body: CreateManualRequirementRequest
): Promise<RequirementSummary> {
  return authorizedRequest<RequirementSummary>('/api/v1/requirements', accessToken, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function updateRequirement(
  accessToken: string | null,
  projectId: string,
  requirementId: string,
  version: number,
  body: UpdateRequirementRequest
): Promise<RequirementSummary> {
  return authorizedRequest<RequirementSummary>(
    apiPaths.requirement(projectId, requirementId),
    accessToken,
    {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'If-Match': String(version)
      },
      body: JSON.stringify(body)
    }
  );
}

export function generateRequirementsFromDocument(
  accessToken: string | null,
  document: File,
  context: {
    projectId: string;
    projectSuiteAssignmentId: string;
    testCycleId: string;
  }
): Promise<RequirementGenerationResult> {
  const formData = new FormData();
  formData.append('document', document);
  formData.append('projectId', context.projectId);
  formData.append('projectSuiteAssignmentId', context.projectSuiteAssignmentId);
  formData.append('testCycleId', context.testCycleId);
  return authorizedRequest<RequirementGenerationResult>(apiPaths.generationJobs, accessToken, {
    method: 'POST',
    body: formData
  });
}

export function approveRequirement(
  accessToken: string | null,
  projectId: string,
  requirementId: string,
  version: number
): Promise<RequirementSummary> {
  return authorizedRequest<RequirementSummary>(
    apiPaths.approveRequirement(projectId, requirementId),
    accessToken,
    {
      method: 'POST',
      headers: { 'If-Match': String(version) }
    }
  );
}

export function getTestCases(
  accessToken: string | null,
  context: TestCaseFilterContext
): Promise<TestCaseListResponse> {
  return authorizedRequest<TestCaseListResponse>(
    apiPaths.filteredTestCases(
      context.projectId,
      context.projectSuiteAssignmentId,
      context.testCycleId,
      context.requirementId
    ),
    accessToken
  );
}

export function createManualTestCase(
  accessToken: string | null,
  body: CreateManualTestCaseRequest
): Promise<TestCaseSummary> {
  return authorizedRequest<TestCaseSummary>(apiPaths.createTestCase, accessToken, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function getAdhocTestCases(
  accessToken: string | null,
  context: AdhocSelectionContext
): Promise<TestCaseListResponse> {
  return authorizedRequest<TestCaseListResponse>(
    apiPaths.adhocTestCases(
      context.projectId,
      context.projectSuiteAssignmentId,
      context.testCycleId
    ),
    accessToken
  );
}

export function createAdhocManualTestCase(
  accessToken: string | null,
  body: CreateAdhocTestCaseRequest
): Promise<TestCaseSummary> {
  return authorizedRequest<TestCaseSummary>(apiPaths.createAdhocTestCase, accessToken, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function generateTestCasesFromRequirement(
  accessToken: string | null,
  context: RequirementSelectionContext
): Promise<TestCaseGenerationResult> {
  return authorizedRequest<TestCaseGenerationResult>(apiPaths.generateTestCases, accessToken, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': crypto.randomUUID()
    },
    body: JSON.stringify(context)
  });
}

export function importTestCasesCsv(
  accessToken: string | null,
  context: RequirementSelectionContext,
  csv: File
): Promise<TestCaseGenerationResult> {
  const formData = new FormData();
  formData.append('csv', csv);
  return authorizedRequest<TestCaseGenerationResult>(
    apiPaths.importTestCasesCsv(
      context.projectId,
      context.projectSuiteAssignmentId,
      context.testCycleId,
      context.requirementId
    ),
    accessToken,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: formData
    }
  );
}

export function importAdhocTestCasesCsv(
  accessToken: string | null,
  context: AdhocSelectionContext,
  csv: File
): Promise<TestCaseGenerationResult> {
  const formData = new FormData();
  formData.append('csv', csv);
  return authorizedRequest<TestCaseGenerationResult>(
    apiPaths.importAdhocTestCasesCsv(
      context.projectId,
      context.projectSuiteAssignmentId,
      context.testCycleId
    ),
    accessToken,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: formData
    }
  );
}

export function updateTestCase(
  accessToken: string | null,
  projectId: string,
  testCaseId: string,
  version: number,
  body: UpdateTestCaseRequest
): Promise<TestCaseSummary> {
  return authorizedRequest<TestCaseSummary>(apiPaths.testCase(projectId, testCaseId), accessToken, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'If-Match': String(version)
    },
    body: JSON.stringify(body)
  });
}

export async function deleteTestCase(
  accessToken: string | null,
  projectId: string,
  testCaseId: string,
  version: number
): Promise<void> {
  const response = await fetch(apiPaths.testCase(projectId, testCaseId), {
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      'If-Match': String(version)
    },
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(await responseErrorDetail(response));
  }
}

export async function deleteRequirement(
  accessToken: string | null,
  projectId: string,
  requirementId: string,
  version: number
): Promise<void> {
  const headers = new Headers({
    Accept: 'application/json',
    'If-Match': String(version)
  });
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  const response = await fetch(apiPaths.requirement(projectId, requirementId), {
    method: 'DELETE',
    headers,
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(`Request failed with ${String(response.status)}`);
  }
}

export function getProject(
  accessToken: string | null,
  projectId: string
): Promise<ProjectDetailResponse> {
  return authorizedRequest<ProjectDetailResponse>(apiPaths.project(projectId), accessToken);
}

export function createProject(
  accessToken: string | null,
  body: CreateProjectRequest
): Promise<ProjectSummary> {
  return authorizedRequest<ProjectSummary>(apiPaths.projects, accessToken, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function getProjectMemberships(
  accessToken: string | null,
  projectId: string
): Promise<ProjectMembershipListResponse> {
  return authorizedRequest<ProjectMembershipListResponse>(
    apiPaths.projectMemberships(projectId),
    accessToken
  );
}

export function getSuites(
  accessToken: string | null,
  projectId: string
): Promise<SuiteCatalogResponse> {
  return authorizedRequest<SuiteCatalogResponse>(
    `${apiPaths.suites}?projectId=${encodeURIComponent(projectId)}`,
    accessToken
  );
}

export function getSuiteCatalog(accessToken: string | null): Promise<SuiteCatalogResponse> {
  return authorizedRequest<SuiteCatalogResponse>(apiPaths.suites, accessToken);
}

export function updateSuite(
  accessToken: string | null,
  projectId: string,
  suiteId: string,
  version: number,
  body: UpdateSuiteRequest
): Promise<SuiteCatalogSummary> {
  return authorizedRequest<SuiteCatalogSummary>(apiPaths.suite(suiteId, projectId), accessToken, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'If-Match': String(version)
    },
    body: JSON.stringify(body)
  });
}

export async function deleteSuite(
  accessToken: string | null,
  projectId: string,
  suiteId: string,
  version: number
): Promise<void> {
  const response = await fetch(apiPaths.suite(suiteId, projectId), {
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      'If-Match': String(version)
    },
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(await responseErrorDetail(response));
  }
}

export function getPredefinedTestCaseTemplates(
  accessToken: string | null,
  suiteId: string
): Promise<PredefinedTemplateListResponse> {
  return authorizedRequest<PredefinedTemplateListResponse>(
    apiPaths.predefinedTestCaseTemplates(suiteId),
    accessToken
  );
}

export function createPredefinedTestCaseTemplate(
  accessToken: string | null,
  body: SavePredefinedTemplateRequest
): Promise<PredefinedTemplateSummary> {
  return authorizedRequest<PredefinedTemplateSummary>(
    apiPaths.createPredefinedTestCaseTemplate,
    accessToken,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }
  );
}

export function updatePredefinedTestCaseTemplate(
  accessToken: string | null,
  templateId: string,
  version: number,
  body: SavePredefinedTemplateRequest
): Promise<PredefinedTemplateSummary> {
  return authorizedRequest<PredefinedTemplateSummary>(
    apiPaths.predefinedTestCaseTemplate(templateId),
    accessToken,
    {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'If-Match': String(version)
      },
      body: JSON.stringify(body)
    }
  );
}

export async function deletePredefinedTestCaseTemplate(
  accessToken: string | null,
  templateId: string,
  version: number
): Promise<void> {
  const response = await fetch(apiPaths.predefinedTestCaseTemplate(templateId), {
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      'If-Match': String(version)
    },
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(await responseErrorDetail(response));
  }
}

export function getProjectSuiteAssignments(
  accessToken: string | null,
  projectId: string
): Promise<ProjectSuiteAssignmentListResponse> {
  return authorizedRequest<ProjectSuiteAssignmentListResponse>(
    apiPaths.projectSuiteAssignments(projectId),
    accessToken
  );
}

export function assignSuiteToProject(
  accessToken: string | null,
  projectId: string,
  body: AssignSuiteRequest
): Promise<ProjectSuiteAssignmentSummary> {
  return authorizedRequest<ProjectSuiteAssignmentSummary>(
    apiPaths.projectSuiteAssignments(projectId),
    accessToken,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }
  );
}

export async function unassignSuiteFromProject(
  accessToken: string | null,
  projectId: string,
  assignmentId: string,
  version: number
): Promise<void> {
  const response = await fetch(apiPaths.projectSuiteAssignment(projectId, assignmentId), {
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      'If-Match': String(version)
    },
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(`Request failed with ${String(response.status)}`);
  }
}

export function getProjectCycles(
  accessToken: string | null,
  projectId: string
): Promise<ProjectCycleListResponse> {
  return authorizedRequest<ProjectCycleListResponse>(
    apiPaths.projectCycles(projectId),
    accessToken
  );
}

export function createProjectCycle(
  accessToken: string | null,
  projectId: string,
  body: SaveCycleRequest
): Promise<ProjectCycleSummary> {
  return authorizedRequest<ProjectCycleSummary>(apiPaths.projectCycles(projectId), accessToken, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function updateProjectCycle(
  accessToken: string | null,
  projectId: string,
  cycleId: string,
  version: number,
  body: SaveCycleRequest
): Promise<ProjectCycleSummary> {
  return authorizedRequest<ProjectCycleSummary>(
    apiPaths.projectCycle(projectId, cycleId),
    accessToken,
    {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'If-Match': String(version)
      },
      body: JSON.stringify(body)
    }
  );
}

export async function deleteProjectCycle(
  accessToken: string | null,
  projectId: string,
  cycleId: string,
  version: number
): Promise<void> {
  const response = await fetch(apiPaths.projectCycle(projectId, cycleId), {
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      'If-Match': String(version)
    },
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(await responseErrorDetail(response));
  }
}

export function addProjectMembership(
  accessToken: string | null,
  projectId: string,
  body: AddProjectMemberRequest
): Promise<ProjectMembershipSummary> {
  return authorizedRequest<ProjectMembershipSummary>(
    apiPaths.projectMemberships(projectId),
    accessToken,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }
  );
}

export function changeProjectMembershipRole(
  accessToken: string | null,
  projectId: string,
  membershipId: string,
  body: ChangeProjectMemberRoleRequest
): Promise<ProjectMembershipSummary> {
  return authorizedRequest<ProjectMembershipSummary>(
    apiPaths.projectMembership(projectId, membershipId),
    accessToken,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }
  );
}

export async function disableProjectMembership(
  accessToken: string | null,
  projectId: string,
  membershipId: string,
  allowLastManagerOverride = false
): Promise<void> {
  const response = await fetch(
    `${apiPaths.projectMembership(projectId, membershipId)}?allowLastManagerOverride=${String(
      allowLastManagerOverride
    )}`,
    {
      method: 'DELETE',
      headers: {
        Accept: 'application/json',
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {})
      },
      credentials: 'same-origin'
    }
  );
  if (!response.ok) {
    throw new Error(`Request failed with ${String(response.status)}`);
  }
}

export async function observeLogout(accessToken: string | null): Promise<void> {
  const headers = new Headers();
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  headers.set('Accept', 'application/json');
  const response = await fetch(apiPaths.authLogout, {
    method: 'POST',
    headers,
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(`Request failed with ${String(response.status)}`);
  }
}
