import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import AddIcon from '@mui/icons-material/Add';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import AutoAwesomeOutlinedIcon from '@mui/icons-material/AutoAwesomeOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import ListAltOutlinedIcon from '@mui/icons-material/ListAltOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import LoginIcon from '@mui/icons-material/Login';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuIcon from '@mui/icons-material/Menu';
import NoteAddOutlinedIcon from '@mui/icons-material/NoteAddOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import RestartAltOutlinedIcon from '@mui/icons-material/RestartAltOutlined';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import SecurityIcon from '@mui/icons-material/Security';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import TableChartOutlinedIcon from '@mui/icons-material/TableChartOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import {
  Alert,
  AppBar,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Container,
  CssBaseline,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  FormControl,
  FormControlLabel,
  FormHelperText,
  FormGroup,
  IconButton,
  InputAdornment,
  InputLabel,
  Link,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  MenuItem,
  Paper,
  Select,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery
} from '@mui/material';
import type { SvgIconComponent } from '@mui/icons-material';
import { ThemeProvider, useTheme } from '@mui/material/styles';
import { useIsAuthenticated, useMsal } from '@azure/msal-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, type ReactNode, useEffect, useMemo, useRef, useState } from 'react';
import {
  Navigate,
  NavLink,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useSearchParams
} from 'react-router-dom';

import {
  addProjectMembership,
  assignSuiteToProject,
  changeProjectMembershipRole,
  approveRequirement,
  createAdhocManualTestCase,
  createManualRequirement,
  createManualTestCase,
  createProjectCycle,
  createProject,
  createUser,
  deleteRequirement,
  deleteTestCase,
  deleteProjectCycle,
  deleteSuite,
  disableProjectMembership,
  generateTestCasesFromRequirement,
  getAdhocTestCases,
  getAuthSession,
  getProject,
  getProjectCycles,
  getProjectMemberships,
  getProjectSuiteAssignments,
  getHealth,
  getProjects,
  getRequirements,
  getSuites,
  getTestCases,
  getUsers,
  generateRequirementsFromDocument,
  importAdhocTestCasesCsv,
  importTestCasesCsv,
  localAdminLogin,
  observeLogout,
  unassignSuiteFromProject,
  updateProjectCycle,
  updateRequirement,
  updateSuite,
  updateTestCase,
  updateUser,
  type AdhocSelectionContext,
  type AuthSessionResponse,
  type AccessPermission,
  type Capability,
  type ProjectCycleSummary,
  type ProjectListResponse,
  type ProjectMembershipSummary,
  type ProjectRole,
  type ProjectSuiteAssignmentSummary,
  type ProjectSummary,
  type RequirementSelectionContext,
  type RequirementSummary,
  type TestCaseStatus,
  type TestCaseSummary,
  type UserRole,
  type UserStatus,
  type UserSummary
} from '../api/client';
import { appName, localAuthEnabled } from '../auth/config';
import { useAccessToken } from '../auth/useAccessToken';
import { appTheme } from '../theme/theme';
import { designTokens } from '../theme/tokens';
import {
  allCapabilities,
  fixtureFromSearch,
  type CycleFixture,
  type SuiteFixture
} from './shellFixtures';
import { LoginBrandPanel } from './LoginBranding';
import { RequirementGenerationPanel } from './RequirementGenerationPanel';

const drawerWidth = designTokens.shell.drawerWidth;

type RouteKey =
  | 'projects'
  | 'project-users'
  | 'test-suites'
  | 'test-cycles'
  | 'requirements'
  | 'requirements-generate'
  | 'requirements-add'
  | 'requirements-view'
  | 'test-cases-through-requirements'
  | 'test-cases-adhoc'
  | 'test-cases-pre-defined'
  | 'test-cases-view-export'
  | 'reports'
  | 'users'
  | 'settings';

interface RouteDefinition {
  key: RouteKey;
  path: string;
  screenId: string;
  title: string;
  description: string;
  required: Capability[];
  icon: SvgIconComponent;
  rows: GridRow[];
}

interface NavItem {
  label: string;
  path: string;
  icon: SvgIconComponent;
  required: Capability[];
  children?: NavItem[];
}

interface ShellData {
  session: AuthSessionResponse;
  projects: ProjectListResponse;
  memberships: Record<string, ProjectMembershipSummary[]>;
  suites: SuiteFixture[];
  cycles: CycleFixture[];
  healthLabel: string;
  fixtureMode: boolean;
  authMode: 'sso' | 'local' | 'fixture';
}

interface GridRow {
  id: string;
  cells: Record<string, ReactNode>;
}

interface GridColumn {
  key: string;
  label: string;
  sortable?: boolean;
}

const routeDefinitions: RouteDefinition[] = [
  {
    key: 'projects',
    path: '/projects',
    screenId: 'UI-02',
    title: 'Project Dashboard',
    description:
      'Project-first workspace with Administrator all-project view and assigned-project view.',
    required: ['PROJECT_VIEW'],
    icon: DashboardOutlinedIcon,
    rows: []
  },
  {
    key: 'project-users',
    path: '/projects/users',
    screenId: 'UI-03',
    title: 'Manage Project & Users',
    description: 'Pre-provisioned users and project-role assignments.',
    required: ['PROJECT_MANAGE_USERS'],
    icon: GroupsOutlinedIcon,
    rows: [
      row('user-alex', {
        name: 'Alex Johnson',
        email: 'alex.johnson@example.test',
        role: 'Test Manager',
        status: statusChip('Invited', 'warning')
      }),
      row('user-beth', {
        name: 'Beth Smith',
        email: 'beth.smith@example.test',
        role: 'Test Lead',
        status: statusChip('Active', 'success')
      })
    ]
  },
  {
    key: 'test-suites',
    path: '/test-suites',
    screenId: 'UI-04',
    title: 'Manage Test Suites',
    description: 'Project-scoped suite assignments.',
    required: ['PROJECT_VIEW'],
    icon: AccountTreeOutlinedIcon,
    rows: [
      row('suite-timekeeping', {
        suite: 'Timekeeping',
        project: 'Australian Broadcasting Corporation',
        status: 'Active'
      }),
      row('suite-integration', {
        suite: 'Integration',
        project: 'Australian Broadcasting Corporation',
        status: 'Active'
      }),
      row('suite-personas', {
        suite: 'Personas',
        project: 'Australian Broadcasting Corporation',
        status: 'Active'
      })
    ]
  },
  {
    key: 'test-cycles',
    path: '/test-cycles',
    screenId: 'UI-05',
    title: 'Manage Test Cycles',
    description: 'Project cycle windows with date range and description.',
    required: ['PROJECT_VIEW'],
    icon: CalendarMonthOutlinedIcon,
    rows: [
      row('cycle-sprint-1', {
        cycle: 'Sprint 1',
        dates: 'May 1-14, 2026',
        project: 'Australian Broadcasting Corporation'
      }),
      row('cycle-sprint-2', {
        cycle: 'Sprint 2',
        dates: 'May 15-28, 2026',
        project: 'Australian Broadcasting Corporation'
      })
    ]
  },
  {
    key: 'requirements',
    path: '/requirements',
    screenId: 'UI-06',
    title: 'Manage Requirements',
    description: 'Requirement Management hub for generation, manual entry, and review.',
    required: ['REQUIREMENT_CREATE'],
    icon: AssignmentOutlinedIcon,
    rows: []
  },
  {
    key: 'requirements-generate',
    path: '/requirements/generate',
    screenId: 'UI-07',
    title: 'Upload Requirement Document / Generate Requirements',
    description: 'Secure PDF, DOCX, DOC, and CSV intake with provider-neutral generation jobs.',
    required: ['REQUIREMENT_CREATE', 'UPLOAD_ACCESS', 'GENERATION_JOB_ACCESS'],
    icon: UploadFileOutlinedIcon,
    rows: [
      row('job-1', {
        file: 'timekeeping-requirements.csv',
        source: 'CSV',
        status: statusChip('RUNNING', 'info')
      })
    ]
  },
  {
    key: 'requirements-add',
    path: '/requirements/add',
    screenId: 'UI-08',
    title: 'Add Requirement Manually',
    description: 'Manual requirement draft entry.',
    required: ['REQUIREMENT_CREATE'],
    icon: NoteAddOutlinedIcon,
    rows: []
  },
  {
    key: 'requirements-view',
    path: '/requirements/view',
    screenId: 'UI-09',
    title: 'Manage Requirements',
    description:
      'Project-scoped requirement list with editing, approval, and deletion policy affordances.',
    required: ['PROJECT_VIEW'],
    icon: FactCheckOutlinedIcon,
    rows: [
      row('req-001', {
        reqId: 'REQ-001',
        header: 'Verify user can clock in',
        suite: 'Timekeeping',
        cycle: 'Sprint 1',
        status: statusChip('Draft', 'default')
      }),
      row('req-002', {
        reqId: 'REQ-002',
        header: 'Validate persona permissions',
        suite: 'Personas',
        cycle: 'Sprint 1',
        status: statusChip('Approved', 'success')
      })
    ]
  },
  {
    key: 'test-cases-through-requirements',
    path: '/test-cases/through-requirements',
    screenId: 'UI-10',
    title: 'Manage Test Cases Through Requirements',
    description: 'Requirement-linked generation, manual entry, and CSV upload entry point.',
    required: ['TEST_CASE_CREATE'],
    icon: ScienceOutlinedIcon,
    rows: [
      row('tc-001', {
        testCaseId: 'TC-001',
        reqId: 'REQ-001',
        header: 'Verify user can clock in',
        status: statusChip('Draft', 'default')
      })
    ]
  },
  {
    key: 'test-cases-adhoc',
    path: '/test-cases/adhoc',
    screenId: 'UI-11',
    title: 'Manage Adhoc Test Cases',
    description: 'Manual or CSV-created cases without a requirement link.',
    required: ['TEST_CASE_CREATE'],
    icon: ListAltOutlinedIcon,
    rows: [
      row('tc-004', {
        testCaseId: 'TC-004',
        reqId: '-',
        header: 'Verify API integration',
        status: statusChip('Draft', 'default')
      })
    ]
  },
  {
    key: 'test-cases-pre-defined',
    path: '/test-cases/pre-defined',
    screenId: 'UI-12',
    title: 'Generate Pre Defined Test Cases',
    description: 'Phase 2 predefined generation by project, suite, and cycle.',
    required: ['PREDEFINED_CASE_GENERATE'],
    icon: AutoAwesomeOutlinedIcon,
    rows: [
      row('template-1', {
        source: 'Timekeeping',
        suite: 'Timekeeping',
        cycle: 'Sprint 1',
        status: statusChip('Draft', 'default')
      })
    ]
  },
  {
    key: 'test-cases-view-export',
    path: '/test-cases/view-export',
    screenId: 'UI-13',
    title: 'View / Export Test Cases',
    description:
      'Project-first grid with dependent filters, pagination, sorting, selection, CSV, and PDF export.',
    required: ['TEST_CASE_VIEW_EXPORT'],
    icon: TableChartOutlinedIcon,
    rows: [
      row('tc-001', {
        testCaseId: 'TC-001',
        reqId: 'REQ-001',
        header: 'Verify user can clock in',
        suite: 'Timekeeping',
        cycle: 'Sprint 1',
        status: statusChip('Draft', 'default'),
        assignee: 'Alex Johnson'
      }),
      row('tc-002', {
        testCaseId: 'TC-002',
        reqId: 'REQ-001',
        header: 'Verify clock-in time recorded',
        suite: 'Timekeeping',
        cycle: 'Sprint 1',
        status: statusChip('Inprogress', 'info'),
        assignee: 'Beth Smith'
      }),
      row('tc-003', {
        testCaseId: 'TC-003',
        reqId: '-',
        header: 'Check accrual statement',
        suite: 'Integration',
        cycle: 'Sprint 2',
        status: statusChip('Retest', 'warning'),
        assignee: 'Carol Williams'
      })
    ]
  },
  {
    key: 'reports',
    path: '/reports',
    screenId: 'REPORT-01',
    title: 'Reports',
    description: 'Project-scoped CSV, PDF, Excel, and Power BI report entry point.',
    required: ['REPORT_VIEW'],
    icon: DescriptionOutlinedIcon,
    rows: []
  },
  {
    key: 'users',
    path: '/users',
    screenId: 'UI-03',
    title: 'Users',
    description: 'Create users, manage access and assign users to projects.',
    required: ['USER_ACCESS_MANAGE'],
    icon: GroupsOutlinedIcon,
    rows: []
  },
  {
    key: 'settings',
    path: '/settings',
    screenId: 'NFR-02',
    title: 'Settings',
    description: 'Security, profile, and provider-adapter settings shell.',
    required: ['AUDIT_VIEW'],
    icon: SettingsOutlinedIcon,
    rows: []
  }
];

const navItems: NavItem[] = [
  { label: 'Projects', path: '/projects', icon: DashboardOutlinedIcon, required: ['PROJECT_VIEW'] },
  {
    label: 'Test Suites',
    path: '/test-suites',
    icon: AccountTreeOutlinedIcon,
    required: ['PROJECT_VIEW']
  },
  {
    label: 'Test Cycles',
    path: '/test-cycles',
    icon: CalendarMonthOutlinedIcon,
    required: ['PROJECT_VIEW']
  },
  {
    label: 'Requirements',
    path: '/requirements',
    icon: AssignmentOutlinedIcon,
    required: ['REQUIREMENT_CREATE'],
    children: [
      {
        label: 'Generate Requirements',
        path: '/requirements/generate',
        icon: UploadFileOutlinedIcon,
        required: ['REQUIREMENT_CREATE']
      },
      {
        label: 'Add Manually',
        path: '/requirements/add',
        icon: NoteAddOutlinedIcon,
        required: ['REQUIREMENT_CREATE']
      },
      {
        label: 'Manage Requirements',
        path: '/requirements/view',
        icon: FactCheckOutlinedIcon,
        required: ['PROJECT_VIEW']
      }
    ]
  },
  {
    label: 'Test Cases',
    path: '/test-cases/view-export',
    icon: ScienceOutlinedIcon,
    required: ['TEST_CASE_VIEW_EXPORT'],
    children: [
      {
        label: 'Through Requirements',
        path: '/test-cases/through-requirements',
        icon: ScienceOutlinedIcon,
        required: ['TEST_CASE_CREATE']
      },
      {
        label: 'Adhoc Test Cases',
        path: '/test-cases/adhoc',
        icon: ListAltOutlinedIcon,
        required: ['TEST_CASE_CREATE']
      },
      {
        label: 'Pre Defined Test Cases',
        path: '/test-cases/pre-defined',
        icon: AutoAwesomeOutlinedIcon,
        required: ['PREDEFINED_CASE_GENERATE']
      },
      {
        label: 'View / Export',
        path: '/test-cases/view-export',
        icon: TableChartOutlinedIcon,
        required: ['TEST_CASE_VIEW_EXPORT']
      }
    ]
  },
  { label: 'Users', path: '/users', icon: GroupsOutlinedIcon, required: ['USER_ACCESS_MANAGE'] }
];

function row(id: string, cells: Record<string, ReactNode>): GridRow {
  return { id, cells };
}

function statusChip(label: string, color: 'default' | 'success' | 'warning' | 'info') {
  return <StatusChip label={label} color={color} />;
}

function useCapabilitySet(data: ShellData) {
  return useMemo(() => {
    if (data.session.globalAdministrator) {
      return new Set(allCapabilities);
    }
    const capabilities = new Set(data.session.globalCapabilities);
    for (const capability of data.projects.globalCapabilities) {
      capabilities.add(capability);
    }
    return capabilities;
  }, [data]);
}

function canAccess(capabilities: Set<Capability>, required: Capability[]) {
  return required.every((capability) => capabilities.has(capability));
}

function hasPermission(data: ShellData, projectId: string, permission: AccessPermission) {
  return (
    data.session.globalAdministrator ||
    (data.session.projectPermissions[projectId] ?? []).includes(permission)
  );
}

function useShellAccess(data: ShellData) {
  const { accounts, instance } = useMsal();
  const account = instance.getActiveAccount() ?? accounts[0] ?? null;
  const { acquireAccessToken } = useAccessToken(instance, account);
  const accountKey = data.authMode === 'local' ? data.session.principalKey : account?.homeAccountId;

  const acquireShellAccessToken = async () => {
    if (data.authMode === 'local') {
      return null;
    }
    const token = await acquireAccessToken();
    if (!token) {
      throw new Error('No access token is available.');
    }
    return token;
  };

  return { accountKey, acquireAccessToken: acquireShellAccessToken };
}

function LoginScreen() {
  const [localUsername, setLocalUsername] = useState('');
  const [localPassword, setLocalPassword] = useState('');
  const [showLocalPassword, setShowLocalPassword] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);
  const localLoginMutation = useMutation({
    mutationFn: () => localAdminLogin({ username: localUsername, password: localPassword }),
    onSuccess: () => {
      setLocalError(null);
      window.location.assign('/projects');
    },
    onError: () => {
      setLocalError('Administrator username or password was not accepted.');
    }
  });

  const submitLocalLogin = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (localLoginMutation.isPending) {
      return;
    }
    setLocalError(null);
    localLoginMutation.mutate();
  };

  return (
    <LoginLayout>
      <Stack spacing={3.25}>
        <Box>
          <Typography
            component="p"
            variant="overline"
            color="primary.dark"
            fontWeight={800}
            letterSpacing="0.12em"
          >
            Smart WFM secure access
          </Typography>
          <Typography component="h1" variant="h4" fontWeight={800} sx={{ mt: 0.5 }}>
            Welcome back
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            Sign in to continue to {appName}.
          </Typography>
        </Box>
        <Paper
          component="form"
          onSubmit={submitLocalLogin}
          variant="outlined"
          aria-label="Smart WFM username and password sign in"
          sx={{
            p: { xs: 2, sm: 2.5 },
            borderColor: 'rgba(50, 170, 152, 0.38)',
            borderRadius: 2.5,
            bgcolor: '#FBFFFD'
          }}
        >
          <Stack spacing={2}>
            {!localAuthEnabled && (
              <Alert severity="info" role="alert">
                Username and password sign-in must be enabled by the application administrator.
              </Alert>
            )}
            {localError && (
              <Alert severity="error" role="alert">
                {localError}
              </Alert>
            )}
            <TextField
              fullWidth
              label="Username"
              name="username"
              autoComplete="username"
              value={localUsername}
              onChange={(event) => {
                setLocalUsername(event.target.value);
              }}
              required
            />
            <TextField
              fullWidth
              label="Password"
              name="password"
              type={showLocalPassword ? 'text' : 'password'}
              autoComplete="current-password"
              value={localPassword}
              onChange={(event) => {
                setLocalPassword(event.target.value);
              }}
              required
              helperText="Enter your Smart WFM account credentials."
              slotProps={{
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label={showLocalPassword ? 'Hide password' : 'Show password'}
                        edge="end"
                        onClick={() => {
                          setShowLocalPassword((visible) => !visible);
                        }}
                        onMouseDown={(event) => {
                          event.preventDefault();
                        }}
                      >
                        {showLocalPassword ? (
                          <VisibilityOffOutlinedIcon />
                        ) : (
                          <VisibilityOutlinedIcon />
                        )}
                      </IconButton>
                    </InputAdornment>
                  )
                }
              }}
            />
            <Button
              fullWidth
              type="submit"
              size="large"
              variant="contained"
              startIcon={
                localLoginMutation.isPending ? (
                  <CircularProgress size={18} color="inherit" />
                ) : (
                  <LoginIcon />
                )
              }
              disabled={
                !localAuthEnabled ||
                localLoginMutation.isPending ||
                localUsername.trim().length === 0 ||
                localPassword.length === 0
              }
              sx={{
                minHeight: 50,
                borderRadius: 2,
                bgcolor: '#32AA98',
                '&:hover': { bgcolor: '#137D72' }
              }}
            >
              {localLoginMutation.isPending ? 'Signing in…' : 'Sign in'}
            </Button>
          </Stack>
        </Paper>
      </Stack>
    </LoginLayout>
  );
}

function LoginLayout({ children }: { children: ReactNode }) {
  return (
    <Box
      component="main"
      id="main-content"
      sx={{
        minHeight: '100vh',
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', md: 'minmax(360px, 0.9fr) minmax(480px, 1.1fr)' },
        bgcolor: '#F4FAF2'
      }}
    >
      <LoginBrandPanel appName={appName} />
      <Stack
        component="section"
        justifyContent="center"
        sx={{
          minWidth: 0,
          px: { xs: 2, sm: 5, lg: 10 },
          py: { xs: 4, sm: 6 },
          bgcolor: '#F4FAF2'
        }}
      >
        <Card
          elevation={0}
          sx={{
            width: '100%',
            maxWidth: 560,
            mx: 'auto',
            border: '1px solid',
            borderColor: 'rgba(50, 170, 152, 0.18)',
            borderRadius: 4,
            boxShadow: '0 24px 60px rgba(19, 125, 114, 0.13)'
          }}
        >
          <CardContent sx={{ p: { xs: 3, sm: 5 } }}>{children}</CardContent>
        </Card>
        <Typography
          component="footer"
          variant="body2"
          color="text.secondary"
          textAlign="center"
          sx={{ mt: 3 }}
        >
          © 2026 Smart WFM AI Hub
        </Typography>
      </Stack>
    </Box>
  );
}

function AuthCallback() {
  const authenticated = useIsAuthenticated();
  const search = new URLSearchParams(useLocation().search);
  const error = search.get('error');
  const description = search.get('error_description') ?? search.get('error_subcode');

  if (authenticated && !error) {
    return <Navigate to="/projects" replace />;
  }

  return (
    <Container component="main" id="main-content" maxWidth="sm" sx={{ py: 6 }}>
      <Stack spacing={2}>
        <Typography component="h1" variant="h5" fontWeight={800}>
          Completing sign in
        </Typography>
        {error ? (
          <>
            <Alert severity="error" role="alert">
              {error === 'access_denied'
                ? 'Access was denied.'
                : 'Microsoft Entra ID returned an authentication error.'}
            </Alert>
            {description && <Typography color="text.secondary">{description}</Typography>}
            <Link href="/">Return to sign in</Link>
          </>
        ) : (
          <Stack direction="row" spacing={2} alignItems="center">
            <CircularProgress size={24} aria-label="Processing sign in" />
            <Typography color="text.secondary">
              Processing the Microsoft Entra ID callback.
            </Typography>
          </Stack>
        )}
      </Stack>
    </Container>
  );
}

function ProtectedRoute() {
  const location = useLocation();
  const fixture = fixtureFromSearch(location.search);
  const authenticated = useIsAuthenticated();

  if (fixture) {
    return (
      <AppShell
        data={{
          session: fixture.session,
          projects: fixture.projects,
          memberships: fixture.memberships,
          suites: fixture.suites,
          cycles: fixture.cycles,
          healthLabel: 'UP',
          fixtureMode: true,
          authMode: 'fixture'
        }}
      />
    );
  }

  if (!authenticated) {
    if (localAuthEnabled) {
      return <LocalAuthenticatedShell />;
    }
    return <LoginScreen />;
  }

  return <AuthenticatedShell />;
}

function LocalAuthenticatedShell() {
  const health = useQuery({ queryKey: ['system-health'], queryFn: getHealth, retry: 1 });
  const session = useQuery({
    queryKey: ['auth-session', 'local'],
    queryFn: () => getAuthSession(null),
    retry: false
  });
  const projects = useQuery({
    queryKey: ['projects', 'local'],
    enabled: Boolean(session.data),
    queryFn: () => getProjects(null),
    retry: false
  });

  if (session.isError) {
    return <LoginScreen />;
  }

  if (session.isLoading || projects.isLoading || !session.data || !projects.data) {
    return <ShellLoading tokenState="idle" />;
  }

  if (projects.isError) {
    return <ShellError />;
  }

  return (
    <AppShell
      data={{
        session: session.data,
        projects: projects.data,
        memberships: {},
        suites: [],
        cycles: [],
        healthLabel: health.data?.status ?? 'checking',
        fixtureMode: false,
        authMode: 'local'
      }}
    />
  );
}

function AuthenticatedShell() {
  const { accounts, instance } = useMsal();
  const account = instance.getActiveAccount() ?? accounts[0] ?? null;
  const { acquireAccessToken, tokenState } = useAccessToken(instance, account);
  const health = useQuery({ queryKey: ['system-health'], queryFn: getHealth, retry: 1 });
  const session = useQuery({
    queryKey: ['auth-session', account?.homeAccountId],
    enabled: Boolean(account),
    queryFn: async () => {
      const token = await acquireAccessToken();
      if (!token) {
        throw new Error('No access token is available.');
      }
      return getAuthSession(token);
    },
    retry: false
  });
  const projects = useQuery({
    queryKey: ['projects', account?.homeAccountId],
    enabled: Boolean(account),
    queryFn: async () => {
      const token = await acquireAccessToken();
      if (!token) {
        throw new Error('No access token is available.');
      }
      return getProjects(token);
    },
    retry: false
  });

  if (session.isLoading || projects.isLoading || !session.data || !projects.data) {
    return <ShellLoading tokenState={tokenState} />;
  }

  if (session.isError || projects.isError) {
    return <ShellError />;
  }

  return (
    <AppShell
      data={{
        session: session.data,
        projects: projects.data,
        memberships: {},
        suites: [],
        cycles: [],
        healthLabel: health.data?.status ?? 'checking',
        fixtureMode: false,
        authMode: 'sso'
      }}
    />
  );
}

function ShellLoading({ tokenState }: { tokenState: string }) {
  return (
    <ShellState
      title="Loading workspace"
      icon={<CircularProgress size={28} aria-label="Loading workspace" />}
    >
      {tokenState === 'claims_challenge' && (
        <Alert severity="warning">
          Additional consent or claims are required. Redirecting to Microsoft Entra ID.
        </Alert>
      )}
      {tokenState === 'expired' && (
        <Alert severity="error">Your session expired. Sign in again to continue.</Alert>
      )}
    </ShellState>
  );
}

function ShellError() {
  return (
    <ShellState title="Workspace unavailable" icon={<ErrorOutlineIcon color="error" />}>
      <Alert severity="error">The authenticated workspace could not be loaded.</Alert>
    </ShellState>
  );
}

function ShellState({
  title,
  icon,
  children
}: {
  title: string;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <Container component="main" id="main-content" maxWidth="sm" sx={{ py: 8 }}>
      <Stack spacing={3} alignItems="flex-start">
        {icon}
        <Typography component="h1" variant="h4" fontWeight={800}>
          {title}
        </Typography>
        {children}
      </Stack>
    </Container>
  );
}

function AppShell({ data }: { data: ShellData }) {
  const theme = useTheme();
  const location = useLocation();
  const compact = useMediaQuery(theme.breakpoints.down('md'));
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [navigationCollapsed, setNavigationCollapsed] = useState(false);
  const mainRef = useRef<HTMLElement | null>(null);
  const capabilities = useCapabilitySet(data);
  const currentDrawerWidth = navigationCollapsed ? 80 : drawerWidth;

  useEffect(() => {
    mainRef.current?.focus();
    setDrawerOpen(false);
  }, [location.pathname]);

  const drawer = (
    <ShellNavigation
      capabilities={capabilities}
      collapsed={!compact && navigationCollapsed}
      collapsible={!compact}
      onToggleCollapsed={() => {
        setNavigationCollapsed((value) => !value);
      }}
      onNavigate={() => {
        setDrawerOpen(false);
      }}
    />
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <Link
        href="#main-content"
        onClick={(event) => {
          event.preventDefault();
          mainRef.current?.focus();
        }}
        sx={{
          position: 'fixed',
          zIndex: theme.zIndex.tooltip,
          left: 12,
          top: 8,
          transform: 'translateY(-150%)',
          '&:focus': { transform: 'translateY(0)' },
          bgcolor: 'background.paper',
          px: 2,
          py: 1,
          borderRadius: 1,
          boxShadow: 2
        }}
      >
        Skip to main content
      </Link>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          borderBottom: `1px solid ${designTokens.color.border}`,
          bgcolor: 'rgba(255, 255, 255, 0.96)',
          backdropFilter: 'blur(10px)',
          ml: { md: `${String(currentDrawerWidth)}px` },
          width: { md: `calc(100% - ${String(currentDrawerWidth)}px)` },
          transition: theme.transitions.create(['margin-left', 'width'], {
            duration: theme.transitions.duration.shorter
          })
        }}
      >
        <Toolbar disableGutters sx={{ minHeight: designTokens.shell.appBarHeight }}>
          {compact && (
            <IconButton
              aria-label="Open navigation"
              onClick={() => {
                setDrawerOpen(true);
              }}
              sx={{ ml: 1 }}
            >
              <MenuIcon />
            </IconButton>
          )}
          <Stack
            direction="row"
            alignItems="center"
            spacing={0.75}
            sx={{ flexGrow: 1, minWidth: 0, px: { xs: 2, sm: 3 } }}
          >
            <Typography
              variant="h6"
              component="div"
              fontWeight={800}
              noWrap
              sx={{ fontSize: { xs: '1rem', sm: '1.3rem' }, color: 'primary.main' }}
            >
              Test Automation Tool
            </Typography>
          </Stack>
          <HeaderAccount data={data} />
        </Toolbar>
      </AppBar>
      <Box component="nav" aria-label="Primary">
        <Drawer
          variant={compact ? 'temporary' : 'permanent'}
          open={compact ? drawerOpen : true}
          onClose={() => {
            setDrawerOpen(false);
          }}
          ModalProps={{ keepMounted: true }}
          sx={{
            width: { md: currentDrawerWidth },
            flexShrink: { md: 0 },
            transition: theme.transitions.create('width', {
              duration: theme.transitions.duration.shorter
            }),
            '& .MuiDrawer-paper': {
              width: compact ? drawerWidth : currentDrawerWidth,
              boxSizing: 'border-box',
              borderRight: `1px solid ${designTokens.color.border}`,
              bgcolor: '#f4f8f6',
              overflowX: 'hidden',
              transition: theme.transitions.create('width', {
                duration: theme.transitions.duration.shorter
              })
            }
          }}
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        id="main-content"
        ref={mainRef}
        tabIndex={-1}
        sx={{
          flexGrow: 1,
          minWidth: 0,
          pt: `${String(designTokens.shell.appBarHeight)}px`,
          px: { xs: 2, sm: 3, lg: 4.5 },
          pb: 5,
          ml: { md: 0 }
        }}
      >
        <Routes>
          <Route path="/" element={<Navigate to="/projects" replace />} />
          {routeDefinitions.map((definition) => (
            <Route
              key={definition.key}
              path={definition.path}
              element={
                <RouteGate definition={definition} data={data} capabilities={capabilities} />
              }
            />
          ))}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Box>
    </Box>
  );
}

function ShellNavigation({
  capabilities,
  onNavigate,
  collapsed,
  collapsible,
  onToggleCollapsed
}: {
  capabilities: Set<Capability>;
  onNavigate: () => void;
  collapsed: boolean;
  collapsible: boolean;
  onToggleCollapsed: () => void;
}) {
  return (
    <Stack sx={{ height: '100%' }}>
      <Stack
        sx={{
          height: designTokens.shell.appBarHeight,
          px: collapsed ? 1.25 : 2.5,
          justifyContent: 'center'
        }}
      >
        <Box
          component="a"
          href="https://www.smartwfm.com/"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="Visit the Smart WFM website"
          sx={{
            width: collapsed ? 54 : 220,
            maxWidth: '100%',
            mx: 'auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: 1,
            '&:focus-visible': {
              outline: '3px solid',
              outlineColor: 'primary.main',
              outlineOffset: 4
            }
          }}
        >
          <Box
            component="img"
            src="/images/smartwfm-logo-official.png"
            alt="Smart WFM"
            sx={{ width: '100%', height: 'auto', display: 'block' }}
          />
        </Box>
        <Typography
          component="span"
          sx={{
            position: 'absolute',
            width: 1,
            height: 1,
            p: 0,
            m: -1,
            overflow: 'hidden',
            clip: 'rect(0 0 0 0)',
            whiteSpace: 'nowrap',
            border: 0
          }}
        >
          Smart QA Assure
        </Typography>
      </Stack>
      <Divider />
      <List
        component="div"
        aria-label="Application routes"
        sx={{ flexGrow: 1, overflowY: 'auto', overflowX: 'hidden', py: 4.5 }}
      >
        {navItems.map((item) => (
          <NavBranch
            key={item.label}
            item={item}
            capabilities={capabilities}
            onNavigate={onNavigate}
            collapsed={collapsed}
            onExpandNavigation={onToggleCollapsed}
          />
        ))}
      </List>
      {collapsible && (
        <Button
          color="inherit"
          aria-label={collapsed ? 'Expand navigation' : 'Collapse navigation'}
          startIcon={collapsed ? undefined : <ChevronLeftIcon />}
          onClick={onToggleCollapsed}
          sx={{
            minWidth: 0,
            justifyContent: collapsed ? 'center' : 'flex-start',
            mx: collapsed ? 1 : 2,
            mb: 2.5,
            px: collapsed ? 1 : 2,
            color: 'text.primary'
          }}
        >
          {collapsed ? <ChevronRightIcon /> : 'Collapse'}
        </Button>
      )}
    </Stack>
  );
}

function NavBranch({
  item,
  capabilities,
  onNavigate,
  collapsed,
  onExpandNavigation
}: {
  item: NavItem;
  capabilities: Set<Capability>;
  onNavigate: () => void;
  collapsed: boolean;
  onExpandNavigation: () => void;
}) {
  const location = useLocation();
  const childActive = Boolean(item.children?.some((child) => location.pathname === child.path));
  const [expanded, setExpanded] = useState(childActive);

  return (
    <>
      <NavEntry
        item={item}
        capabilities={capabilities}
        onNavigate={
          item.children
            ? () => {
                if (collapsed) {
                  setExpanded(true);
                  onExpandNavigation();
                } else {
                  setExpanded((value) => !value);
                }
              }
            : onNavigate
        }
        inset={false}
        expandable={Boolean(item.children)}
        expanded={expanded}
        collapsed={collapsed}
      />
      <Box sx={{ display: expanded && !collapsed ? 'block' : 'none' }}>
        {item.children?.map((child) => (
          <NavEntry
            key={child.label}
            item={child}
            capabilities={capabilities}
            onNavigate={onNavigate}
            inset
            collapsed={collapsed}
          />
        ))}
      </Box>
    </>
  );
}

function NavEntry({
  item,
  capabilities,
  onNavigate,
  inset,
  expandable = false,
  expanded = false,
  collapsed = false
}: {
  item: NavItem;
  capabilities: Set<Capability>;
  onNavigate: () => void;
  inset: boolean;
  expandable?: boolean;
  expanded?: boolean;
  collapsed?: boolean;
}) {
  const Icon = item.icon;
  const allowed = canAccess(capabilities, item.required);
  const button = (
    <ListItemButton
      component={allowed && !expandable ? NavLink : 'div'}
      to={allowed && !expandable ? item.path : undefined}
      onClick={allowed ? onNavigate : undefined}
      aria-disabled={!allowed}
      aria-label={collapsed ? item.label : undefined}
      sx={{
        minHeight: 54,
        mx: collapsed ? 1 : 1.5,
        mb: 0.5,
        borderRadius: 2,
        px: collapsed ? 1.5 : undefined,
        pl: collapsed ? 1.5 : inset ? 4.5 : 2,
        justifyContent: collapsed ? 'center' : 'flex-start',
        color: allowed ? 'text.primary' : 'text.secondary',
        '&.active': {
          bgcolor: designTokens.color.brandSoft,
          color: 'primary.main',
          fontWeight: 800
        }
      }}
    >
      <ListItemIcon
        sx={{ minWidth: collapsed ? 0 : 34, color: 'inherit', justifyContent: 'center' }}
      >
        {allowed ? <Icon fontSize="small" /> : <LockOutlinedIcon fontSize="small" />}
      </ListItemIcon>
      {!collapsed && (
        <ListItemText
          primary={item.label}
          slotProps={{
            primary: {
              fontWeight: inset ? 500 : 700,
              fontSize: inset ? '0.9rem' : '0.95rem'
            }
          }}
        />
      )}
      {!collapsed &&
        expandable &&
        (expanded ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />)}
    </ListItemButton>
  );

  if (!allowed) {
    return <Tooltip title="Not available for this session">{button}</Tooltip>;
  }
  return collapsed ? <Tooltip title={item.label}>{button}</Tooltip> : button;
}

function RouteGate({
  definition,
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
}) {
  if (!canAccess(capabilities, definition.required)) {
    return <ForbiddenPage definition={definition} />;
  }
  if (definition.key === 'projects') {
    return <ProjectsPage data={data} />;
  }
  if (definition.key === 'project-users') {
    return <ProjectUsersPage definition={definition} data={data} />;
  }
  if (definition.key === 'users') {
    return <UsersPage definition={definition} data={data} />;
  }
  if (definition.key === 'test-suites') {
    return <SuiteManagementPage definition={definition} data={data} />;
  }
  if (definition.key === 'test-cycles') {
    return <CycleManagementPage definition={definition} data={data} />;
  }
  if (definition.key.startsWith('requirements')) {
    return (
      <RequirementManagementPage definition={definition} data={data} capabilities={capabilities} />
    );
  }
  if (definition.key === 'test-cases-through-requirements') {
    return (
      <TestCasesThroughRequirementsPage
        definition={definition}
        data={data}
        capabilities={capabilities}
      />
    );
  }
  if (definition.key === 'test-cases-adhoc') {
    return <AdhocTestCasesPage definition={definition} data={data} capabilities={capabilities} />;
  }
  if (definition.key === 'test-cases-view-export') {
    return (
      <ViewExportTestCasesPage definition={definition} data={data} capabilities={capabilities} />
    );
  }
  return <SkeletonPage definition={definition} data={data} capabilities={capabilities} />;
}

function ProjectsPage({ data }: { data: ShellData }) {
  const { acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [projectKey, setProjectKey] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const createProjectMutation = useMutation({
    mutationFn: async () => {
      if (data.fixtureMode) {
        throw new Error('Fixture sessions do not save projects.');
      }
      const token = await acquireAccessToken();
      const payload = {
        name: name.trim(),
        ...(projectKey.trim() ? { projectKey: projectKey.trim() } : {}),
        ...(description.trim() ? { description: description.trim() } : {})
      };
      return createProject(token, payload);
    },
    onSuccess: () => {
      setDialogOpen(false);
      setProjectKey('');
      setName('');
      setDescription('');
      setFormError(null);
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'Project could not be created.');
    }
  });
  const columns: GridColumn[] = [
    { key: 'name', label: 'Project', sortable: true },
    { key: 'projectKey', label: 'Project Key', sortable: true },
    { key: 'description', label: 'Description' },
    { key: 'suites', label: 'Suites' },
    { key: 'cycles', label: 'Cycles' },
    { key: 'users', label: 'Users' },
    { key: 'active', label: 'Status' },
    { key: 'action', label: 'Action' }
  ];
  const rows = data.projects.projects.map((project) =>
    row(project.id, {
      name: (
        <Stack direction="row" spacing={1} alignItems="center">
          <FolderOutlinedIcon color="primary" fontSize="small" />
          <Typography fontWeight={700}>{project.name}</Typography>
        </Stack>
      ),
      projectKey: project.projectKey,
      description: project.description ?? '-',
      suites: project.suiteCount,
      cycles: project.cycleCount,
      users: project.userCount,
      active: statusChip(
        project.active ? 'Active' : 'Disabled',
        project.active ? 'success' : 'default'
      ),
      action: (
        <Button
          component={NavLink}
          to={`/projects/users?projectId=${project.id}`}
          variant="outlined"
          size="small"
        >
          View
        </Button>
      )
    })
  );

  const submitProject = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFormError(null);
    if (data.fixtureMode) {
      setFormError('Fixture sessions do not save projects.');
      return;
    }
    createProjectMutation.mutate();
  };

  return (
    <PageFrame
      screenId="UI-02"
      title={data.projects.scopeLabel}
      description="Project Dashboard"
      action={
        data.projects.canCreateProject ? (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setDialogOpen(true);
            }}
            sx={{ px: 2.25, py: 1.15 }}
          >
            Create Project
          </Button>
        ) : undefined
      }
    >
      <Stack spacing={0.5}>
        <StateCards
          projectCount={data.projects.projects.length}
          userCount={data.projects.projects.reduce(
            (total, project) => total + project.userCount,
            0
          )}
          suiteCount={data.projects.projects.reduce(
            (total, project) => total + project.suiteCount,
            0
          )}
          cycleCount={data.projects.projects.reduce(
            (total, project) => total + project.cycleCount,
            0
          )}
        />
        <ServerDataGrid
          ariaLabel="Project dashboard grid"
          columns={columns}
          rows={rows}
          page={0}
          pageSize={5}
          total={rows.length}
          emptyTitle="No assigned projects"
          title="Projects"
          hidePagination
        />
      </Stack>
      <Dialog
        open={dialogOpen}
        onClose={() => {
          setDialogOpen(false);
        }}
        aria-labelledby="create-project-title"
        fullWidth
        maxWidth="sm"
      >
        <Box component="form" onSubmit={submitProject}>
          <DialogTitle id="create-project-title">Create Project</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ pt: 1 }}>
              {formError && <Alert severity="error">{formError}</Alert>}
              <TextField
                label="Project Key"
                value={projectKey}
                onChange={(event) => {
                  setProjectKey(event.target.value);
                }}
                helperText="Optional. Leave blank to derive from the project name."
                fullWidth
              />
              <TextField
                label="Project Name"
                value={name}
                onChange={(event) => {
                  setName(event.target.value);
                }}
                required
                fullWidth
              />
              <TextField
                label="Description"
                value={description}
                onChange={(event) => {
                  setDescription(event.target.value);
                }}
                multiline
                minRows={3}
                fullWidth
              />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                setDialogOpen(false);
              }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={createProjectMutation.isPending || name.trim().length === 0}
            >
              Save
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </PageFrame>
  );
}

const projectRoleOptions: {
  value: ProjectRole;
  label: ProjectMembershipSummary['projectRole'];
}[] = [
  { value: 'TEST_MANAGER', label: 'Test Manager' },
  { value: 'TEST_LEAD', label: 'Test Lead' },
  { value: 'TEST_ANALYST', label: 'Test Analyst' }
];

const projectRoleByLabel = new Map<ProjectMembershipSummary['projectRole'], ProjectRole>(
  projectRoleOptions.map((option) => [option.label, option.value])
);

const userRoleOptions: { value: UserRole; label: string }[] = [
  { value: 'ADMINISTRATOR', label: 'Administrator' },
  { value: 'TEST_MANAGER', label: 'Test Manager' },
  { value: 'TEST_LEAD', label: 'Test Lead' },
  { value: 'TEST_ANALYST', label: 'Test Analyst' }
];

const accessPermissionOptions: { value: AccessPermission; label: string }[] = [
  { value: 'VIEW', label: 'View' },
  { value: 'CREATE', label: 'Create' },
  { value: 'EDIT', label: 'Edit' },
  { value: 'EXECUTE', label: 'Execute' },
  { value: 'DELETE', label: 'Delete' },
  { value: 'APPROVE_REQUIREMENTS', label: 'Approve Requirements' },
  { value: 'MANAGE_ASSIGNMENTS', label: 'Manage Assignments' }
];

function UsersPage({ definition, data }: { definition: RouteDefinition; data: ShellData }) {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserSummary | null>(null);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [role, setRole] = useState<UserRole>('TEST_ANALYST');
  const [status, setStatus] = useState<UserStatus>('ACTIVE');
  const [projectIds, setProjectIds] = useState<string[]>([]);
  const [suiteAssignmentIds, setSuiteAssignmentIds] = useState<string[]>([]);
  const [testCycleIds, setTestCycleIds] = useState<string[]>([]);
  const [permissions, setPermissions] = useState<AccessPermission[]>(['VIEW']);
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const { accountKey, acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();

  const usersQuery = useQuery({
    queryKey: ['users', accountKey],
    enabled: !data.fixtureMode,
    queryFn: async () => getUsers(await acquireAccessToken()),
    retry: false
  });
  const fixtureUsers: UserSummary[] = [
    {
      id: data.session.userId,
      firstName: data.session.firstName,
      lastName: data.session.lastName,
      email: data.session.contactEmail,
      role: data.session.globalAdministrator ? 'ADMINISTRATOR' : 'TEST_MANAGER',
      status: 'ACTIVE',
      projectIds: data.projects.projects.map((project) => project.id),
      permissions: ['VIEW']
    }
  ];
  const userRows = data.fixtureMode ? fixtureUsers : (usersQuery.data?.users ?? []);

  const assignmentOptionsQuery = useQuery({
    queryKey: ['create-user-assignment-options', projectIds, accountKey],
    enabled: drawerOpen && !editingUser && projectIds.length > 0,
    queryFn: async () => {
      const token = await acquireAccessToken();
      const options = await Promise.all(
        projectIds.map(async (projectId) => {
          const [suiteResponse, cycleResponse] = await Promise.all([
            getProjectSuiteAssignments(token, projectId),
            getProjectCycles(token, projectId)
          ]);
          return { suites: suiteResponse.assignments, cycles: cycleResponse.cycles };
        })
      );
      return {
        suites: options.flatMap((option) => option.suites),
        cycles: options.flatMap((option) => option.cycles)
      };
    },
    retry: false
  });
  const suiteOptions = data.fixtureMode
    ? data.suites
        .filter((suite) => projectIds.includes(suite.projectId))
        .map((suite) => ({ id: suite.id, projectId: suite.projectId, name: suite.name }))
    : (assignmentOptionsQuery.data?.suites ?? []);
  const cycleOptions = data.fixtureMode
    ? data.cycles.filter((cycle) => projectIds.includes(cycle.projectId))
    : (assignmentOptionsQuery.data?.cycles ?? []);

  const resetForm = () => {
    setEditingUser(null);
    setFirstName('');
    setLastName('');
    setEmail('');
    setPassword('');
    setConfirmPassword('');
    setShowPassword(false);
    setShowConfirmPassword(false);
    setRole('TEST_ANALYST');
    setStatus('ACTIVE');
    setProjectIds([]);
    setSuiteAssignmentIds([]);
    setTestCycleIds([]);
    setPermissions(['VIEW']);
    setFormError(null);
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      if (data.fixtureMode) {
        throw new Error('Fixture sessions do not create users.');
      }
      if (password !== confirmPassword) {
        throw new Error('Password and confirmation must match.');
      }
      return createUser(await acquireAccessToken(), {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
        confirmPassword,
        role,
        status,
        projectIds,
        suiteAssignmentIds,
        testCycleIds,
        permissions
      });
    },
    onSuccess: (created) => {
      setDrawerOpen(false);
      setSuccessMessage(`${created.firstName} ${created.lastName} was created successfully.`);
      resetForm();
      void queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'User could not be created.');
    }
  });

  const updateMutation = useMutation({
    mutationFn: async () => {
      if (!editingUser || data.fixtureMode) {
        throw new Error('Fixture sessions do not update users.');
      }
      if (!data.session.globalAdministrator) {
        throw new Error('Administrator access is required to edit users or reset passwords.');
      }
      return updateUser(await acquireAccessToken(), editingUser.id, {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        role,
        status,
        projectIds,
        permissions,
        newPassword: password,
        confirmNewPassword: confirmPassword
      });
    },
    onSuccess: (updated) => {
      setDrawerOpen(false);
      setSuccessMessage(
        password.length > 0 || confirmPassword.length > 0
          ? `${updated.firstName} ${updated.lastName} was updated and the password was reset successfully.`
          : `${updated.firstName} ${updated.lastName} was updated successfully.`
      );
      resetForm();
      void queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'User could not be updated.');
    }
  });

  const passwordValid =
    password.length >= 10 &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /\d/.test(password) &&
    /[^A-Za-z0-9]/.test(password);
  const passwordResetRequested = password.length > 0 || confirmPassword.length > 0;
  const editPasswordValid =
    !passwordResetRequested || (passwordValid && password === confirmPassword);
  const canSubmit =
    firstName.trim().length > 0 &&
    lastName.trim().length > 0 &&
    email.trim().length > 0 &&
    (editingUser !== null ? editPasswordValid : passwordValid && password === confirmPassword) &&
    permissions.includes('VIEW') &&
    (role === 'ADMINISTRATOR' || projectIds.length > 0);

  const openEditDrawer = (user: UserSummary) => {
    setEditingUser(user);
    setFirstName(user.firstName);
    setLastName(user.lastName);
    setEmail(user.email);
    setPassword('');
    setConfirmPassword('');
    setShowPassword(false);
    setShowConfirmPassword(false);
    setRole(user.role);
    setStatus(user.status === 'ACTIVE' ? 'ACTIVE' : 'INACTIVE');
    setProjectIds(
      user.role === 'ADMINISTRATOR'
        ? data.projects.projects.map((project) => project.id)
        : user.projectIds
    );
    setPermissions(user.permissions.length > 0 ? user.permissions : ['VIEW']);
    setSuiteAssignmentIds([]);
    setTestCycleIds([]);
    setFormError(null);
    setDrawerOpen(true);
  };

  const mutationPending = createMutation.isPending || updateMutation.isPending;

  const changeProjects = (nextProjects: string[]) => {
    setProjectIds(nextProjects);
    const projectSet = new Set(nextProjects);
    setSuiteAssignmentIds((current) =>
      current.filter((id) => {
        const option = suiteOptions.find((suite) => suite.id === id);
        return option ? projectSet.has(option.projectId) : false;
      })
    );
    setTestCycleIds((current) =>
      current.filter((id) => {
        const option = cycleOptions.find((cycle) => cycle.id === id);
        return option ? projectSet.has(option.projectId) : false;
      })
    );
  };

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={2.5}>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={2}>
          <Typography color="text.secondary">
            Administrator access is required to create users and manage assignments.
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              resetForm();
              setDrawerOpen(true);
            }}
          >
            Create User
          </Button>
        </Stack>
        {successMessage && (
          <Alert
            severity="success"
            onClose={() => {
              setSuccessMessage(null);
            }}
          >
            {successMessage}
          </Alert>
        )}
        {usersQuery.isError && <Alert severity="error">Users could not be loaded.</Alert>}
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Users table">
            <TableHead>
              <TableRow>
                <TableCell>User</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Projects</TableCell>
                {data.session.globalAdministrator && <TableCell>Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {usersQuery.isLoading && !data.fixtureMode ? (
                <TableRow>
                  <TableCell colSpan={data.session.globalAdministrator ? 6 : 5} align="center">
                    <CircularProgress size={24} aria-label="Loading users" />
                  </TableCell>
                </TableRow>
              ) : (
                userRows.map((user) => (
                  <TableRow key={user.id} hover>
                    <TableCell>
                      <Typography fontWeight={700}>
                        {user.firstName} {user.lastName}
                      </Typography>
                    </TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      {userRoleOptions.find((option) => option.value === user.role)?.label}
                    </TableCell>
                    <TableCell>
                      {statusChip(
                        user.status === 'ACTIVE' ? 'Active' : 'Inactive',
                        user.status === 'ACTIVE' ? 'success' : 'default'
                      )}
                    </TableCell>
                    <TableCell>
                      {user.role === 'ADMINISTRATOR'
                        ? 'All projects'
                        : user.projectIds
                            .map(
                              (id) =>
                                data.projects.projects.find((project) => project.id === id)?.name
                            )
                            .filter(Boolean)
                            .join(', ') || 'None'}
                    </TableCell>
                    {data.session.globalAdministrator && (
                      <TableCell>
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<EditOutlinedIcon />}
                          aria-label={`Edit ${user.firstName} ${user.lastName}`}
                          onClick={() => {
                            openEditDrawer(user);
                          }}
                        >
                          Edit
                        </Button>
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Stack>

      <Drawer
        anchor="right"
        open={drawerOpen}
        onClose={() => {
          if (!mutationPending) {
            setDrawerOpen(false);
            resetForm();
          }
        }}
        PaperProps={{
          component: 'form',
          onSubmit: (event: FormEvent<HTMLFormElement>) => {
            event.preventDefault();
            setFormError(null);
            if (editingUser) {
              updateMutation.mutate();
            } else {
              createMutation.mutate();
            }
          },
          sx: { width: { xs: '100%', sm: 600, md: 680 }, maxWidth: '100%' }
        }}
      >
        <Stack sx={{ height: '100%' }}>
          <Box sx={{ px: { xs: 2, sm: 3 }, py: 2.5 }}>
            <Typography variant="h5" component="h2" fontWeight={800}>
              {editingUser ? 'Edit User' : 'Create User'}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 0.5 }}>
              {editingUser
                ? 'Update profile details, role, status and project access.'
                : 'Add login details, access scope and permissions.'}
            </Typography>
          </Box>
          <Divider />
          <Stack spacing={2.5} sx={{ p: { xs: 2, sm: 3 }, overflowY: 'auto', flexGrow: 1 }}>
            {formError && <Alert severity="error">{formError}</Alert>}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="First Name"
                value={firstName}
                onChange={(event) => {
                  setFirstName(event.target.value);
                }}
                required
                fullWidth
              />
              <TextField
                label="Last Name"
                value={lastName}
                onChange={(event) => {
                  setLastName(event.target.value);
                }}
                required
                fullWidth
              />
            </Stack>
            <TextField
              label="Email"
              type="email"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
              }}
              required
              fullWidth
            />
            {editingUser && (
              <Box>
                <Typography variant="subtitle1" fontWeight={700}>
                  Reset Password (optional)
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Leave both fields blank to keep the existing password unchanged.
                </Typography>
              </Box>
            )}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label={editingUser ? 'New Password' : 'Password'}
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value);
                }}
                error={passwordResetRequested && !passwordValid}
                helperText={
                  editingUser && !passwordResetRequested
                    ? 'Leave blank to keep the existing password.'
                    : '10+ characters with upper, lower, number and special character.'
                }
                required={!editingUser}
                fullWidth
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          aria-label={
                            showPassword
                              ? `Hide ${editingUser ? 'new password' : 'password'}`
                              : `Show ${editingUser ? 'new password' : 'password'}`
                          }
                          onClick={() => {
                            setShowPassword((value) => !value);
                          }}
                          edge="end"
                        >
                          {showPassword ? (
                            <VisibilityOffOutlinedIcon />
                          ) : (
                            <VisibilityOutlinedIcon />
                          )}
                        </IconButton>
                      </InputAdornment>
                    )
                  }
                }}
              />
              <TextField
                label={editingUser ? 'Confirm New Password' : 'Confirm Password'}
                type={showConfirmPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(event) => {
                  setConfirmPassword(event.target.value);
                }}
                error={passwordResetRequested && password !== confirmPassword}
                helperText={
                  passwordResetRequested && password !== confirmPassword
                    ? 'Passwords do not match.'
                    : editingUser
                      ? 'Re-enter the new password.'
                      : 'Re-enter the password.'
                }
                required={!editingUser}
                fullWidth
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          aria-label={
                            showConfirmPassword
                              ? `Hide ${editingUser ? 'confirm new password' : 'confirm password'}`
                              : `Show ${editingUser ? 'confirm new password' : 'confirm password'}`
                          }
                          onClick={() => {
                            setShowConfirmPassword((value) => !value);
                          }}
                          edge="end"
                        >
                          {showConfirmPassword ? (
                            <VisibilityOffOutlinedIcon />
                          ) : (
                            <VisibilityOutlinedIcon />
                          )}
                        </IconButton>
                      </InputAdornment>
                    )
                  }
                }}
              />
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                select
                label="Role"
                value={role}
                onChange={(event) => {
                  const nextRole = event.target.value as UserRole;
                  setRole(nextRole);
                  if (nextRole !== 'TEST_MANAGER') {
                    setPermissions((current) =>
                      current.filter((permission) => permission !== 'APPROVE_REQUIREMENTS')
                    );
                  }
                }}
                required
                fullWidth
              >
                {userRoleOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                select
                label="Status"
                value={status}
                onChange={(event) => {
                  setStatus(event.target.value as UserStatus);
                }}
                required
                fullWidth
              >
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
              </TextField>
            </Stack>
            <FormControl fullWidth required={role !== 'ADMINISTRATOR'}>
              <InputLabel id="create-user-projects-label">Projects</InputLabel>
              <Select
                labelId="create-user-projects-label"
                label="Projects"
                multiple
                value={projectIds}
                onChange={(event) => {
                  changeProjects(event.target.value as string[]);
                }}
                renderValue={(selected) =>
                  selected
                    .map((id) => data.projects.projects.find((project) => project.id === id)?.name)
                    .filter(Boolean)
                    .join(', ')
                }
              >
                {data.projects.projects.map((project) => (
                  <MenuItem key={project.id} value={project.id}>
                    <Checkbox checked={projectIds.includes(project.id)} />
                    {project.name}
                  </MenuItem>
                ))}
              </Select>
              <FormHelperText>
                {editingUser
                  ? 'Select the projects this user can access.'
                  : 'Select projects before choosing suites and cycles.'}
              </FormHelperText>
            </FormControl>
            {!editingUser && (
              <FormControl
                fullWidth
                disabled={projectIds.length === 0 || assignmentOptionsQuery.isLoading}
              >
                <InputLabel id="create-user-suites-label">Test Suites</InputLabel>
                <Select
                  labelId="create-user-suites-label"
                  label="Test Suites"
                  multiple
                  value={suiteAssignmentIds}
                  onChange={(event) => {
                    setSuiteAssignmentIds(event.target.value as string[]);
                  }}
                  renderValue={(selected) =>
                    selected
                      .map((id) => suiteOptions.find((suite) => suite.id === id)?.name)
                      .filter(Boolean)
                      .join(', ')
                  }
                >
                  {suiteOptions.map((suite) => (
                    <MenuItem key={suite.id} value={suite.id}>
                      <Checkbox checked={suiteAssignmentIds.includes(suite.id)} />
                      {suite.name}
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>Only suites from selected projects are available.</FormHelperText>
              </FormControl>
            )}
            {!editingUser && (
              <FormControl
                fullWidth
                disabled={projectIds.length === 0 || assignmentOptionsQuery.isLoading}
              >
                <InputLabel id="create-user-cycles-label">Test Cycles</InputLabel>
                <Select
                  labelId="create-user-cycles-label"
                  label="Test Cycles"
                  multiple
                  value={testCycleIds}
                  onChange={(event) => {
                    setTestCycleIds(event.target.value as string[]);
                  }}
                  renderValue={(selected) =>
                    selected
                      .map((id) => cycleOptions.find((cycle) => cycle.id === id)?.name)
                      .filter(Boolean)
                      .join(', ')
                  }
                >
                  {cycleOptions.map((cycle) => (
                    <MenuItem key={cycle.id} value={cycle.id}>
                      <Checkbox checked={testCycleIds.includes(cycle.id)} />
                      {cycle.name}
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>Only cycles from selected projects are available.</FormHelperText>
              </FormControl>
            )}
            <Box>
              <Typography variant="subtitle1" fontWeight={700}>
                Permissions
              </Typography>
              <FormGroup row sx={{ mt: 0.5, columnGap: 1 }}>
                {accessPermissionOptions.map((option) => (
                  <FormControlLabel
                    key={option.value}
                    control={
                      <Checkbox
                        checked={permissions.includes(option.value)}
                        disabled={
                          option.value === 'VIEW' ||
                          (option.value === 'APPROVE_REQUIREMENTS' && role !== 'TEST_MANAGER')
                        }
                        onChange={(event) => {
                          setPermissions((current) =>
                            event.target.checked
                              ? [...current, option.value]
                              : current.filter((value) => value !== option.value)
                          );
                        }}
                      />
                    }
                    label={option.label}
                  />
                ))}
              </FormGroup>
            </Box>
          </Stack>
          <Divider />
          <Stack
            direction="row"
            justifyContent="flex-end"
            spacing={1.5}
            sx={{ p: { xs: 2, sm: 3 } }}
          >
            <Button
              disabled={mutationPending}
              onClick={() => {
                setDrawerOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={!canSubmit || mutationPending}>
              {editingUser
                ? updateMutation.isPending
                  ? 'Saving…'
                  : 'Save Changes'
                : createMutation.isPending
                  ? 'Creating…'
                  : 'Create User'}
            </Button>
          </Stack>
        </Stack>
      </Drawer>
    </PageFrame>
  );
}

function ProjectUsersPage({ definition, data }: { definition: RouteDefinition; data: ShellData }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialProjectId = searchParams.get('projectId') ?? data.projects.projects[0]?.id ?? '';
  const [projectId, setProjectId] = useState(initialProjectId);
  const [tab, setTab] = useState(0);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [projectRole, setProjectRole] = useState<ProjectRole>('TEST_ANALYST');
  const [allowLastManagerOverride, setAllowLastManagerOverride] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const { accountKey, acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();
  const selectedProject = data.projects.projects.find((project) => project.id === projectId);
  const fixtureMemberships = data.memberships[projectId] ?? [];
  const membershipsQuery = useQuery({
    queryKey: ['project-memberships', projectId, accountKey],
    enabled: Boolean(!data.fixtureMode && projectId),
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectMemberships(token, projectId);
    },
    retry: false
  });
  const memberships = data.fixtureMode
    ? fixtureMemberships
    : (membershipsQuery.data?.memberships ?? []);
  const canManageUsers = hasPermission(data, projectId, 'MANAGE_ASSIGNMENTS');

  const invalidateMemberships = () => {
    void queryClient.invalidateQueries({ queryKey: ['project-memberships', projectId] });
    void queryClient.invalidateQueries({ queryKey: ['projects'] });
  };

  const addMemberMutation = useMutation({
    mutationFn: async () => {
      if (data.fixtureMode) {
        throw new Error('Fixture sessions do not save project users.');
      }
      const duplicate = memberships.some(
        (member) =>
          member.membershipStatus === 'ACTIVE' &&
          member.email.localeCompare(email.trim(), undefined, { sensitivity: 'accent' }) === 0
      );
      if (duplicate) {
        throw new Error('That user already has an active membership on this project.');
      }
      const token = await acquireAccessToken();
      return addProjectMembership(token, projectId, {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        projectRole
      });
    },
    onSuccess: () => {
      setFirstName('');
      setLastName('');
      setEmail('');
      setProjectRole('TEST_ANALYST');
      setActionError(null);
      invalidateMemberships();
    },
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Project user could not be saved.');
    }
  });

  const roleMutation = useMutation({
    mutationFn: async ({
      membershipId,
      nextRole
    }: {
      membershipId: string;
      nextRole: ProjectRole;
    }) => {
      const token = await acquireAccessToken();
      return changeProjectMembershipRole(token, projectId, membershipId, {
        projectRole: nextRole,
        allowLastManagerOverride
      });
    },
    onSuccess: () => {
      setActionError(null);
      invalidateMemberships();
    },
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Project role could not be changed.');
    }
  });

  const disableMutation = useMutation({
    mutationFn: async (membershipId: string) => {
      const token = await acquireAccessToken();
      await disableProjectMembership(token, projectId, membershipId, allowLastManagerOverride);
    },
    onSuccess: () => {
      setActionError(null);
      invalidateMemberships();
    },
    onError: (error) => {
      setActionError(
        error instanceof Error ? error.message : 'Project membership could not be disabled.'
      );
    }
  });

  const submitMember = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setActionError(null);
    addMemberMutation.mutate();
  };

  const changeProject = (nextProjectId: string) => {
    setProjectId(nextProjectId);
    setSearchParams(nextProjectId ? { projectId: nextProjectId } : {});
  };

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={3}>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
            <FormControl fullWidth required>
              <InputLabel id="manage-users-project-label">Project</InputLabel>
              <Select
                labelId="manage-users-project-label"
                label="Project"
                value={projectId}
                onChange={(event) => {
                  changeProject(event.target.value);
                }}
              >
                {data.projects.projects.map((project) => (
                  <MenuItem key={project.id} value={project.id}>
                    {project.name}
                  </MenuItem>
                ))}
              </Select>
              <FormHelperText>Only authorized projects are available.</FormHelperText>
            </FormControl>
            {data.session.globalAdministrator && (
              <Stack direction="row" spacing={1} alignItems="center">
                <Checkbox
                  checked={allowLastManagerOverride}
                  onChange={(event) => {
                    setAllowLastManagerOverride(event.target.checked);
                  }}
                  inputProps={{ 'aria-label': 'Allow last manager override' }}
                />
                <Typography variant="body2">Allow last manager override</Typography>
              </Stack>
            )}
          </Stack>
        </Paper>

        {membershipsQuery.isError && (
          <Alert severity="error">Project memberships could not be loaded.</Alert>
        )}
        {actionError && <Alert severity="error">{actionError}</Alert>}

        <Paper variant="outlined">
          <Tabs
            value={tab}
            onChange={(_, nextTab: number) => {
              setTab(nextTab);
            }}
            aria-label="Manage Project and Users tabs"
            variant="scrollable"
            allowScrollButtonsMobile
          >
            <Tab label="Project Details" />
            <Tab label="Assign Users" />
            <Tab label="Assign Suites" />
            <Tab label="Assign Cycles" />
          </Tabs>
          <Divider />
          <Box sx={{ p: 2 }}>
            {tab === 0 && (
              <ProjectDetailsPanel project={selectedProject} membershipCount={memberships.length} />
            )}
            {tab === 1 && (
              <ProjectMembershipPanel
                canManageUsers={canManageUsers}
                memberships={memberships}
                loading={membershipsQuery.isLoading}
                firstName={firstName}
                lastName={lastName}
                email={email}
                projectRole={projectRole}
                setFirstName={setFirstName}
                setLastName={setLastName}
                setEmail={setEmail}
                setProjectRole={setProjectRole}
                submitMember={submitMember}
                savePending={addMemberMutation.isPending}
                changeRole={(membershipId, nextRole) => {
                  roleMutation.mutate({ membershipId, nextRole });
                }}
                disableMember={(membershipId) => {
                  disableMutation.mutate(membershipId);
                }}
              />
            )}
            {tab === 2 && (
              <UpcomingProjectAssignmentPanel
                title="Assign Suites"
                link="/test-suites"
                label="Open Test Suites"
              />
            )}
            {tab === 3 && (
              <UpcomingProjectAssignmentPanel
                title="Assign Cycles"
                link="/test-cycles"
                label="Open Test Cycles"
              />
            )}
          </Box>
        </Paper>
      </Stack>
    </PageFrame>
  );
}

function ProjectDetailsPanel({
  project,
  membershipCount
}: {
  project: ProjectSummary | undefined;
  membershipCount: number;
}) {
  if (!project) {
    return <Alert severity="info">Select a project to view details.</Alert>;
  }
  return (
    <Stack spacing={2}>
      <Typography variant="h6" component="h2">
        {project.name}
      </Typography>
      <Typography color="text.secondary">
        {project.description ?? 'No description recorded.'}
      </Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} flexWrap="wrap">
        <Chip label={`Project Key: ${project.projectKey}`} variant="outlined" />
        <Chip label={`${String(project.suiteCount)} suites`} variant="outlined" />
        <Chip label={`${String(project.cycleCount)} cycles`} variant="outlined" />
        <Chip label={`${String(membershipCount)} users`} variant="outlined" />
        {statusChip(project.active ? 'Active' : 'Disabled', project.active ? 'success' : 'default')}
      </Stack>
    </Stack>
  );
}

function ProjectMembershipPanel({
  canManageUsers,
  memberships,
  loading,
  firstName,
  lastName,
  email,
  projectRole,
  setFirstName,
  setLastName,
  setEmail,
  setProjectRole,
  submitMember,
  savePending,
  changeRole,
  disableMember
}: {
  canManageUsers: boolean;
  memberships: ProjectMembershipSummary[];
  loading: boolean;
  firstName: string;
  lastName: string;
  email: string;
  projectRole: ProjectRole;
  setFirstName: (value: string) => void;
  setLastName: (value: string) => void;
  setEmail: (value: string) => void;
  setProjectRole: (value: ProjectRole) => void;
  submitMember: (event: FormEvent<HTMLFormElement>) => void;
  savePending: boolean;
  changeRole: (membershipId: string, nextRole: ProjectRole) => void;
  disableMember: (membershipId: string) => void;
}) {
  return (
    <Stack spacing={3}>
      <Box component="form" onSubmit={submitMember}>
        <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
          <TextField
            label="First Name"
            value={firstName}
            onChange={(event) => {
              setFirstName(event.target.value);
            }}
            required
            disabled={!canManageUsers}
            fullWidth
          />
          <TextField
            label="Last Name"
            value={lastName}
            onChange={(event) => {
              setLastName(event.target.value);
            }}
            required
            disabled={!canManageUsers}
            fullWidth
          />
          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={(event) => {
              setEmail(event.target.value);
            }}
            required
            disabled={!canManageUsers}
            fullWidth
          />
          <FormControl fullWidth required disabled={!canManageUsers}>
            <InputLabel id="new-member-role-label">Project Role</InputLabel>
            <Select
              labelId="new-member-role-label"
              label="Project Role"
              value={projectRole}
              onChange={(event) => {
                setProjectRole(event.target.value as ProjectRole);
              }}
            >
              {projectRoleOptions.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            type="submit"
            variant="contained"
            startIcon={<AddIcon />}
            disabled={
              !canManageUsers ||
              savePending ||
              firstName.trim().length === 0 ||
              lastName.trim().length === 0 ||
              email.trim().length === 0
            }
            sx={{ minWidth: 160 }}
          >
            Add User
          </Button>
        </Stack>
      </Box>

      {loading ? (
        <Stack direction="row" spacing={2} alignItems="center">
          <CircularProgress size={22} aria-label="Loading project users" />
          <Typography color="text.secondary">Loading project users</Typography>
        </Stack>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Project users table">
            <TableHead>
              <TableRow>
                <TableCell>User</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Invitation</TableCell>
                <TableCell>Identity</TableCell>
                <TableCell>Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {memberships.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7}>
                    <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                      No project users assigned.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                memberships.map((member) => (
                  <TableRow key={member.id} hover>
                    <TableCell>
                      <Typography fontWeight={700}>
                        {member.firstName} {member.lastName}
                      </Typography>
                    </TableCell>
                    <TableCell>{member.email}</TableCell>
                    <TableCell>
                      <FormControl size="small" fullWidth disabled={!canManageUsers}>
                        <InputLabel id={`member-role-${member.id}`}>Role</InputLabel>
                        <Select
                          labelId={`member-role-${member.id}`}
                          label="Role"
                          value={projectRoleByLabel.get(member.projectRole) ?? 'TEST_ANALYST'}
                          onChange={(event) => {
                            changeRole(member.id, event.target.value as ProjectRole);
                          }}
                        >
                          {projectRoleOptions.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                              {option.label}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </TableCell>
                    <TableCell>
                      {statusChip(
                        member.membershipStatus === 'ACTIVE' ? 'Active' : 'Disabled',
                        member.membershipStatus === 'ACTIVE' ? 'success' : 'default'
                      )}
                    </TableCell>
                    <TableCell>{member.invitationStatus}</TableCell>
                    <TableCell>
                      {member.entraBound ? (
                        <Chip label="Bound" size="small" color="success" />
                      ) : (
                        <Chip label="Invited" size="small" variant="outlined" />
                      )}
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="outlined"
                        size="small"
                        disabled={!canManageUsers || member.membershipStatus !== 'ACTIVE'}
                        onClick={() => {
                          disableMember(member.id);
                        }}
                      >
                        Disable
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Stack>
  );
}

function UpcomingProjectAssignmentPanel({
  title,
  link,
  label
}: {
  title: string;
  link: string;
  label: string;
}) {
  return (
    <Stack spacing={2}>
      <Typography variant="h6" component="h2">
        {title}
      </Typography>
      <Typography color="text.secondary">
        This tab links to the upcoming project assignment screen.
      </Typography>
      <Button component={NavLink} to={link} variant="outlined">
        {label}
      </Button>
    </Stack>
  );
}

function SuiteManagementPage({
  definition,
  data
}: {
  definition: RouteDefinition;
  data: ShellData;
}) {
  const [projectId, setProjectId] = useState(data.projects.projects[0]?.id ?? '');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedSuiteId, setSelectedSuiteId] = useState('');
  const [editing, setEditing] = useState<ProjectSuiteAssignmentSummary | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const { accountKey, acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();
  const canCreate = hasPermission(data, projectId, 'CREATE');
  const canEdit = hasPermission(data, projectId, 'EDIT');
  const canDelete = hasPermission(data, projectId, 'DELETE');
  const canManageAssignments = hasPermission(data, projectId, 'MANAGE_ASSIGNMENTS');
  const canSave = editing ? canEdit : canCreate;
  const fixtureAssignments = data.suites
    .filter((suite) => suite.projectId === projectId)
    .map((suite) => ({
      id: suite.id,
      projectId: suite.projectId,
      suiteId: suite.id,
      suiteKey: suite.name.toUpperCase().replace(/\s+/g, '_'),
      name: suite.name,
      description: 'Development seed suite',
      active: true,
      version: 0,
      suiteVersion: 0
    }));

  const suiteQuery = useQuery({
    queryKey: ['suites', projectId, accountKey],
    enabled: Boolean(!data.fixtureMode && projectId),
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getSuites(token, projectId);
    },
    retry: false
  });
  const assignmentQuery = useQuery({
    queryKey: ['project-suite-assignments', projectId, accountKey],
    enabled: Boolean(!data.fixtureMode && projectId),
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectSuiteAssignments(token, projectId);
    },
    retry: false
  });
  const assignments = data.fixtureMode
    ? fixtureAssignments
    : (assignmentQuery.data?.assignments ?? []);
  const catalog = data.fixtureMode
    ? fixtureAssignments.map((suite) => ({
        id: suite.suiteId,
        suiteKey: suite.suiteKey,
        name: suite.name,
        description: suite.description,
        active: true,
        version: suite.suiteVersion
      }))
    : (suiteQuery.data?.suites ?? []);

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['suites'] });
    void queryClient.invalidateQueries({ queryKey: ['project-suite-assignments', projectId] });
    void queryClient.invalidateQueries({ queryKey: ['projects'] });
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (data.fixtureMode) {
        throw new Error('Fixture sessions do not save suites.');
      }
      const token = await acquireAccessToken();
      if (editing) {
        return updateSuite(token, projectId, editing.suiteId, editing.suiteVersion, {
          name: name.trim(),
          ...(description.trim() ? { description: description.trim() } : {})
        });
      }
      return assignSuiteToProject(token, projectId, {
        ...(selectedSuiteId ? { suiteId: selectedSuiteId } : {}),
        name: name.trim(),
        ...(description.trim() ? { description: description.trim() } : {})
      });
    },
    onSuccess: () => {
      setName('');
      setDescription('');
      setSelectedSuiteId('');
      setEditing(null);
      setActionError(null);
      invalidate();
    },
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Suite action failed.');
    }
  });

  const unassignMutation = useMutation({
    mutationFn: async (assignment: ProjectSuiteAssignmentSummary) => {
      const token = await acquireAccessToken();
      await unassignSuiteFromProject(token, projectId, assignment.id, assignment.version);
    },
    onSuccess: invalidate,
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Suite could not be unassigned.');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: async (assignment: ProjectSuiteAssignmentSummary) => {
      const token = await acquireAccessToken();
      await deleteSuite(token, projectId, assignment.suiteId, assignment.suiteVersion);
    },
    onSuccess: invalidate,
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Suite could not be deleted.');
    }
  });

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={3}>
        <ProjectPicker
          labelId="suite-project-label"
          projects={data.projects.projects}
          projectId={projectId}
          setProjectId={setProjectId}
        />
        {actionError && <Alert severity="error">{actionError}</Alert>}
        {(suiteQuery.isError || assignmentQuery.isError) && (
          <Alert severity="error">Suite data could not be loaded.</Alert>
        )}
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Box
            component="form"
            onSubmit={(event: FormEvent<HTMLFormElement>) => {
              event.preventDefault();
              saveMutation.mutate();
            }}
          >
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <FormControl fullWidth disabled={!canSave}>
                <InputLabel id="suite-catalog-label">Reusable Suite</InputLabel>
                <Select
                  labelId="suite-catalog-label"
                  label="Reusable Suite"
                  value={editing ? '' : selectedSuiteId}
                  onChange={(event) => {
                    const suiteId = event.target.value;
                    const selected = catalog.find((suite) => suite.id === suiteId);
                    setSelectedSuiteId(suiteId);
                    setEditing(null);
                    if (selected) {
                      setName(selected.name);
                      setDescription(selected.description ?? '');
                    }
                  }}
                >
                  {catalog.map((suite) => (
                    <MenuItem key={suite.id} value={suite.id}>
                      {suite.name}
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>Select existing or enter a new suite name.</FormHelperText>
              </FormControl>
              <TextField
                label="Suite Name"
                value={name}
                onChange={(event) => {
                  setName(event.target.value);
                }}
                required
                disabled={!canSave}
                fullWidth
              />
              <TextField
                label="Description"
                value={description}
                onChange={(event) => {
                  setDescription(event.target.value);
                }}
                disabled={!canSave}
                fullWidth
              />
              <Button
                type="submit"
                variant="contained"
                disabled={!canSave || saveMutation.isPending || name.trim().length === 0}
                sx={{ minWidth: 170 }}
              >
                {editing ? 'Save Suite' : 'Assign Suite'}
              </Button>
            </Stack>
          </Box>
        </Paper>
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Assigned suites table">
            <TableHead>
              <TableRow>
                <TableCell>Suite</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Version</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {assignments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5}>
                    <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                      No suites assigned.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                assignments.map((assignment) => (
                  <TableRow key={assignment.id} hover>
                    <TableCell>
                      <Typography fontWeight={700}>{assignment.name}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {assignment.suiteKey}
                      </Typography>
                    </TableCell>
                    <TableCell>{assignment.description ?? '-'}</TableCell>
                    <TableCell>{statusChip('Assigned', 'success')}</TableCell>
                    <TableCell>{assignment.version}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1}>
                        <Button
                          size="small"
                          variant="outlined"
                          disabled={!canEdit}
                          onClick={() => {
                            setEditing(assignment);
                            setSelectedSuiteId('');
                            setName(assignment.name);
                            setDescription(assignment.description ?? '');
                          }}
                        >
                          Edit
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          disabled={!canManageAssignments}
                          onClick={() => {
                            unassignMutation.mutate(assignment);
                          }}
                        >
                          Unassign
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          color="error"
                          disabled={!canDelete}
                          onClick={() => {
                            deleteMutation.mutate(assignment);
                          }}
                        >
                          Delete
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Stack>
    </PageFrame>
  );
}

function CycleManagementPage({
  definition,
  data
}: {
  definition: RouteDefinition;
  data: ShellData;
}) {
  const [projectId, setProjectId] = useState(data.projects.projects[0]?.id ?? '');
  const [editing, setEditing] = useState<ProjectCycleSummary | null>(null);
  const [name, setName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [description, setDescription] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);
  const { accountKey, acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();
  const canCreate = hasPermission(data, projectId, 'CREATE');
  const canEdit = hasPermission(data, projectId, 'EDIT');
  const canDelete = hasPermission(data, projectId, 'DELETE');
  const canSave = editing ? canEdit : canCreate;
  const fixtureCycles = data.cycles
    .filter((cycle) => cycle.projectId === projectId)
    .map((cycle) => ({
      id: cycle.id,
      projectId: cycle.projectId,
      name: cycle.name,
      startDate: cycle.startDate,
      endDate: cycle.endDate,
      description: cycle.description,
      active: true,
      version: 0
    }));
  const cycleQuery = useQuery({
    queryKey: ['project-cycles', projectId, accountKey],
    enabled: Boolean(!data.fixtureMode && projectId),
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectCycles(token, projectId);
    },
    retry: false
  });
  const cycles = data.fixtureMode ? fixtureCycles : (cycleQuery.data?.cycles ?? []);

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['project-cycles', projectId] });
    void queryClient.invalidateQueries({ queryKey: ['projects'] });
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (data.fixtureMode) {
        throw new Error('Fixture sessions do not save cycles.');
      }
      if (startDate && endDate && endDate < startDate) {
        throw new Error('Start date must be before or equal to end date.');
      }
      const token = await acquireAccessToken();
      const payload = {
        name: name.trim(),
        ...(startDate ? { startDate } : {}),
        ...(endDate ? { endDate } : {}),
        ...(description.trim() ? { description: description.trim() } : {})
      };
      return editing
        ? updateProjectCycle(token, projectId, editing.id, editing.version, payload)
        : createProjectCycle(token, projectId, payload);
    },
    onSuccess: () => {
      setEditing(null);
      setName('');
      setStartDate('');
      setEndDate('');
      setDescription('');
      setActionError(null);
      invalidate();
    },
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Cycle action failed.');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: async (cycle: ProjectCycleSummary) => {
      const token = await acquireAccessToken();
      await deleteProjectCycle(token, projectId, cycle.id, cycle.version);
    },
    onSuccess: invalidate,
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Cycle could not be deleted.');
    }
  });

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={3}>
        <ProjectPicker
          labelId="cycle-project-label"
          projects={data.projects.projects}
          projectId={projectId}
          setProjectId={setProjectId}
        />
        {actionError && <Alert severity="error">{actionError}</Alert>}
        {cycleQuery.isError && <Alert severity="error">Cycle data could not be loaded.</Alert>}
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Box
            component="form"
            onSubmit={(event: FormEvent<HTMLFormElement>) => {
              event.preventDefault();
              saveMutation.mutate();
            }}
          >
            <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
              <TextField
                label="Cycle Name"
                value={name}
                onChange={(event) => {
                  setName(event.target.value);
                }}
                required
                disabled={!canSave}
                fullWidth
              />
              <TextField
                label="Start Date"
                type="date"
                value={startDate}
                onChange={(event) => {
                  setStartDate(event.target.value);
                }}
                required
                disabled={!canSave}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                label="End Date"
                type="date"
                value={endDate}
                onChange={(event) => {
                  setEndDate(event.target.value);
                }}
                required
                disabled={!canSave}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                label="Description"
                value={description}
                onChange={(event) => {
                  setDescription(event.target.value);
                }}
                disabled={!canSave}
                fullWidth
              />
              <Button
                type="submit"
                variant="contained"
                disabled={!canSave || saveMutation.isPending || name.trim().length === 0}
                sx={{ minWidth: 150 }}
              >
                {editing ? 'Save Cycle' : 'Create Cycle'}
              </Button>
            </Stack>
          </Box>
        </Paper>
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Test cycles table">
            <TableHead>
              <TableRow>
                <TableCell>Cycle Name</TableCell>
                <TableCell>Start Date</TableCell>
                <TableCell>End Date</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {cycles.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6}>
                    <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                      No cycles created.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                cycles.map((cycle) => (
                  <TableRow key={cycle.id} hover>
                    <TableCell>
                      <Typography fontWeight={700}>{cycle.name}</Typography>
                    </TableCell>
                    <TableCell>{cycle.startDate ?? '-'}</TableCell>
                    <TableCell>{cycle.endDate ?? '-'}</TableCell>
                    <TableCell>{cycle.description ?? '-'}</TableCell>
                    <TableCell>
                      {statusChip(cycle.active ? 'Active' : 'Disabled', 'success')}
                    </TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1}>
                        <Button
                          size="small"
                          variant="outlined"
                          disabled={!canEdit}
                          onClick={() => {
                            setEditing(cycle);
                            setName(cycle.name);
                            setStartDate(cycle.startDate ?? '');
                            setEndDate(cycle.endDate ?? '');
                            setDescription(cycle.description ?? '');
                          }}
                        >
                          Edit
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          color="error"
                          disabled={!canDelete}
                          onClick={() => {
                            deleteMutation.mutate(cycle);
                          }}
                        >
                          Delete
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Stack>
    </PageFrame>
  );
}

function ProjectPicker({
  labelId,
  projects,
  projectId,
  setProjectId
}: {
  labelId: string;
  projects: ProjectSummary[];
  projectId: string;
  setProjectId: (projectId: string) => void;
}) {
  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <FormControl fullWidth required>
        <InputLabel id={labelId}>Project</InputLabel>
        <Select
          labelId={labelId}
          label="Project"
          value={projectId}
          onChange={(event) => {
            setProjectId(event.target.value);
          }}
        >
          {projects.map((project) => (
            <MenuItem key={project.id} value={project.id}>
              {project.name}
            </MenuItem>
          ))}
        </Select>
        <FormHelperText>Options are limited to authorized projects.</FormHelperText>
      </FormControl>
    </Paper>
  );
}

function RequirementManagementPage({
  definition,
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
}) {
  const { acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const requestedProjectId = searchParams.get('projectId') ?? data.projects.projects[0]?.id ?? '';
  const selectedRequirementId = searchParams.get('requirementId');
  const [projectId, setProjectId] = useState(requestedProjectId);
  const [suiteAssignmentId, setSuiteAssignmentId] = useState('');
  const [cycleId, setCycleId] = useState('');
  const [header, setHeader] = useState('');
  const [description, setDescription] = useState('');
  const [requirementEdits, setRequirementEdits] = useState<Record<string, RequirementEditState>>(
    {}
  );
  const [editingRequirementId, setEditingRequirementId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const suiteAssignmentsQuery = useQuery({
    queryKey: ['requirement-suite-assignments', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectSuiteAssignments(token, projectId);
    }
  });
  const cyclesQuery = useQuery({
    queryKey: ['requirement-cycles', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectCycles(token, projectId);
    }
  });
  const requirementsQuery = useQuery({
    queryKey: ['requirements', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getRequirements(token, projectId);
    }
  });
  const projectAccessQuery = useQuery({
    queryKey: ['requirement-project-access', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProject(token, projectId);
    }
  });

  const fixtureAssignments: ProjectSuiteAssignmentSummary[] = data.suites
    .filter((suite) => suite.projectId === projectId)
    .map((suite) => ({
      id: suite.id,
      projectId,
      suiteId: suite.id,
      suiteKey: suite.name.toUpperCase().replaceAll(' ', '_'),
      name: suite.name,
      description: null,
      active: true,
      version: 0,
      suiteVersion: 0
    }));
  const fixtureCycles: ProjectCycleSummary[] = data.cycles
    .filter((cycle) => cycle.projectId === projectId)
    .map((cycle) => ({
      id: cycle.id,
      projectId,
      name: cycle.name,
      startDate: cycle.startDate,
      endDate: cycle.endDate,
      description: cycle.description,
      active: true,
      version: 0
    }));
  const suiteAssignments = data.fixtureMode
    ? fixtureAssignments
    : (suiteAssignmentsQuery.data?.assignments ?? []);
  const cycles = data.fixtureMode ? fixtureCycles : (cyclesQuery.data?.cycles ?? []);
  const requirements = useMemo(
    () => (data.fixtureMode ? [] : (requirementsQuery.data?.requirements ?? [])),
    [data.fixtureMode, requirementsQuery.data?.requirements]
  );
  const requirementCapabilities = useMemo(() => {
    const merged = new Set(data.fixtureMode ? capabilities : []);
    for (const capability of projectAccessQuery.data?.capabilities ?? []) {
      merged.add(capability);
    }
    if (data.session.globalAdministrator) {
      return new Set(allCapabilities);
    }
    return merged;
  }, [
    capabilities,
    data.fixtureMode,
    data.session.globalAdministrator,
    projectAccessQuery.data?.capabilities
  ]);

  useEffect(() => {
    setProjectId(requestedProjectId);
  }, [requestedProjectId]);

  useEffect(() => {
    setSuiteAssignmentId('');
    setCycleId('');
    setFeedback(null);
  }, [projectId]);

  useEffect(() => {
    setRequirementEdits((current) => {
      const next = { ...current };
      for (const requirement of requirements) {
        next[requirement.id] ??= requirementEditStateFromSummary(requirement);
      }
      return next;
    });
  }, [requirements]);

  const createMutation = useMutation({
    mutationFn: async () => {
      if (data.fixtureMode) {
        throw new Error('Fixture sessions do not save requirements.');
      }
      const token = await acquireAccessToken();
      return createManualRequirement(token, {
        projectId,
        projectSuiteAssignmentId: suiteAssignmentId,
        testCycleId: cycleId,
        header,
        description
      });
    },
    onSuccess: (created) => {
      setHeader('');
      setDescription('');
      setFeedback(`${created.reqId} was saved as Draft.`);
      void queryClient.invalidateQueries({ queryKey: ['requirements', data.authMode, projectId] });
    },
    onError: () => {
      setFeedback('The requirement could not be saved. Check the selections and try again.');
    }
  });
  const approveMutation = useMutation({
    mutationFn: async (requirement: RequirementSummary) => {
      const token = await acquireAccessToken();
      return approveRequirement(token, projectId, requirement.id, requirement.version);
    },
    onSuccess: () => {
      setFeedback('Requirement approved.');
      void queryClient.invalidateQueries({ queryKey: ['requirements', data.authMode, projectId] });
    },
    onError: () => {
      setFeedback('The requirement could not be approved. Refresh and try again.');
    }
  });
  const updateMutation = useMutation({
    mutationFn: async (requirement: RequirementSummary) => {
      const edit = requirementEdits[requirement.id] ?? requirementEditStateFromSummary(requirement);
      const token = await acquireAccessToken();
      return updateRequirement(token, projectId, requirement.id, requirement.version, {
        header: edit.header.trim(),
        description: edit.description.trim(),
        acceptanceCriteria: edit.acceptanceCriteria.trim(),
        assumptions: edit.assumptions.trim(),
        dependencies: edit.dependencies.trim()
      });
    },
    onSuccess: () => {
      setEditingRequirementId(null);
      setFeedback('Requirement updated.');
      void queryClient.invalidateQueries({ queryKey: ['requirements', data.authMode, projectId] });
    },
    onError: () => {
      setFeedback('The requirement could not be updated. Refresh and try again.');
    }
  });
  const deleteMutation = useMutation({
    mutationFn: async (requirement: RequirementSummary) => {
      const token = await acquireAccessToken();
      await deleteRequirement(token, projectId, requirement.id, requirement.version);
    },
    onSuccess: () => {
      setFeedback('Requirement deleted.');
      void queryClient.invalidateQueries({ queryKey: ['requirements', data.authMode, projectId] });
    },
    onError: () => {
      setFeedback('The requirement could not be deleted. It may have linked test cases.');
    }
  });

  const selectorPanel = (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
        <FormControl fullWidth required>
          <InputLabel id="requirement-project-label">Project</InputLabel>
          <Select
            labelId="requirement-project-label"
            label="Project"
            value={projectId}
            onChange={(event) => {
              setProjectId(event.target.value);
            }}
          >
            {data.projects.projects.map((project) => (
              <MenuItem key={project.id} value={project.id}>
                {project.name}
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>Only authorized projects are available.</FormHelperText>
        </FormControl>
        <FormControl fullWidth required disabled={suiteAssignments.length === 0}>
          <InputLabel id="requirement-suite-label">Test Suite</InputLabel>
          <Select
            labelId="requirement-suite-label"
            label="Test Suite"
            value={suiteAssignmentId}
            onChange={(event) => {
              setSuiteAssignmentId(event.target.value);
            }}
          >
            {suiteAssignments.map((suite) => (
              <MenuItem key={suite.id} value={suite.id}>
                {suite.name}
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>Suite choices are project-scoped.</FormHelperText>
        </FormControl>
        <FormControl fullWidth required disabled={cycles.length === 0}>
          <InputLabel id="requirement-cycle-label">Test Cycle</InputLabel>
          <Select
            labelId="requirement-cycle-label"
            label="Test Cycle"
            value={cycleId}
            onChange={(event) => {
              setCycleId(event.target.value);
            }}
          >
            {cycles.map((cycle) => (
              <MenuItem key={cycle.id} value={cycle.id}>
                {cycle.name}
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>Cycle choices are project-scoped.</FormHelperText>
        </FormControl>
      </Stack>
    </Paper>
  );

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={3}>
        <Stack direction="row" alignItems="center">
          <Tabs
            value={definition.key === 'requirements' ? false : definition.key}
            aria-label="Requirement Management tabs"
            variant="scrollable"
          >
            <Tab
              component={NavLink}
              to="/requirements/generate"
              value="requirements-generate"
              label="Generate Requirements"
            />
            <Tab
              component={NavLink}
              to="/requirements/add"
              value="requirements-add"
              label="Add Manually"
            />
            <Tab
              component={NavLink}
              to="/requirements/view"
              value="requirements-view"
              label="Manage Requirements"
            />
          </Tabs>
          <Tooltip title="Open Manage Requirements in a new tab">
            <IconButton
              component="a"
              href={`/requirements/view${projectId ? `?projectId=${encodeURIComponent(projectId)}` : ''}`}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Open Manage Requirements in a new tab"
              size="small"
              color="primary"
              sx={{ ml: 0.5, flexShrink: 0 }}
            >
              <OpenInNewOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
        {selectorPanel}
        {feedback && (
          <Alert severity={feedback.includes('could not') ? 'error' : 'success'}>{feedback}</Alert>
        )}
        {definition.key === 'requirements' && (
          <Alert severity="info">
            Choose Generate Requirements, Add Manually, or Manage Requirements to continue.
          </Alert>
        )}
        {definition.key === 'requirements-generate' && (
          <RequirementGenerationPanel
            projectId={projectId}
            suiteAssignmentId={suiteAssignmentId}
            cycleId={cycleId}
            authorized={canAccess(requirementCapabilities, [
              'REQUIREMENT_CREATE',
              'UPLOAD_ACCESS',
              'GENERATION_JOB_ACCESS'
            ])}
            onGenerate={async (document) => {
              const token = await acquireAccessToken();
              return generateRequirementsFromDocument(token, document, {
                projectId,
                projectSuiteAssignmentId: suiteAssignmentId,
                testCycleId: cycleId
              });
            }}
            onViewGeneratedRequirements={() => {
              void navigate(`/requirements/view?projectId=${encodeURIComponent(projectId)}`);
            }}
          />
        )}
        {definition.key === 'requirements-add' && (
          <Card
            component="form"
            variant="outlined"
            onSubmit={(event: FormEvent) => {
              event.preventDefault();
              setFeedback(null);
              createMutation.mutate();
            }}
          >
            <CardContent>
              <Stack spacing={2}>
                <Typography variant="h6" component="h2">
                  Requirement Draft
                </Typography>
                <TextField
                  label="Header"
                  required
                  fullWidth
                  value={header}
                  slotProps={{ htmlInput: { maxLength: 300 } }}
                  onChange={(event) => {
                    setHeader(event.target.value);
                  }}
                />
                <TextField
                  label="Description"
                  required
                  fullWidth
                  multiline
                  minRows={4}
                  value={description}
                  onChange={(event) => {
                    setDescription(event.target.value);
                  }}
                />
                <Button
                  type="submit"
                  variant="contained"
                  disabled={
                    createMutation.isPending ||
                    !projectId ||
                    !suiteAssignmentId ||
                    !cycleId ||
                    !header.trim() ||
                    !description.trim() ||
                    !canAccess(requirementCapabilities, ['REQUIREMENT_CREATE'])
                  }
                >
                  Save Draft
                </Button>
              </Stack>
            </CardContent>
          </Card>
        )}
        {definition.key === 'requirements-view' && (
          <RequirementTable
            requirements={requirements}
            selectedRequirementId={selectedRequirementId}
            loading={requirementsQuery.isLoading && !data.fixtureMode}
            edits={requirementEdits}
            editingRequirementId={editingRequirementId}
            canEdit={canAccess(requirementCapabilities, ['REQUIREMENT_EDIT'])}
            canApprove={canAccess(requirementCapabilities, ['REQUIREMENT_APPROVE'])}
            canDelete={canAccess(requirementCapabilities, ['REQUIREMENT_DELETE_UNLINKED'])}
            busy={approveMutation.isPending || updateMutation.isPending || deleteMutation.isPending}
            onEdit={(requirement) => {
              setFeedback(null);
              setRequirementEdits((current) => ({
                ...current,
                [requirement.id]:
                  current[requirement.id] ?? requirementEditStateFromSummary(requirement)
              }));
              setEditingRequirementId(requirement.id);
            }}
            onEditChange={(requirementId, patch) => {
              setRequirementEdits((current) => ({
                ...current,
                [requirementId]: {
                  ...(current[requirementId] ?? {
                    header: '',
                    description: '',
                    acceptanceCriteria: '',
                    assumptions: '',
                    dependencies: ''
                  }),
                  ...patch
                }
              }));
            }}
            onEditClose={() => {
              setEditingRequirementId(null);
            }}
            onSave={(requirement) => {
              setFeedback(null);
              updateMutation.mutate(requirement);
            }}
            onApprove={(requirement) => {
              setFeedback(null);
              approveMutation.mutate(requirement);
            }}
            onDelete={(requirement) => {
              setFeedback(null);
              deleteMutation.mutate(requirement);
            }}
          />
        )}
      </Stack>
    </PageFrame>
  );
}

interface RequirementEditState {
  header: string;
  description: string;
  acceptanceCriteria: string;
  assumptions: string;
  dependencies: string;
}

function requirementEditStateFromSummary(requirement: RequirementSummary): RequirementEditState {
  return {
    header: requirement.header,
    description: requirement.description,
    acceptanceCriteria: requirement.acceptanceCriteria,
    assumptions: requirement.assumptions,
    dependencies: requirement.dependencies
  };
}

function RequirementTable({
  requirements,
  selectedRequirementId,
  loading,
  edits,
  editingRequirementId,
  canEdit,
  canApprove,
  canDelete,
  busy,
  onEdit,
  onEditChange,
  onEditClose,
  onSave,
  onApprove,
  onDelete
}: {
  requirements: RequirementSummary[];
  selectedRequirementId: string | null;
  loading: boolean;
  edits: Record<string, RequirementEditState>;
  editingRequirementId: string | null;
  canEdit: boolean;
  canApprove: boolean;
  canDelete: boolean;
  busy: boolean;
  onEdit: (requirement: RequirementSummary) => void;
  onEditChange: (requirementId: string, patch: Partial<RequirementEditState>) => void;
  onEditClose: () => void;
  onSave: (requirement: RequirementSummary) => void;
  onApprove: (requirement: RequirementSummary) => void;
  onDelete: (requirement: RequirementSummary) => void;
}) {
  const editingRequirement =
    requirements.find((requirement) => requirement.id === editingRequirementId) ?? null;
  const editState = editingRequirement
    ? (edits[editingRequirement.id] ?? requirementEditStateFromSummary(editingRequirement))
    : null;
  const canSaveEdit =
    Boolean(editingRequirement) &&
    Boolean(editState?.header.trim()) &&
    Boolean(editState?.description.trim());

  return (
    <>
      <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2.5 }}>
        <TableContainer
          sx={{
            border: `1px solid ${designTokens.color.border}`,
            borderRadius: 1.5,
            overflow: 'hidden'
          }}
        >
          <Table aria-label="Requirements table" size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ width: 86 }}>ReqID</TableCell>
                <TableCell sx={{ minWidth: 220 }}>Header</TableCell>
                <TableCell sx={{ minWidth: 120 }}>Suite</TableCell>
                <TableCell sx={{ minWidth: 110 }}>Cycle</TableCell>
                <TableCell sx={{ width: 105 }}>Status</TableCell>
                <TableCell sx={{ width: 112 }}>Created</TableCell>
                <TableCell
                  align="center"
                  sx={{ minWidth: 210, borderLeft: `1px solid ${designTokens.color.border}` }}
                >
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading && (
                <TableRow>
                  <TableCell colSpan={7}>
                    <CircularProgress size={24} aria-label="Loading requirements" />
                  </TableCell>
                </TableRow>
              )}
              {!loading && requirements.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>No requirements found for this project.</TableCell>
                </TableRow>
              )}
              {requirements.map((requirement) => (
                <TableRow
                  key={requirement.id}
                  hover
                  selected={requirement.id === selectedRequirementId}
                >
                  <TableCell>
                    <Typography variant="caption" fontWeight={600} color="text.secondary">
                      {requirement.reqId}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight={700}>
                      {requirement.header}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {requirement.description}
                    </Typography>
                    {requirement.acceptanceCriteria && (
                      <RequirementDetail
                        label="Acceptance Criteria"
                        value={requirement.acceptanceCriteria}
                      />
                    )}
                    {requirement.assumptions && (
                      <RequirementDetail label="Assumptions" value={requirement.assumptions} />
                    )}
                    {requirement.dependencies && (
                      <RequirementDetail label="Dependencies" value={requirement.dependencies} />
                    )}
                  </TableCell>
                  <TableCell>{requirement.suiteName}</TableCell>
                  <TableCell>{requirement.cycleName}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={requirement.status}
                      color={requirement.status === 'Approved' ? 'success' : 'warning'}
                      sx={{ height: 22, fontSize: '0.7rem' }}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {new Date(requirement.createdDate).toLocaleDateString()}
                    </Typography>
                  </TableCell>
                  <TableCell
                    align="center"
                    sx={{ borderLeft: `1px solid ${designTokens.color.border}` }}
                  >
                    <Stack direction="row" spacing={0.25} justifyContent="center">
                      {canApprove && requirement.status === 'Draft' && (
                        <Button
                          aria-label="Approve"
                          size="small"
                          disabled={busy}
                          onClick={() => {
                            onApprove(requirement);
                          }}
                          sx={{
                            minWidth: 62,
                            flexDirection: 'column',
                            gap: 0.25,
                            fontSize: '0.68rem'
                          }}
                        >
                          <CheckCircleOutlineIcon fontSize="small" />
                          Approve
                        </Button>
                      )}
                      {canEdit && (
                        <Button
                          aria-label="Edit"
                          size="small"
                          disabled={busy}
                          onClick={() => {
                            onEdit(requirement);
                          }}
                          sx={{
                            minWidth: 52,
                            flexDirection: 'column',
                            gap: 0.25,
                            fontSize: '0.68rem'
                          }}
                        >
                          <EditOutlinedIcon fontSize="small" />
                          Edit
                        </Button>
                      )}
                      {canDelete && (
                        <Button
                          aria-label="Delete"
                          size="small"
                          color="error"
                          disabled={busy}
                          onClick={() => {
                            onDelete(requirement);
                          }}
                          sx={{
                            minWidth: 56,
                            flexDirection: 'column',
                            gap: 0.25,
                            fontSize: '0.68rem'
                          }}
                        >
                          <DeleteOutlineIcon fontSize="small" />
                          Delete
                        </Button>
                      )}
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <Alert
          severity="info"
          variant="outlined"
          sx={{ mt: 1.5, py: 0.25, bgcolor: '#f8fafc', color: 'text.secondary' }}
        >
          Once approved, the requirement status is updated and cannot be changed.
        </Alert>
      </Paper>
      <Dialog open={Boolean(editingRequirement && editState)} onClose={onEditClose} fullWidth>
        <DialogTitle>Edit Requirement</DialogTitle>
        <DialogContent>
          {editingRequirement && editState && (
            <Stack spacing={2} sx={{ pt: 1 }}>
              <TextField label="ReqID" value={editingRequirement.reqId} disabled fullWidth />
              <TextField
                label="Header"
                required
                fullWidth
                value={editState.header}
                slotProps={{ htmlInput: { maxLength: 300 } }}
                onChange={(event) => {
                  onEditChange(editingRequirement.id, { header: event.target.value });
                }}
              />
              <TextField
                label="Description"
                required
                fullWidth
                multiline
                minRows={4}
                value={editState.description}
                onChange={(event) => {
                  onEditChange(editingRequirement.id, { description: event.target.value });
                }}
              />
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onEditClose}>Cancel</Button>
          {editingRequirement && (
            <Button
              variant="contained"
              disabled={busy || !canSaveEdit}
              onClick={() => {
                onSave(editingRequirement);
              }}
            >
              Save
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </>
  );
}

function RequirementDetail({ label, value }: { label: string; value: string }) {
  return (
    <Box sx={{ mt: 1 }}>
      <Typography variant="caption" fontWeight={700} color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
        {value}
      </Typography>
    </Box>
  );
}

const testCaseStatuses: TestCaseStatus[] = [
  'Draft',
  'Inprogress',
  'Defect',
  'Resolved',
  'Not applicable',
  'Retest'
];

interface TestCaseEditState {
  header: string;
  description: string;
  assigneeMembershipId: string;
  dueDate: string;
  status: TestCaseStatus;
}

function editStateFromTestCase(testCase: TestCaseSummary): TestCaseEditState {
  return {
    header: testCase.header,
    description: testCase.description,
    assigneeMembershipId: testCase.assigneeMembershipId ?? '',
    dueDate: testCase.dueDate ?? '',
    status: testCase.status
  };
}

function TestCaseEditDialog({
  open,
  testCase,
  edit,
  activeMembers,
  canAssign,
  busy,
  onChange,
  onClose,
  onSubmit
}: {
  open: boolean;
  testCase: TestCaseSummary | null;
  edit: TestCaseEditState | null;
  activeMembers: ProjectMembershipSummary[];
  canAssign: boolean;
  busy: boolean;
  onChange: (patch: Partial<TestCaseEditState>) => void;
  onClose: () => void;
  onSubmit: () => void;
}) {
  const canSave =
    edit !== null && edit.header.trim().length > 0 && edit.description.trim().length > 0;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="md"
      aria-labelledby="edit-test-case-dialog-title"
    >
      <Box
        component="form"
        onSubmit={(event: FormEvent) => {
          event.preventDefault();
          onSubmit();
        }}
      >
        <DialogTitle id="edit-test-case-dialog-title">Edit Test Case</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField label="Test Case ID" value={testCase?.testCaseId ?? ''} fullWidth disabled />
            <TextField
              label="Test Case Header"
              required
              fullWidth
              value={edit?.header ?? ''}
              slotProps={{ htmlInput: { maxLength: 300 } }}
              onChange={(event) => {
                onChange({ header: event.target.value });
              }}
            />
            <TextField
              label="Description"
              required
              fullWidth
              multiline
              minRows={4}
              value={edit?.description ?? ''}
              onChange={(event) => {
                onChange({ description: event.target.value });
              }}
            />
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <FormControl fullWidth>
                <InputLabel id="edit-test-case-status-label">Status</InputLabel>
                <Select
                  labelId="edit-test-case-status-label"
                  label="Status"
                  value={edit?.status ?? 'Draft'}
                  onChange={(event) => {
                    onChange({ status: event.target.value as TestCaseStatus });
                  }}
                >
                  {testCaseStatuses.map((status) => (
                    <MenuItem key={status} value={status}>
                      {status}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel id="edit-test-case-assignee-label">Assign To</InputLabel>
                <Select
                  labelId="edit-test-case-assignee-label"
                  label="Assign To"
                  value={edit?.assigneeMembershipId ?? ''}
                  displayEmpty
                  disabled={!canAssign}
                  onChange={(event) => {
                    onChange({ assigneeMembershipId: event.target.value });
                  }}
                >
                  <MenuItem value="">Unassigned</MenuItem>
                  {activeMembers.map((membership) => (
                    <MenuItem key={membership.id} value={membership.id}>
                      {membership.firstName} {membership.lastName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <TextField
                label="Due Date"
                type="date"
                fullWidth
                value={edit?.dueDate ?? ''}
                slotProps={{ inputLabel: { shrink: true } }}
                onChange={(event) => {
                  onChange({ dueDate: event.target.value });
                }}
              />
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={busy || !canSave}>
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

function TestCasesThroughRequirementsPage({
  definition,
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
}) {
  const { acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();
  const [projectId, setProjectId] = useState(data.projects.projects[0]?.id ?? '');
  const [suiteAssignmentId, setSuiteAssignmentId] = useState('');
  const [cycleId, setCycleId] = useState('');
  const [requirementId, setRequirementId] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [manualOpen, setManualOpen] = useState(false);
  const [manualHeader, setManualHeader] = useState('');
  const [manualDescription, setManualDescription] = useState('');
  const [edits, setEdits] = useState<Record<string, TestCaseEditState>>({});
  const [editingTestCaseId, setEditingTestCaseId] = useState<string | null>(null);

  const suiteAssignmentsQuery = useQuery({
    queryKey: ['testcase-suite-assignments', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectSuiteAssignments(token, projectId);
    }
  });
  const cyclesQuery = useQuery({
    queryKey: ['testcase-cycles', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectCycles(token, projectId);
    }
  });
  const requirementsQuery = useQuery({
    queryKey: ['testcase-requirements', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getRequirements(token, projectId);
    }
  });
  const projectAccessQuery = useQuery({
    queryKey: ['testcase-project-access', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProject(token, projectId);
    }
  });

  const fixtureAssignments: ProjectSuiteAssignmentSummary[] = data.suites
    .filter((suite) => suite.projectId === projectId)
    .map((suite) => ({
      id: suite.id,
      projectId,
      suiteId: suite.id,
      suiteKey: suite.name.toUpperCase().replaceAll(' ', '_'),
      name: suite.name,
      description: null,
      active: true,
      version: 0,
      suiteVersion: 0
    }));
  const fixtureCycles: ProjectCycleSummary[] = data.cycles
    .filter((cycle) => cycle.projectId === projectId)
    .map((cycle) => ({
      id: cycle.id,
      projectId,
      name: cycle.name,
      startDate: cycle.startDate,
      endDate: cycle.endDate,
      description: cycle.description,
      active: true,
      version: 0
    }));
  const suiteAssignments = data.fixtureMode
    ? fixtureAssignments
    : (suiteAssignmentsQuery.data?.assignments ?? []);
  const cycles = data.fixtureMode ? fixtureCycles : (cyclesQuery.data?.cycles ?? []);
  const memberships = data.fixtureMode
    ? (data.memberships[projectId] ?? [])
    : (projectAccessQuery.data?.memberships ?? []);
  const activeMembers = memberships.filter(
    (membership) => membership.membershipStatus === 'ACTIVE'
  );

  const fixtureRequirements: RequirementSummary[] =
    projectId && suiteAssignments[0] && cycles[0]
      ? [
          {
            id: 'fixture-requirement-1',
            projectId,
            projectSuiteAssignmentId: suiteAssignments[0].id,
            suiteId: suiteAssignments[0].suiteId,
            suiteName: suiteAssignments[0].name,
            testCycleId: cycles[0].id,
            cycleName: cycles[0].name,
            reqId: 'REQ-001',
            header: 'Verify user can clock in',
            description: 'Confirm an active employee can clock in from an approved device.',
            acceptanceCriteria: '',
            assumptions: '',
            dependencies: '',
            status: 'Approved',
            sourceType: 'MANUAL',
            createdDate: new Date().toISOString(),
            approvedAt: null,
            approvedBy: null,
            version: 0
          }
        ]
      : [];
  const requirements = data.fixtureMode
    ? fixtureRequirements
    : (requirementsQuery.data?.requirements ?? []);
  const scopedRequirements = requirements.filter(
    (requirement) =>
      requirement.status === 'Approved' &&
      (!suiteAssignmentId || requirement.projectSuiteAssignmentId === suiteAssignmentId) &&
      (!cycleId || requirement.testCycleId === cycleId)
  );

  const context: RequirementSelectionContext | null =
    projectId && requirementId
      ? {
          projectId,
          projectSuiteAssignmentId: suiteAssignmentId || null,
          testCycleId: cycleId || null,
          requirementId
        }
      : null;

  const testCasesQuery = useQuery({
    queryKey: ['test-cases-through-requirements', data.authMode, context],
    enabled: Boolean(context) && !data.fixtureMode,
    queryFn: async () => {
      if (!context) {
        throw new Error('Select a project and requirement.');
      }
      const token = await acquireAccessToken();
      return getTestCases(token, context);
    }
  });

  const testCaseCapabilities = useMemo(() => {
    const merged = new Set(data.fixtureMode ? capabilities : []);
    for (const capability of projectAccessQuery.data?.capabilities ?? []) {
      merged.add(capability);
    }
    if (data.session.globalAdministrator) {
      return new Set(allCapabilities);
    }
    return merged;
  }, [
    capabilities,
    data.fixtureMode,
    data.session.globalAdministrator,
    projectAccessQuery.data?.capabilities
  ]);
  const canCreate = canAccess(testCaseCapabilities, ['TEST_CASE_CREATE']);
  const canEdit = canAccess(testCaseCapabilities, ['TEST_CASE_EDIT']);
  const canAssign = canAccess(testCaseCapabilities, ['TEST_CASE_ASSIGN']);
  const canDelete = canAccess(testCaseCapabilities, ['TEST_CASE_DELETE_DRAFT']);
  const canGenerate = canAccess(testCaseCapabilities, [
    'TEST_CASE_CREATE',
    'GENERATION_JOB_ACCESS'
  ]);
  const canUpload = canCreate;

  const testCases = useMemo(
    () => (data.fixtureMode ? [] : (testCasesQuery.data?.testCases ?? [])),
    [data.fixtureMode, testCasesQuery.data?.testCases]
  );

  useEffect(() => {
    setSuiteAssignmentId('');
    setCycleId('');
    setRequirementId('');
    setFeedback(null);
  }, [projectId]);

  useEffect(() => {
    setRequirementId('');
  }, [suiteAssignmentId, cycleId]);

  useEffect(() => {
    setEdits((current) => {
      const next = { ...current };
      for (const testCase of testCases) {
        next[testCase.id] ??= editStateFromTestCase(testCase);
      }
      return next;
    });
  }, [testCases]);

  const invalidateTestCases = () => {
    void queryClient.invalidateQueries({ queryKey: ['test-cases-through-requirements'] });
  };
  const generateMutation = useMutation({
    mutationFn: async () => {
      if (!context) {
        throw new Error('Select a project and requirement.');
      }
      const token = await acquireAccessToken();
      return generateTestCasesFromRequirement(token, context);
    },
    onSuccess: (result) => {
      setFeedback(`${String(result.importedCount)} AI test case(s) saved as Draft.`);
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : 'AI generation failed.');
    }
  });
  const manualMutation = useMutation({
    mutationFn: async () => {
      if (!context) {
        throw new Error('Select a project and requirement.');
      }
      const token = await acquireAccessToken();
      return createManualTestCase(token, {
        ...context,
        header: manualHeader.trim(),
        description: manualDescription.trim()
      });
    },
    onSuccess: (created) => {
      setManualOpen(false);
      setManualHeader('');
      setManualDescription('');
      setFeedback(`${created.testCaseId} was saved as Draft.`);
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : 'Manual test case could not be saved.');
    }
  });
  const csvMutation = useMutation({
    mutationFn: async (file: File) => {
      if (!context) {
        throw new Error('Select a project and requirement.');
      }
      const token = await acquireAccessToken();
      return importTestCasesCsv(token, context, file);
    },
    onSuccess: (result) => {
      setFeedback(`${String(result.importedCount)} CSV test case(s) imported as Draft.`);
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : 'CSV import failed.');
    }
  });
  const updateMutation = useMutation({
    mutationFn: async (testCase: TestCaseSummary) => {
      const edit = edits[testCase.id];
      const token = await acquireAccessToken();
      return updateTestCase(token, projectId, testCase.id, testCase.version, {
        header: edit?.header.trim() ?? testCase.header,
        description: edit?.description.trim() ?? testCase.description,
        assigneeMembershipId:
          edit?.assigneeMembershipId && edit.assigneeMembershipId.length > 0
            ? edit.assigneeMembershipId
            : null,
        dueDate: edit?.dueDate && edit.dueDate.length > 0 ? edit.dueDate : null,
        status: edit?.status ?? testCase.status
      });
    },
    onSuccess: () => {
      setFeedback('Test case updated.');
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : 'Test case could not be updated.');
    }
  });
  const deleteMutation = useMutation({
    mutationFn: async (testCase: TestCaseSummary) => {
      const token = await acquireAccessToken();
      await deleteTestCase(token, projectId, testCase.id, testCase.version);
    },
    onSuccess: () => {
      setFeedback('Draft test case deleted.');
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : 'Only Draft test cases can be deleted.');
    }
  });

  const updateEdit = (testCaseId: string, patch: Partial<TestCaseEditState>) => {
    setEdits((current) => ({
      ...current,
      [testCaseId]: {
        header: current[testCaseId]?.header ?? '',
        description: current[testCaseId]?.description ?? '',
        assigneeMembershipId: current[testCaseId]?.assigneeMembershipId ?? '',
        dueDate: current[testCaseId]?.dueDate ?? '',
        status: current[testCaseId]?.status ?? 'Draft',
        ...patch
      }
    }));
  };

  const downloadSample = () => {
    const link = document.createElement('a');
    const blob = new Blob(
      [
        'Test Case Header,Description\r\nValidate employee clock-in,Confirm an active employee can clock in successfully.\r\n'
      ],
      { type: 'text/csv' }
    );
    link.href = URL.createObjectURL(blob);
    link.download = 'test-case-upload-sample.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const busy =
    generateMutation.isPending ||
    manualMutation.isPending ||
    csvMutation.isPending ||
    updateMutation.isPending ||
    deleteMutation.isPending;
  const editingTestCase = testCases.find((testCase) => testCase.id === editingTestCaseId) ?? null;
  const editingState = editingTestCase
    ? (edits[editingTestCase.id] ?? editStateFromTestCase(editingTestCase))
    : null;

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={3}>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
            <FormControl fullWidth required>
              <InputLabel id="testcase-project-label">Project</InputLabel>
              <Select
                labelId="testcase-project-label"
                label="Project"
                value={projectId}
                onChange={(event) => {
                  setProjectId(event.target.value);
                }}
              >
                {data.projects.projects.map((project) => (
                  <MenuItem key={project.id} value={project.id}>
                    {project.name}
                  </MenuItem>
                ))}
              </Select>
              <FormHelperText>Only authorized projects are available.</FormHelperText>
            </FormControl>
            <FormControl fullWidth disabled={suiteAssignments.length === 0}>
              <InputLabel id="testcase-suite-label">Test Suite</InputLabel>
              <Select
                labelId="testcase-suite-label"
                label="Test Suite"
                value={suiteAssignmentId}
                onChange={(event) => {
                  setSuiteAssignmentId(event.target.value);
                }}
              >
                <MenuItem value="">All test suites</MenuItem>
                {suiteAssignments.map((suite) => (
                  <MenuItem key={suite.id} value={suite.id}>
                    {suite.name}
                  </MenuItem>
                ))}
              </Select>
              <FormHelperText>Optional filter for the Requirement dropdown.</FormHelperText>
            </FormControl>
            <FormControl fullWidth disabled={cycles.length === 0}>
              <InputLabel id="testcase-cycle-label">Test Cycle</InputLabel>
              <Select
                labelId="testcase-cycle-label"
                label="Test Cycle"
                value={cycleId}
                onChange={(event) => {
                  setCycleId(event.target.value);
                }}
              >
                <MenuItem value="">All test cycles</MenuItem>
                {cycles.map((cycle) => (
                  <MenuItem key={cycle.id} value={cycle.id}>
                    {cycle.name}
                  </MenuItem>
                ))}
              </Select>
              <FormHelperText>Optional filter for the Requirement dropdown.</FormHelperText>
            </FormControl>
          </Stack>
        </Paper>
        <FormControl fullWidth required disabled={!projectId}>
          <InputLabel id="testcase-requirement-label">Requirement</InputLabel>
          <Select
            labelId="testcase-requirement-label"
            label="Requirement"
            value={requirementId}
            onChange={(event) => {
              setRequirementId(event.target.value);
            }}
          >
            {scopedRequirements.map((requirement) => (
              <MenuItem key={requirement.id} value={requirement.id}>
                {requirement.reqId} - {requirement.header}
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>
            Selecting a project loads approved requirements; suite and cycle narrow this list.
          </FormHelperText>
        </FormControl>
        {feedback && (
          <Alert
            severity={
              feedback.includes('failed') || feedback.includes('could') ? 'error' : 'success'
            }
          >
            {feedback}
          </Alert>
        )}
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} justifyContent="flex-end">
          <Button
            variant="contained"
            startIcon={<AutoAwesomeOutlinedIcon />}
            disabled={!context || !canGenerate || busy || data.fixtureMode}
            onClick={() => {
              setFeedback(null);
              generateMutation.mutate();
            }}
          >
            Generate Test Cases
          </Button>
          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            disabled={!context || !canCreate || busy || data.fixtureMode}
            onClick={() => {
              setManualOpen(true);
            }}
          >
            Add Manually
          </Button>
          <Button variant="outlined" startIcon={<DownloadOutlinedIcon />} onClick={downloadSample}>
            CSV Sample
          </Button>
          <Button
            component="label"
            variant="outlined"
            startIcon={<UploadFileOutlinedIcon />}
            disabled={!context || !canUpload || busy || data.fixtureMode}
          >
            Upload from CSV
            <input
              hidden
              type="file"
              accept=".csv,text/csv"
              onChange={(event) => {
                const file = event.target.files?.[0];
                event.target.value = '';
                if (file) {
                  setFeedback(null);
                  csvMutation.mutate(file);
                }
              }}
            />
          </Button>
        </Stack>
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Requirement-linked test cases table">
            <TableHead>
              <TableRow>
                <TableCell>Test Case ID</TableCell>
                <TableCell>Test Case Header</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Assign To</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {testCasesQuery.isLoading && !data.fixtureMode && (
                <TableRow>
                  <TableCell colSpan={7}>
                    <CircularProgress size={24} aria-label="Loading test cases" />
                  </TableCell>
                </TableRow>
              )}
              {!testCasesQuery.isLoading && testCases.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>
                    No test cases found for the selected requirement.
                  </TableCell>
                </TableRow>
              )}
              {testCases.map((testCase) => (
                <TableRow key={testCase.id} hover>
                  <TableCell>{testCase.testCaseId}</TableCell>
                  <TableCell>
                    <Typography fontWeight={700}>{testCase.header}</Typography>
                  </TableCell>
                  <TableCell>{testCase.description}</TableCell>
                  <TableCell>{testCase.status}</TableCell>
                  <TableCell>{testCase.assigneeName ?? 'Unassigned'}</TableCell>
                  <TableCell>{testCase.dueDate ?? '-'}</TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={1}>
                      <Button
                        size="small"
                        variant="outlined"
                        disabled={busy || !canEdit}
                        onClick={() => {
                          setFeedback(null);
                          setEdits((current) => ({
                            ...current,
                            [testCase.id]: current[testCase.id] ?? editStateFromTestCase(testCase)
                          }));
                          setEditingTestCaseId(testCase.id);
                        }}
                      >
                        Edit
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        disabled={busy || !canDelete || testCase.status !== 'Draft'}
                        onClick={() => {
                          setFeedback(null);
                          deleteMutation.mutate(testCase);
                        }}
                      >
                        Delete
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Stack>
      <TestCaseEditDialog
        open={Boolean(editingTestCase)}
        testCase={editingTestCase}
        edit={editingState}
        activeMembers={activeMembers}
        canAssign={canAssign}
        busy={busy}
        onChange={(patch) => {
          if (editingTestCase) {
            updateEdit(editingTestCase.id, patch);
          }
        }}
        onClose={() => {
          setEditingTestCaseId(null);
        }}
        onSubmit={() => {
          if (editingTestCase) {
            setFeedback(null);
            updateMutation.mutate(editingTestCase, {
              onSuccess: () => {
                setEditingTestCaseId(null);
              }
            });
          }
        }}
      />
      <Dialog
        open={manualOpen}
        onClose={() => {
          setManualOpen(false);
        }}
        fullWidth
        maxWidth="sm"
        aria-labelledby="manual-test-case-dialog-title"
      >
        <Box
          component="form"
          onSubmit={(event: FormEvent) => {
            event.preventDefault();
            setFeedback(null);
            manualMutation.mutate();
          }}
        >
          <DialogTitle id="manual-test-case-dialog-title">Add Manual Test Case</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ pt: 1 }}>
              <TextField
                label="Test Case Header"
                required
                fullWidth
                value={manualHeader}
                slotProps={{ htmlInput: { maxLength: 300 } }}
                onChange={(event) => {
                  setManualHeader(event.target.value);
                }}
              />
              <TextField
                label="Test Case Description"
                required
                fullWidth
                multiline
                minRows={4}
                value={manualDescription}
                onChange={(event) => {
                  setManualDescription(event.target.value);
                }}
              />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                setManualOpen(false);
              }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={
                !manualHeader.trim() || !manualDescription.trim() || manualMutation.isPending
              }
            >
              Save Draft
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </PageFrame>
  );
}

function AdhocTestCasesPage({
  definition,
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
}) {
  const { acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();
  const [projectId, setProjectId] = useState(data.projects.projects[0]?.id ?? '');
  const [suiteAssignmentId, setSuiteAssignmentId] = useState('');
  const [cycleId, setCycleId] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [manualOpen, setManualOpen] = useState(false);
  const [manualHeader, setManualHeader] = useState('');
  const [manualDescription, setManualDescription] = useState('');
  const [edits, setEdits] = useState<Record<string, TestCaseEditState>>({});
  const [editingTestCaseId, setEditingTestCaseId] = useState<string | null>(null);

  const suiteAssignmentsQuery = useQuery({
    queryKey: ['adhoc-suite-assignments', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectSuiteAssignments(token, projectId);
    }
  });
  const cyclesQuery = useQuery({
    queryKey: ['adhoc-cycles', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectCycles(token, projectId);
    }
  });
  const projectAccessQuery = useQuery({
    queryKey: ['adhoc-project-access', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProject(token, projectId);
    }
  });

  const fixtureAssignments: ProjectSuiteAssignmentSummary[] = data.suites
    .filter((suite) => suite.projectId === projectId)
    .map((suite) => ({
      id: suite.id,
      projectId,
      suiteId: suite.id,
      suiteKey: suite.name.toUpperCase().replaceAll(' ', '_'),
      name: suite.name,
      description: null,
      active: true,
      version: 0,
      suiteVersion: 0
    }));
  const fixtureCycles: ProjectCycleSummary[] = data.cycles
    .filter((cycle) => cycle.projectId === projectId)
    .map((cycle) => ({
      id: cycle.id,
      projectId,
      name: cycle.name,
      startDate: cycle.startDate,
      endDate: cycle.endDate,
      description: cycle.description,
      active: true,
      version: 0
    }));
  const suiteAssignments = data.fixtureMode
    ? fixtureAssignments
    : (suiteAssignmentsQuery.data?.assignments ?? []);
  const cycles = data.fixtureMode ? fixtureCycles : (cyclesQuery.data?.cycles ?? []);
  const memberships = data.fixtureMode
    ? (data.memberships[projectId] ?? [])
    : (projectAccessQuery.data?.memberships ?? []);
  const activeMembers = memberships.filter(
    (membership) => membership.membershipStatus === 'ACTIVE'
  );

  const context: AdhocSelectionContext | null =
    projectId && suiteAssignmentId && cycleId
      ? {
          projectId,
          projectSuiteAssignmentId: suiteAssignmentId,
          testCycleId: cycleId
        }
      : null;

  const testCasesQuery = useQuery({
    queryKey: ['test-cases-adhoc', data.authMode, context],
    enabled: Boolean(context) && !data.fixtureMode,
    queryFn: async () => {
      if (!context) {
        throw new Error('Select a project, test suite, and test cycle.');
      }
      const token = await acquireAccessToken();
      return getAdhocTestCases(token, context);
    }
  });

  const testCaseCapabilities = useMemo(() => {
    const merged = new Set(data.fixtureMode ? capabilities : []);
    for (const capability of projectAccessQuery.data?.capabilities ?? []) {
      merged.add(capability);
    }
    if (data.session.globalAdministrator) {
      return new Set(allCapabilities);
    }
    return merged;
  }, [
    capabilities,
    data.fixtureMode,
    data.session.globalAdministrator,
    projectAccessQuery.data?.capabilities
  ]);
  const canCreate = canAccess(testCaseCapabilities, ['TEST_CASE_CREATE']);
  const canEdit = canAccess(testCaseCapabilities, ['TEST_CASE_EDIT']);
  const canAssign = canAccess(testCaseCapabilities, ['TEST_CASE_ASSIGN']);
  const canDelete = canAccess(testCaseCapabilities, ['TEST_CASE_DELETE_DRAFT']);
  const canUpload = canCreate;
  const testCases = useMemo(
    () => (data.fixtureMode ? [] : (testCasesQuery.data?.testCases ?? [])),
    [data.fixtureMode, testCasesQuery.data?.testCases]
  );

  useEffect(() => {
    setSuiteAssignmentId('');
    setCycleId('');
    setFeedback(null);
  }, [projectId]);

  useEffect(() => {
    setEdits((current) => {
      const next = { ...current };
      for (const testCase of testCases) {
        next[testCase.id] ??= editStateFromTestCase(testCase);
      }
      return next;
    });
  }, [testCases]);

  const invalidateTestCases = () => {
    void queryClient.invalidateQueries({ queryKey: ['test-cases-adhoc'] });
  };
  const manualMutation = useMutation({
    mutationFn: async () => {
      if (!context) {
        throw new Error('Select a project, test suite, and test cycle.');
      }
      const token = await acquireAccessToken();
      return createAdhocManualTestCase(token, {
        ...context,
        header: manualHeader.trim(),
        description: manualDescription.trim()
      });
    },
    onSuccess: (created) => {
      setManualOpen(false);
      setManualHeader('');
      setManualDescription('');
      setFeedback(`${created.testCaseId} was saved as Draft with no ReqID.`);
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(
        error instanceof Error ? error.message : 'Manual ad hoc test case could not be saved.'
      );
    }
  });
  const csvMutation = useMutation({
    mutationFn: async (file: File) => {
      if (!context) {
        throw new Error('Select a project, test suite, and test cycle.');
      }
      const token = await acquireAccessToken();
      return importAdhocTestCasesCsv(token, context, file);
    },
    onSuccess: (result) => {
      setFeedback(`${String(result.importedCount)} ad hoc CSV test case(s) imported as Draft.`);
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : 'CSV import failed.');
    }
  });
  const updateMutation = useMutation({
    mutationFn: async (testCase: TestCaseSummary) => {
      const edit = edits[testCase.id];
      const token = await acquireAccessToken();
      return updateTestCase(token, projectId, testCase.id, testCase.version, {
        header: edit?.header.trim() ?? testCase.header,
        description: edit?.description.trim() ?? testCase.description,
        assigneeMembershipId:
          edit?.assigneeMembershipId && edit.assigneeMembershipId.length > 0
            ? edit.assigneeMembershipId
            : null,
        dueDate: edit?.dueDate && edit.dueDate.length > 0 ? edit.dueDate : null,
        status: edit?.status ?? testCase.status
      });
    },
    onSuccess: () => {
      setFeedback('Test case updated.');
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : 'Test case could not be updated.');
    }
  });
  const deleteMutation = useMutation({
    mutationFn: async (testCase: TestCaseSummary) => {
      const token = await acquireAccessToken();
      await deleteTestCase(token, projectId, testCase.id, testCase.version);
    },
    onSuccess: () => {
      setFeedback('Draft test case deleted.');
      invalidateTestCases();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : 'Only Draft test cases can be deleted.');
    }
  });

  const updateEdit = (testCaseId: string, patch: Partial<TestCaseEditState>) => {
    setEdits((current) => ({
      ...current,
      [testCaseId]: {
        header: current[testCaseId]?.header ?? '',
        description: current[testCaseId]?.description ?? '',
        assigneeMembershipId: current[testCaseId]?.assigneeMembershipId ?? '',
        dueDate: current[testCaseId]?.dueDate ?? '',
        status: current[testCaseId]?.status ?? 'Draft',
        ...patch
      }
    }));
  };

  const downloadSample = () => {
    const link = document.createElement('a');
    const blob = new Blob(
      [
        'Test Case Header,Description\r\nValidate employee clock-in,Confirm an active employee can clock in successfully.\r\n'
      ],
      { type: 'text/csv' }
    );
    link.href = URL.createObjectURL(blob);
    link.download = 'adhoc-test-case-upload-sample.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const busy =
    manualMutation.isPending ||
    csvMutation.isPending ||
    updateMutation.isPending ||
    deleteMutation.isPending;
  const editingTestCase = testCases.find((testCase) => testCase.id === editingTestCaseId) ?? null;
  const editingState = editingTestCase
    ? (edits[editingTestCase.id] ?? editStateFromTestCase(editingTestCase))
    : null;

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={3}>
        <Alert severity="info">
          Ad hoc test cases created here are not linked to any requirement, so ReqID remains blank.
        </Alert>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
            <FormControl fullWidth required>
              <InputLabel id="adhoc-project-label">Project</InputLabel>
              <Select
                labelId="adhoc-project-label"
                label="Project"
                value={projectId}
                onChange={(event) => {
                  setProjectId(event.target.value);
                }}
              >
                {data.projects.projects.map((project) => (
                  <MenuItem key={project.id} value={project.id}>
                    {project.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth required disabled={suiteAssignments.length === 0}>
              <InputLabel id="adhoc-suite-label">Test Suite</InputLabel>
              <Select
                labelId="adhoc-suite-label"
                label="Test Suite"
                value={suiteAssignmentId}
                onChange={(event) => {
                  setSuiteAssignmentId(event.target.value);
                }}
              >
                {suiteAssignments.map((suite) => (
                  <MenuItem key={suite.id} value={suite.id}>
                    {suite.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth required disabled={cycles.length === 0}>
              <InputLabel id="adhoc-cycle-label">Test Cycle</InputLabel>
              <Select
                labelId="adhoc-cycle-label"
                label="Test Cycle"
                value={cycleId}
                onChange={(event) => {
                  setCycleId(event.target.value);
                }}
              >
                {cycles.map((cycle) => (
                  <MenuItem key={cycle.id} value={cycle.id}>
                    {cycle.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </Paper>
        {feedback && (
          <Alert
            severity={
              feedback.includes('failed') || feedback.includes('could') ? 'error' : 'success'
            }
          >
            {feedback}
          </Alert>
        )}
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} justifyContent="flex-end">
          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            disabled={!context || !canCreate || busy || data.fixtureMode}
            onClick={() => {
              setManualOpen(true);
            }}
          >
            Add Manually
          </Button>
          <Button variant="outlined" startIcon={<DownloadOutlinedIcon />} onClick={downloadSample}>
            CSV Sample
          </Button>
          <Button
            component="label"
            variant="outlined"
            startIcon={<UploadFileOutlinedIcon />}
            disabled={!context || !canUpload || busy || data.fixtureMode}
          >
            Upload from CSV
            <input
              hidden
              type="file"
              accept=".csv,text/csv"
              onChange={(event) => {
                const file = event.target.files?.[0];
                event.target.value = '';
                if (file) {
                  setFeedback(null);
                  csvMutation.mutate(file);
                }
              }}
            />
          </Button>
        </Stack>
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Ad hoc test cases table">
            <TableHead>
              <TableRow>
                <TableCell>Test Case ID</TableCell>
                <TableCell>Test Case Header</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Assign To</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {testCasesQuery.isLoading && !data.fixtureMode && (
                <TableRow>
                  <TableCell colSpan={7}>
                    <CircularProgress size={24} aria-label="Loading ad hoc test cases" />
                  </TableCell>
                </TableRow>
              )}
              {!testCasesQuery.isLoading && testCases.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>
                    No ad hoc test cases found for the selected suite and cycle.
                  </TableCell>
                </TableRow>
              )}
              {testCases.map((testCase) => (
                <TableRow key={testCase.id} hover>
                  <TableCell>{testCase.testCaseId}</TableCell>
                  <TableCell>
                    <Typography fontWeight={700}>{testCase.header}</Typography>
                  </TableCell>
                  <TableCell>{testCase.description}</TableCell>
                  <TableCell>{testCase.status}</TableCell>
                  <TableCell>{testCase.assigneeName ?? 'Unassigned'}</TableCell>
                  <TableCell>{testCase.dueDate ?? '-'}</TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={1}>
                      <Button
                        size="small"
                        variant="outlined"
                        disabled={busy || !canEdit}
                        onClick={() => {
                          setFeedback(null);
                          setEdits((current) => ({
                            ...current,
                            [testCase.id]: current[testCase.id] ?? editStateFromTestCase(testCase)
                          }));
                          setEditingTestCaseId(testCase.id);
                        }}
                      >
                        Edit
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        disabled={busy || !canDelete || testCase.status !== 'Draft'}
                        onClick={() => {
                          setFeedback(null);
                          deleteMutation.mutate(testCase);
                        }}
                      >
                        Delete
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Stack>
      <TestCaseEditDialog
        open={Boolean(editingTestCase)}
        testCase={editingTestCase}
        edit={editingState}
        activeMembers={activeMembers}
        canAssign={canAssign}
        busy={busy}
        onChange={(patch) => {
          if (editingTestCase) {
            updateEdit(editingTestCase.id, patch);
          }
        }}
        onClose={() => {
          setEditingTestCaseId(null);
        }}
        onSubmit={() => {
          if (editingTestCase) {
            setFeedback(null);
            updateMutation.mutate(editingTestCase, {
              onSuccess: () => {
                setEditingTestCaseId(null);
              }
            });
          }
        }}
      />
      <Dialog
        open={manualOpen}
        onClose={() => {
          setManualOpen(false);
        }}
        fullWidth
        maxWidth="sm"
        aria-labelledby="manual-adhoc-test-case-dialog-title"
      >
        <Box
          component="form"
          onSubmit={(event: FormEvent) => {
            event.preventDefault();
            setFeedback(null);
            manualMutation.mutate();
          }}
        >
          <DialogTitle id="manual-adhoc-test-case-dialog-title">
            Add Manual Ad Hoc Test Case
          </DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ pt: 1 }}>
              <TextField
                label="Test Case Header"
                required
                fullWidth
                value={manualHeader}
                slotProps={{ htmlInput: { maxLength: 300 } }}
                onChange={(event) => {
                  setManualHeader(event.target.value);
                }}
              />
              <TextField
                label="Test Case Description"
                required
                fullWidth
                multiline
                minRows={4}
                value={manualDescription}
                onChange={(event) => {
                  setManualDescription(event.target.value);
                }}
              />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                setManualOpen(false);
              }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={
                !manualHeader.trim() || !manualDescription.trim() || manualMutation.isPending
              }
            >
              Save Draft
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </PageFrame>
  );
}

function csvValue(value: string | null | undefined) {
  const text = value ?? '';
  return `"${text.replaceAll('"', '""')}"`;
}

function downloadBlob(filename: string, blob: Blob) {
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}

function exportTestCasesCsv(testCases: TestCaseSummary[], filename: string) {
  const headers = testCaseExportHeaders();
  const rows = testCases.map((testCase) => testCaseExportRow(testCase));
  const csv = [headers, ...rows]
    .map((rowItems) => rowItems.map((value) => csvValue(value)).join(','))
    .join('\r\n');
  downloadBlob(filename, new Blob([csv], { type: 'text/csv' }));
}

function testCaseExportFilename(projectName: string, extension: 'csv' | 'pdf') {
  return `${sanitizeExportFilenamePart(projectName)}_TestCases_${aestTimestamp()}.${extension}`;
}

function sanitizeExportFilenamePart(value: string) {
  return (
    value
      .trim()
      .replace(/[<>:"/\\|?*]+/g, ' ')
      .replace(/\s+/g, ' ')
      .slice(0, 120) || 'Project'
  );
}

function aestTimestamp(date = new Date()) {
  const parts = new Intl.DateTimeFormat('en-AU', {
    timeZone: 'Australia/Brisbane',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23'
  }).formatToParts(date);
  const valueByType = new Map(parts.map((part) => [part.type, part.value]));
  return `${valueByType.get('year') ?? '0000'}${valueByType.get('month') ?? '00'}${valueByType.get('day') ?? '00'}_${valueByType.get('hour') ?? '00'}${valueByType.get('minute') ?? '00'}${valueByType.get('second') ?? '00'}`;
}

function testCaseExportHeaders() {
  return [
    'Test Case ID',
    'Test Case Header',
    'Description',
    'ReqID',
    'Req Description',
    'Test Suite',
    'Test Cycle',
    'Project',
    'Status',
    'Assign To',
    'Due Date'
  ];
}

function testCaseExportRow(testCase: TestCaseSummary) {
  return [
    testCase.testCaseId,
    testCase.header,
    testCase.description,
    testCase.reqId ?? '',
    testCase.requirementDescription ?? '',
    testCase.suiteName,
    testCase.cycleName,
    testCase.projectName,
    testCase.status,
    testCase.assigneeName ?? '',
    testCase.dueDate ?? ''
  ];
}

function pdfText(value: string) {
  return value
    .replace(/[^\x20-\x7E]/g, '?')
    .replaceAll('\\', '\\\\')
    .replaceAll('(', '\\(')
    .replaceAll(')', '\\)');
}

interface PdfTableColumn {
  header: string;
  width: number;
}

interface PdfCell {
  lines: string[];
  width: number;
}

function exportTestCasesPdf(testCases: TestCaseSummary[], filename: string) {
  const pageWidth = 792;
  const pageHeight = 612;
  const margin = 24;
  const cellPadding = 3;
  const fontSize = 6;
  const headerFontSize = 6;
  const lineHeight = 8;
  const columns: PdfTableColumn[] = [
    { header: 'Test Case ID', width: 52 },
    { header: 'Test Case Header', width: 84 },
    { header: 'Description', width: 122 },
    { header: 'ReqID', width: 40 },
    { header: 'Req Description', width: 106 },
    { header: 'Test Suite', width: 60 },
    { header: 'Test Cycle', width: 54 },
    { header: 'Project', width: 80 },
    { header: 'Status', width: 46 },
    { header: 'Assign To', width: 56 },
    { header: 'Due Date', width: 44 }
  ];

  const tableRows = testCases.map((testCase) =>
    testCaseExportRow(testCase).map((value, index) =>
      pdfCell(value, columns[index]?.width ?? 60, fontSize, cellPadding)
    )
  );
  const streams: string[] = [];
  let stream = pdfPageHeader(pageWidth, pageHeight, margin, columns, headerFontSize, cellPadding);
  let cursorY = pageHeight - 74;

  for (const row of tableRows) {
    const rowHeight = Math.max(
      ...row.map((cell) => cell.lines.length * lineHeight + cellPadding * 2)
    );
    if (cursorY - rowHeight < margin) {
      streams.push(stream.join('\n'));
      stream = pdfPageHeader(pageWidth, pageHeight, margin, columns, headerFontSize, cellPadding);
      cursorY = pageHeight - 74;
    }
    stream.push(...pdfTableRow(margin, cursorY, row, rowHeight, fontSize, lineHeight, cellPadding));
    cursorY -= rowHeight;
  }

  if (tableRows.length === 0) {
    const emptyCells = [
      pdfCell(
        'No selected test cases.',
        columns.reduce((sum, column) => sum + column.width, 0),
        fontSize,
        cellPadding
      )
    ];
    stream.push(...pdfTableRow(margin, cursorY, emptyCells, 18, fontSize, lineHeight, cellPadding));
  }

  streams.push(stream.join('\n'));
  const pageObjectIds = streams.map((_, index) => 4 + index * 2);
  const objects = [
    '1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj',
    `2 0 obj << /Type /Pages /Kids [${pageObjectIds.map((id) => `${String(id)} 0 R`).join(' ')}] /Count ${String(pageObjectIds.length)} >> endobj`,
    '3 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj',
    ...streams.flatMap((stream, index) => {
      const pageObjectId = 4 + index * 2;
      const contentObjectId = pageObjectId + 1;
      return [
        `${String(pageObjectId)} 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 ${String(pageWidth)} ${String(pageHeight)}] /Resources << /Font << /F1 3 0 R >> >> /Contents ${String(contentObjectId)} 0 R >> endobj`,
        `${String(contentObjectId)} 0 obj << /Length ${String(stream.length)} >> stream\n${stream}\nendstream endobj`
      ];
    })
  ];
  let pdf = '%PDF-1.4\n';
  const offsets = [0];
  for (const object of objects) {
    offsets.push(pdf.length);
    pdf += `${object}\n`;
  }
  const xrefStart = pdf.length;
  pdf += `xref\n0 ${String(objects.length + 1)}\n0000000000 65535 f \n`;
  for (const offset of offsets.slice(1)) {
    pdf += `${String(offset).padStart(10, '0')} 00000 n \n`;
  }
  pdf += `trailer << /Size ${String(objects.length + 1)} /Root 1 0 R >>\nstartxref\n${String(xrefStart)}\n%%EOF`;
  downloadBlob(filename, new Blob([pdf], { type: 'application/pdf' }));
}

function pdfPageHeader(
  pageWidth: number,
  pageHeight: number,
  margin: number,
  columns: PdfTableColumn[],
  fontSize: number,
  cellPadding: number
) {
  const titleY = pageHeight - 28;
  const headerTop = pageHeight - 48;
  const headerHeight = 20;
  const headerCells = columns.map((column) =>
    pdfCell(column.header, column.width, fontSize, cellPadding)
  );
  return [
    `BT /F1 7 Tf ${String(pageWidth - margin - 110)} ${String(titleY)} Td (${pdfText(new Date().toLocaleDateString())}) Tj ET`,
    ...pdfTableRow(margin, headerTop, headerCells, headerHeight, fontSize, 8, cellPadding, true)
  ];
}

function pdfCell(value: string, width: number, fontSize: number, cellPadding: number): PdfCell {
  return {
    lines: wrapPdfCellText(value, width - cellPadding * 2, fontSize),
    width
  };
}

function pdfTableRow(
  startX: number,
  topY: number,
  cells: PdfCell[],
  height: number,
  fontSize: number,
  lineHeight: number,
  cellPadding: number,
  shaded = false
) {
  const commands: string[] = [];
  let x = startX;
  for (const cell of cells) {
    const bottomY = topY - height;
    if (shaded) {
      commands.push(
        `0.91 0.96 0.90 rg ${pdfNumber(x)} ${pdfNumber(bottomY)} ${pdfNumber(cell.width)} ${pdfNumber(height)} re f`
      );
    }
    commands.push(
      `0.72 0.78 0.72 RG ${pdfNumber(x)} ${pdfNumber(bottomY)} ${pdfNumber(cell.width)} ${pdfNumber(height)} re S`
    );
    cell.lines.forEach((line, index) => {
      const textY = topY - cellPadding - fontSize - index * lineHeight;
      if (textY > bottomY + cellPadding - 1) {
        commands.push(
          `0 0 0 rg BT /F1 ${String(fontSize)} Tf ${pdfNumber(x + cellPadding)} ${pdfNumber(textY)} Td (${pdfText(line)}) Tj ET`
        );
      }
    });
    x += cell.width;
  }
  return commands;
}

function wrapPdfCellText(value: string, availableWidth: number, fontSize: number) {
  const normalized = value.replace(/\s+/g, ' ').trim();
  if (!normalized) {
    return [''];
  }
  const maxChars = Math.max(4, Math.floor(availableWidth / (fontSize * 0.52)));
  const lines: string[] = [];
  let current = '';
  for (const word of normalized.split(' ')) {
    const fragments = splitLongPdfWord(word, maxChars);
    for (const fragment of fragments) {
      const candidate = current ? `${current} ${fragment}` : fragment;
      if (candidate.length <= maxChars) {
        current = candidate;
      } else {
        if (current) {
          lines.push(current);
        }
        current = fragment;
      }
    }
  }
  if (current) {
    lines.push(current);
  }
  return lines;
}

function splitLongPdfWord(word: string, maxChars: number) {
  if (word.length <= maxChars) {
    return [word];
  }
  const fragments: string[] = [];
  for (let index = 0; index < word.length; index += maxChars) {
    fragments.push(word.slice(index, index + maxChars));
  }
  return fragments;
}

function pdfNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function ViewExportTestCasesPage({
  definition,
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
}) {
  const { acquireAccessToken } = useShellAccess(data);
  const [projectId, setProjectId] = useState(data.projects.projects[0]?.id ?? '');
  const [suiteAssignmentId, setSuiteAssignmentId] = useState('');
  const [cycleId, setCycleId] = useState('');
  const [submittedFilters, setSubmittedFilters] = useState<{
    projectId: string;
    projectSuiteAssignmentId: string | null;
    testCycleId: string | null;
  } | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [requirementDetail, setRequirementDetail] = useState<TestCaseSummary | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const suiteAssignmentsQuery = useQuery({
    queryKey: ['view-export-suite-assignments', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectSuiteAssignments(token, projectId);
    }
  });
  const cyclesQuery = useQuery({
    queryKey: ['view-export-cycles', data.authMode, projectId],
    enabled: Boolean(projectId) && !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getProjectCycles(token, projectId);
    }
  });
  const testCasesQuery = useQuery({
    queryKey: ['test-cases-view-export', data.authMode, submittedFilters],
    enabled: Boolean(submittedFilters) && !data.fixtureMode,
    queryFn: async () => {
      if (!submittedFilters) {
        throw new Error('Select a project.');
      }
      const token = await acquireAccessToken();
      return getTestCases(token, submittedFilters);
    }
  });

  const fixtureAssignments: ProjectSuiteAssignmentSummary[] = data.suites
    .filter((suite) => suite.projectId === projectId)
    .map((suite) => ({
      id: suite.id,
      projectId,
      suiteId: suite.id,
      suiteKey: suite.name.toUpperCase().replaceAll(' ', '_'),
      name: suite.name,
      description: null,
      active: true,
      version: 0,
      suiteVersion: 0
    }));
  const fixtureCycles: ProjectCycleSummary[] = data.cycles
    .filter((cycle) => cycle.projectId === projectId)
    .map((cycle) => ({
      id: cycle.id,
      projectId,
      name: cycle.name,
      startDate: cycle.startDate,
      endDate: cycle.endDate,
      description: cycle.description,
      active: true,
      version: 0
    }));
  const suiteAssignments = data.fixtureMode
    ? fixtureAssignments
    : (suiteAssignmentsQuery.data?.assignments ?? []);
  const cycles = data.fixtureMode ? fixtureCycles : (cyclesQuery.data?.cycles ?? []);
  const testCases = data.fixtureMode ? [] : (testCasesQuery.data?.testCases ?? []);
  const selectedTestCases = testCases.filter((testCase) => selectedIds.has(testCase.id));
  const selectedProjectName =
    data.projects.projects.find((project) => project.id === projectId)?.name ?? 'Project';
  const canSearch = canAccess(capabilities, ['PROJECT_VIEW']);
  const canExport = selectedIds.size > 0;

  useEffect(() => {
    setSuiteAssignmentId('');
    setCycleId('');
    setSubmittedFilters(null);
    setSelectedIds(new Set());
    setFeedback(null);
  }, [projectId]);

  useEffect(() => {
    setSelectedIds(new Set());
  }, [submittedFilters]);

  const reset = () => {
    setProjectId(data.projects.projects[0]?.id ?? '');
    setSuiteAssignmentId('');
    setCycleId('');
    setSubmittedFilters(null);
    setSelectedIds(new Set());
    setFeedback(null);
  };

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={3}>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
            <FormControl fullWidth required>
              <InputLabel id="view-export-project-label">Project</InputLabel>
              <Select
                labelId="view-export-project-label"
                label="Project"
                value={projectId}
                onChange={(event) => {
                  setProjectId(event.target.value);
                }}
              >
                {data.projects.projects.map((project) => (
                  <MenuItem key={project.id} value={project.id}>
                    {project.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth disabled={suiteAssignments.length === 0}>
              <InputLabel id="view-export-suite-label">Test Suite</InputLabel>
              <Select
                labelId="view-export-suite-label"
                label="Test Suite"
                value={suiteAssignmentId}
                onChange={(event) => {
                  setSuiteAssignmentId(event.target.value);
                }}
              >
                <MenuItem value="">All test suites</MenuItem>
                {suiteAssignments.map((suite) => (
                  <MenuItem key={suite.id} value={suite.id}>
                    {suite.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth disabled={cycles.length === 0}>
              <InputLabel id="view-export-cycle-label">Test Cycle</InputLabel>
              <Select
                labelId="view-export-cycle-label"
                label="Test Cycle"
                value={cycleId}
                onChange={(event) => {
                  setCycleId(event.target.value);
                }}
              >
                <MenuItem value="">All test cycles</MenuItem>
                {cycles.map((cycle) => (
                  <MenuItem key={cycle.id} value={cycle.id}>
                    {cycle.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </Paper>
        {feedback && <Alert severity="info">{feedback}</Alert>}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} justifyContent="flex-end">
          <Button
            variant="outlined"
            startIcon={<SearchOutlinedIcon />}
            disabled={!projectId || !canSearch || data.fixtureMode}
            onClick={() => {
              setFeedback(null);
              setSubmittedFilters({
                projectId,
                projectSuiteAssignmentId: suiteAssignmentId || null,
                testCycleId: cycleId || null
              });
            }}
          >
            Search
          </Button>
          <Button variant="outlined" startIcon={<RestartAltOutlinedIcon />} onClick={reset}>
            Reset
          </Button>
          <Button
            variant="outlined"
            startIcon={<DownloadOutlinedIcon />}
            disabled={!canExport}
            onClick={() => {
              exportTestCasesPdf(
                selectedTestCases,
                testCaseExportFilename(selectedProjectName, 'pdf')
              );
              setFeedback(
                `${String(selectedTestCases.length)} selected test case(s) exported as PDF.`
              );
            }}
          >
            Export as PDF
          </Button>
          <Button
            variant="outlined"
            startIcon={<DownloadOutlinedIcon />}
            disabled={!canExport}
            onClick={() => {
              exportTestCasesCsv(
                selectedTestCases,
                testCaseExportFilename(selectedProjectName, 'csv')
              );
              setFeedback(
                `${String(selectedTestCases.length)} selected test case(s) exported as CSV.`
              );
            }}
          >
            Export as CSV
          </Button>
        </Stack>
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="View and export test cases table">
            <TableHead>
              <TableRow>
                <TableCell padding="checkbox">
                  <Checkbox
                    inputProps={{ 'aria-label': 'Select all test cases' }}
                    checked={testCases.length > 0 && selectedIds.size === testCases.length}
                    indeterminate={selectedIds.size > 0 && selectedIds.size < testCases.length}
                    onChange={(event) => {
                      setSelectedIds(
                        event.target.checked
                          ? new Set(testCases.map((testCase) => testCase.id))
                          : new Set()
                      );
                    }}
                  />
                </TableCell>
                <TableCell>Test Case ID</TableCell>
                <TableCell>Test Case Header</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>ReqID</TableCell>
                <TableCell>Req Description</TableCell>
                <TableCell>Test Suite</TableCell>
                <TableCell>Test Cycle</TableCell>
                <TableCell>Project</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Assign To</TableCell>
                <TableCell>Due Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {testCasesQuery.isLoading && (
                <TableRow>
                  <TableCell colSpan={12}>
                    <CircularProgress size={24} aria-label="Loading test cases" />
                  </TableCell>
                </TableRow>
              )}
              {!testCasesQuery.isLoading && testCases.length === 0 && (
                <TableRow>
                  <TableCell colSpan={12}>
                    {submittedFilters
                      ? 'No test cases found for the selected search criteria.'
                      : 'Run search to display test cases.'}
                  </TableCell>
                </TableRow>
              )}
              {testCases.map((testCase) => (
                <TableRow key={testCase.id} hover>
                  <TableCell padding="checkbox">
                    <Checkbox
                      inputProps={{ 'aria-label': `Select ${testCase.testCaseId}` }}
                      checked={selectedIds.has(testCase.id)}
                      onChange={(event) => {
                        const next = new Set(selectedIds);
                        if (event.target.checked) {
                          next.add(testCase.id);
                        } else {
                          next.delete(testCase.id);
                        }
                        setSelectedIds(next);
                      }}
                    />
                  </TableCell>
                  <TableCell>{testCase.testCaseId}</TableCell>
                  <TableCell>
                    <Typography fontWeight={700}>{testCase.header}</Typography>
                  </TableCell>
                  <TableCell>{testCase.description}</TableCell>
                  <TableCell>
                    {testCase.reqId && testCase.requirementId ? (
                      <Button
                        size="small"
                        variant="text"
                        onClick={() => {
                          setRequirementDetail(testCase);
                        }}
                      >
                        {testCase.reqId}
                      </Button>
                    ) : (
                      ''
                    )}
                  </TableCell>
                  <TableCell>{testCase.requirementDescription ?? ''}</TableCell>
                  <TableCell>{testCase.suiteName}</TableCell>
                  <TableCell>{testCase.cycleName}</TableCell>
                  <TableCell>{testCase.projectName}</TableCell>
                  <TableCell>{testCase.status}</TableCell>
                  <TableCell>{testCase.assigneeName ?? 'Unassigned'}</TableCell>
                  <TableCell>{testCase.dueDate ?? '-'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <Dialog
          open={Boolean(requirementDetail)}
          onClose={() => {
            setRequirementDetail(null);
          }}
          fullWidth
          maxWidth="sm"
        >
          <DialogTitle>
            {requirementDetail?.reqId
              ? `Requirement ${requirementDetail.reqId}`
              : 'Requirement Details'}
          </DialogTitle>
          <DialogContent>
            <Stack spacing={2}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Req Header
                </Typography>
                <Typography fontWeight={700}>
                  {requirementDetail?.requirementHeader ?? ''}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Description
                </Typography>
                <Typography>{requirementDetail?.requirementDescription ?? ''}</Typography>
              </Box>
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                setRequirementDetail(null);
              }}
            >
              Close
            </Button>
          </DialogActions>
        </Dialog>
      </Stack>
    </PageFrame>
  );
}

function SkeletonPage({
  definition,
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
}) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const columns = columnsFromRows(definition.rows);

  return (
    <PageFrame
      screenId={definition.screenId}
      title={definition.title}
      description={definition.description}
    >
      <Stack spacing={3}>
        <ProjectSuiteCycleSelectors
          projects={data.projects.projects}
          suites={data.suites}
          cycles={data.cycles}
        />
        {definition.key === 'requirements-add' && <ManualRequirementFields />}
        {definition.key === 'requirements-generate' && <UploadPanel />}
        {definition.key === 'test-cases-view-export' && (
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} justifyContent="flex-end">
            <Button variant="outlined" startIcon={<SearchOutlinedIcon />}>
              Search
            </Button>
            <Button variant="outlined" startIcon={<RestartAltOutlinedIcon />}>
              Reset
            </Button>
            <Button variant="outlined" startIcon={<DownloadOutlinedIcon />}>
              Export as PDF
            </Button>
            <Button variant="outlined" startIcon={<DownloadOutlinedIcon />}>
              Export as CSV
            </Button>
          </Stack>
        )}
        <ServerDataGrid
          ariaLabel={`${definition.title} grid`}
          columns={columns}
          rows={data.fixtureMode ? definition.rows : []}
          page={0}
          pageSize={5}
          total={data.fixtureMode ? definition.rows.length : 0}
          selectable={definition.key === 'test-cases-view-export'}
          emptyTitle="No records to display"
        />
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            disabled={!canAccess(capabilities, definition.required)}
            onClick={() => {
              setDialogOpen(true);
            }}
          >
            Prepare Action
          </Button>
          <Button variant="outlined" disabled>
            Save Draft
          </Button>
        </Stack>
      </Stack>
      <Dialog
        open={dialogOpen}
        onClose={() => {
          setDialogOpen(false);
        }}
        aria-labelledby="route-dialog-title"
      >
        <DialogTitle id="route-dialog-title">{definition.title}</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">No changes were saved.</Typography>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setDialogOpen(false);
            }}
          >
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </PageFrame>
  );
}

function PageFrame({
  screenId,
  title,
  description,
  action,
  children
}: {
  screenId: string;
  title: string;
  description: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <Stack spacing={3} data-screen-id={screenId}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
        <Stack spacing={0.75} sx={{ flexGrow: 1 }}>
          <Typography component="h1" variant="h4" fontWeight={800} color="text.primary">
            {title}
          </Typography>
          <Typography color="text.primary" maxWidth={920} sx={{ fontSize: '1rem' }}>
            {description}
          </Typography>
        </Stack>
        {action}
      </Stack>
      {children}
    </Stack>
  );
}

function StateCards({
  projectCount,
  userCount,
  suiteCount,
  cycleCount
}: {
  projectCount: number;
  userCount: number;
  suiteCount: number;
  cycleCount: number;
}) {
  return (
    <Stack direction={{ xs: 'column', lg: 'row' }} spacing={1.5}>
      <StateCard
        icon={<FolderOutlinedIcon />}
        title="Total Projects"
        count={projectCount}
        detail="Accessible"
      />
      <StateCard
        icon={<GroupsOutlinedIcon />}
        title="Total Users"
        count={userCount}
        detail="Across projects"
      />
      <StateCard
        icon={<AccountTreeOutlinedIcon />}
        title="Total Suites"
        count={suiteCount}
        detail="Across projects"
      />
      <StateCard
        icon={<CalendarMonthOutlinedIcon />}
        title="Total Cycles"
        count={cycleCount}
        detail="Across projects"
      />
    </Stack>
  );
}

function StateCard({
  icon,
  title,
  count,
  detail
}: {
  icon: ReactNode;
  title: string;
  count: number;
  detail: string;
}) {
  return (
    <Card
      variant="outlined"
      sx={{
        flex: 1,
        minWidth: 0,
        borderRadius: 2,
        minHeight: 106,
        display: 'flex',
        alignItems: 'center'
      }}
    >
      <CardContent sx={{ width: '100%', px: 1.5, py: 1.25, '&:last-child': { pb: 1.25 } }}>
        <Stack direction="row" spacing={1.25} alignItems="center">
          <Avatar
            sx={{
              width: 44,
              height: 44,
              bgcolor: '#eaf4ed',
              color: 'primary.main',
              '& .MuiSvgIcon-root': { fontSize: 25 }
            }}
          >
            {icon}
          </Avatar>
          <Box>
            <Typography variant="body2" color="text.primary" sx={{ fontSize: '0.8rem' }}>
              {title}
            </Typography>
            <Typography
              component="p"
              fontWeight={500}
              sx={{ fontSize: '1.8rem', lineHeight: 1.08 }}
            >
              {count}
            </Typography>
            <Typography variant="body2" color="text.primary" sx={{ fontSize: '0.8rem' }}>
              {detail}
            </Typography>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}

function ProjectSuiteCycleSelectors({
  projects,
  suites,
  cycles
}: {
  projects: ProjectSummary[];
  suites: SuiteFixture[];
  cycles: CycleFixture[];
}) {
  const [projectId, setProjectId] = useState(projects[0]?.id ?? '');
  const projectSuites = suites.filter((suite) => suite.projectId === projectId);
  const projectCycles = cycles.filter((cycle) => cycle.projectId === projectId);

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
        <FormControl fullWidth required>
          <InputLabel id="project-selector-label">Project</InputLabel>
          <Select
            labelId="project-selector-label"
            label="Project"
            value={projectId}
            onChange={(event) => {
              setProjectId(event.target.value);
            }}
          >
            {projects.map((project) => (
              <MenuItem key={project.id} value={project.id}>
                {project.name}
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>Project-first filtering is required.</FormHelperText>
        </FormControl>
        <FormControl fullWidth disabled={projectSuites.length === 0}>
          <InputLabel id="suite-selector-label">Test Suite</InputLabel>
          <Select labelId="suite-selector-label" label="Test Suite" value="">
            {projectSuites.map((suite) => (
              <MenuItem key={suite.id} value={suite.id}>
                {suite.name}
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>Options depend on the selected project.</FormHelperText>
        </FormControl>
        <FormControl fullWidth disabled={projectCycles.length === 0}>
          <InputLabel id="cycle-selector-label">Test Cycle</InputLabel>
          <Select labelId="cycle-selector-label" label="Test Cycle" value="">
            {projectCycles.map((cycle) => (
              <MenuItem key={cycle.id} value={cycle.id}>
                {cycle.name}
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>Cycle choices stay project-scoped.</FormHelperText>
        </FormControl>
      </Stack>
    </Paper>
  );
}

function ManualRequirementFields() {
  return (
    <Card variant="outlined">
      <CardContent>
        <Stack spacing={2}>
          <Typography variant="h6" component="h2">
            Requirement Draft
          </Typography>
          <TextField label="Header" required fullWidth aria-describedby="requirement-header-help" />
          <FormHelperText id="requirement-header-help">
            Header is required before submission.
          </FormHelperText>
          <TextField label="Description" required fullWidth multiline minRows={4} />
        </Stack>
      </CardContent>
    </Card>
  );
}

function UploadPanel() {
  return (
    <Card variant="outlined">
      <CardContent>
        <Stack spacing={2}>
          <Typography variant="h6" component="h2">
            Requirement Document
          </Typography>
          <Button component="label" variant="outlined" startIcon={<UploadFileOutlinedIcon />}>
            Choose PDF, DOCX, DOC, or CSV
            <input hidden type="file" accept=".pdf,.docx,.doc,.csv" />
          </Button>
          <Alert severity="info">
            Files are validated and processed through secure upload contracts.
          </Alert>
        </Stack>
      </CardContent>
    </Card>
  );
}

function ServerDataGrid({
  ariaLabel,
  columns,
  rows,
  page,
  pageSize,
  total,
  selectable = false,
  emptyTitle,
  title,
  hidePagination = false
}: {
  ariaLabel: string;
  columns: GridColumn[];
  rows: GridRow[];
  page: number;
  pageSize: number;
  total: number;
  selectable?: boolean;
  emptyTitle: string;
  title?: string;
  hidePagination?: boolean;
}) {
  const [sortKey, setSortKey] = useState(columns[0]?.key ?? '');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [selected, setSelected] = useState<Set<string>>(new Set());

  return (
    <Paper
      variant="outlined"
      sx={{ borderRadius: 2.5, overflow: 'hidden', boxShadow: '0 2px 8px rgba(18, 32, 58, 0.06)' }}
    >
      {title && (
        <Typography
          component="h2"
          variant="h6"
          color="primary.main"
          fontWeight={700}
          sx={{ px: 3, pt: 2.5, pb: 1.5 }}
        >
          {title}
        </Typography>
      )}
      <TableContainer sx={{ maxWidth: '100%', overflowX: 'auto' }}>
        <Table aria-label={ariaLabel} size="medium">
          <TableHead>
            <TableRow>
              {selectable && (
                <TableCell padding="checkbox">
                  <Checkbox
                    inputProps={{ 'aria-label': 'Select all rows' }}
                    checked={rows.length > 0 && selected.size === rows.length}
                    indeterminate={selected.size > 0 && selected.size < rows.length}
                    onChange={(event) => {
                      setSelected(
                        event.target.checked
                          ? new Set(rows.map((rowItem) => rowItem.id))
                          : new Set()
                      );
                    }}
                  />
                </TableCell>
              )}
              {columns.map((column) => (
                <TableCell key={column.key}>
                  {column.sortable ? (
                    <TableSortLabel
                      active={sortKey === column.key}
                      direction={sortKey === column.key ? sortDirection : 'asc'}
                      onClick={() => {
                        setSortKey(column.key);
                        setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
                      }}
                    >
                      {column.label}
                    </TableSortLabel>
                  ) : (
                    column.label
                  )}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + (selectable ? 1 : 0)}>
                  <Stack spacing={1} alignItems="center" sx={{ py: 5 }}>
                    <InfoOutlinedIcon color="primary" />
                    <Typography fontWeight={700}>{emptyTitle}</Typography>
                    <Typography color="text.secondary">
                      Adjust filters or select another project.
                    </Typography>
                  </Stack>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((rowItem) => (
                <TableRow key={rowItem.id} hover>
                  {selectable && (
                    <TableCell padding="checkbox">
                      <Checkbox
                        inputProps={{ 'aria-label': `Select ${rowItem.id}` }}
                        checked={selected.has(rowItem.id)}
                        onChange={(event) => {
                          const next = new Set(selected);
                          if (event.target.checked) {
                            next.add(rowItem.id);
                          } else {
                            next.delete(rowItem.id);
                          }
                          setSelected(next);
                        }}
                      />
                    </TableCell>
                  )}
                  {columns.map((column) => (
                    <TableCell key={column.key}>{rowItem.cells[column.key] ?? '-'}</TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {!hidePagination && (
        <TablePagination
          component="div"
          count={total}
          page={page}
          rowsPerPage={pageSize}
          rowsPerPageOptions={[5, 10, 25]}
          onPageChange={() => undefined}
          onRowsPerPageChange={() => undefined}
        />
      )}
    </Paper>
  );
}

function columnsFromRows(rows: GridRow[]): GridColumn[] {
  const firstRow = rows[0];
  if (!firstRow) {
    return [
      { key: 'name', label: 'Name', sortable: true },
      { key: 'status', label: 'Status', sortable: true },
      { key: 'updated', label: 'Updated' }
    ];
  }
  return Object.keys(firstRow.cells).map((key) => ({
    key,
    label: key.replace(/([A-Z])/g, ' $1').replace(/^./, (value) => value.toUpperCase()),
    sortable: key.toLowerCase().includes('id') || key === 'header' || key === 'status'
  }));
}

function StatusChip({
  label,
  color
}: {
  label: string;
  color: 'default' | 'success' | 'warning' | 'info';
}) {
  if (color === 'default') {
    return <Chip size="small" label={label} variant="outlined" />;
  }
  return <Chip size="small" label={label} color={color} variant="filled" />;
}

function ForbiddenPage({ definition }: { definition: RouteDefinition }) {
  return (
    <PageFrame
      screenId={definition.screenId}
      title="Forbidden"
      description="The requested resource is not available."
    >
      <Alert severity="warning" icon={<LockOutlinedIcon />}>
        This route is not available for the current session.
      </Alert>
    </PageFrame>
  );
}

function NotFoundPage() {
  return (
    <PageFrame screenId="404" title="Not Found" description="The requested route is not available.">
      <Button component={NavLink} to="/projects" variant="contained">
        Go to Projects
      </Button>
    </PageFrame>
  );
}

function LogoutAction({ authMode }: { authMode: ShellData['authMode'] }) {
  const { accounts, instance } = useMsal();
  const account = instance.getActiveAccount() ?? accounts[0] ?? null;
  const { acquireAccessToken } = useAccessToken(instance, account);

  const logout = async () => {
    if (authMode === 'local') {
      await observeLogout(null);
      window.location.assign('/');
      return;
    }
    const token = await acquireAccessToken();
    if (token) {
      await observeLogout(token);
    }
    await instance.logoutRedirect({ account });
  };

  if (authMode === 'fixture') {
    return (
      <Chip icon={<SecurityIcon />} label="Fixture session" color="primary" variant="outlined" />
    );
  }

  if (authMode === 'sso' && !account) {
    return null;
  }

  return (
    <Button
      color="primary"
      startIcon={<LogoutIcon />}
      onClick={() => {
        void logout();
      }}
    >
      Sign out
    </Button>
  );
}

function HeaderAccount({ data }: { data: ShellData }) {
  const fullName = `${data.session.firstName} ${data.session.lastName}`.trim();
  const initials = `${data.session.firstName.charAt(0)}${data.session.lastName.charAt(0)}`
    .toUpperCase()
    .trim();
  const roleLabel = data.session.globalAdministrator ? 'Administrator' : data.projects.scopeLabel;

  return (
    <Stack
      direction="row"
      alignItems="center"
      sx={{ alignSelf: 'stretch', flexShrink: 0, pr: { xs: 1, sm: 3 } }}
    >
      <Avatar
        aria-label={`${fullName} profile`}
        sx={{
          width: 52,
          height: 52,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          fontSize: '1rem',
          fontWeight: 700
        }}
      >
        {initials}
      </Avatar>
      <Box sx={{ ml: 1.5, minWidth: 165, display: { xs: 'none', sm: 'block' } }}>
        <Typography variant="body1" fontWeight={700} noWrap>
          {fullName}
        </Typography>
        <Typography variant="body2" color="text.secondary" noWrap>
          {roleLabel}
        </Typography>
      </Box>
      <Divider orientation="vertical" flexItem sx={{ mx: { xs: 1, sm: 2.5 }, my: 1.75 }} />
      <LogoutAction authMode={data.authMode} />
    </Stack>
  );
}

export function App() {
  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <Routes>
        <Route path="/" element={<ProtectedRoute />} />
        <Route path="/auth/callback" element={<AuthCallback />} />
        <Route path="/auth/logout" element={<Navigate to="/" replace />} />
        <Route path="/access-denied" element={<LoginScreen />} />
        <Route path="/session-expired" element={<LoginScreen />} />
        <Route path="/*" element={<ProtectedRoute />} />
      </Routes>
    </ThemeProvider>
  );
}
