import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import AddIcon from '@mui/icons-material/Add';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import AutoAwesomeOutlinedIcon from '@mui/icons-material/AutoAwesomeOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import ListAltOutlinedIcon from '@mui/icons-material/ListAltOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import LoginIcon from '@mui/icons-material/Login';
import LogoutIcon from '@mui/icons-material/Logout';
import NoteAddOutlinedIcon from '@mui/icons-material/NoteAddOutlined';
import RestartAltOutlinedIcon from '@mui/icons-material/RestartAltOutlined';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import SecurityIcon from '@mui/icons-material/Security';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import TableChartOutlinedIcon from '@mui/icons-material/TableChartOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
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
  Snackbar,
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
  createManualRequirement,
  createProjectCycle,
  createProject,
  createUser,
  deleteRequirement,
  deleteProjectCycle,
  deleteSuite,
  disableProjectMembership,
  getAuthSession,
  getProject,
  getProjectCycles,
  getProjectMemberships,
  getProjectSuiteAssignments,
  getHealth,
  getProjects,
  getRequirements,
  getSuites,
  getUsers,
  generateRequirementsFromDocument,
  localAdminLogin,
  observeLogout,
  unassignSuiteFromProject,
  updateProjectCycle,
  updateSuite,
  type AuthSessionResponse,
  type AccessPermission,
  type Capability,
  type ProjectCycleSummary,
  type ProjectListResponse,
  type ProjectMembershipSummary,
  type ProjectRole,
  type ProjectSuiteAssignmentSummary,
  type ProjectSummary,
  type RequirementSummary,
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
    title: 'View Requirements',
    description: 'Project-scoped requirement list with approval and deletion policy affordances.',
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
    title: 'Create Test Cases Through Requirements',
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
    title: 'Create Adhoc Test Cases',
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
        label: 'View Requirements',
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
  { label: 'Reports', path: '/reports', icon: DescriptionOutlinedIcon, required: ['REPORT_VIEW'] },
  { label: 'Users', path: '/users', icon: GroupsOutlinedIcon, required: ['USER_ACCESS_MANAGE'] },
  { label: 'Settings', path: '/settings', icon: SettingsOutlinedIcon, required: ['AUDIT_VIEW'] }
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
  const mainRef = useRef<HTMLElement | null>(null);
  const capabilities = useCapabilitySet(data);

  useEffect(() => {
    mainRef.current?.focus();
    setDrawerOpen(false);
  }, [location.pathname]);

  const drawer = (
    <ShellNavigation
      capabilities={capabilities}
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
          ml: { md: `${String(drawerWidth)}px` },
          width: { md: `calc(100% - ${String(drawerWidth)}px)` }
        }}
      >
        <Toolbar disableGutters sx={{ minHeight: designTokens.shell.appBarHeight }}>
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
              sx={{ fontSize: { xs: '1rem', sm: '1.25rem' } }}
            >
              Test Management Application
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
            width: { md: drawerWidth },
            flexShrink: { md: 0 },
            '& .MuiDrawer-paper': {
              width: drawerWidth,
              boxSizing: 'border-box',
              borderRight: `1px solid ${designTokens.color.border}`
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
          pt: `${String(designTokens.shell.appBarHeight + 24)}px`,
          px: { xs: 2, sm: 3, lg: 4 },
          pb: 4,
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
      <Snackbar
        open
        autoHideDuration={null}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert severity="info" icon={<InfoOutlinedIcon />} sx={{ alignItems: 'center' }}>
          Workspace ready.
        </Alert>
      </Snackbar>
    </Box>
  );
}

function ShellNavigation({
  capabilities,
  onNavigate
}: {
  capabilities: Set<Capability>;
  onNavigate: () => void;
}) {
  return (
    <Stack sx={{ height: '100%' }}>
      <Stack spacing={1} sx={{ p: 2.5 }}>
        <Typography variant="h6" component="p" fontWeight={800}>
          Smart QA Assure
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Test Management Tool
        </Typography>
      </Stack>
      <Divider />
      <List
        component="div"
        aria-label="Application routes"
        sx={{ flexGrow: 1, overflowY: 'auto', py: 1 }}
      >
        {navItems.map((item) => (
          <NavBranch
            key={item.label}
            item={item}
            capabilities={capabilities}
            onNavigate={onNavigate}
          />
        ))}
      </List>
    </Stack>
  );
}

function NavBranch({
  item,
  capabilities,
  onNavigate
}: {
  item: NavItem;
  capabilities: Set<Capability>;
  onNavigate: () => void;
}) {
  return (
    <>
      <NavEntry item={item} capabilities={capabilities} onNavigate={onNavigate} inset={false} />
      {item.children?.map((child) => (
        <NavEntry
          key={child.label}
          item={child}
          capabilities={capabilities}
          onNavigate={onNavigate}
          inset
        />
      ))}
    </>
  );
}

function NavEntry({
  item,
  capabilities,
  onNavigate,
  inset
}: {
  item: NavItem;
  capabilities: Set<Capability>;
  onNavigate: () => void;
  inset: boolean;
}) {
  const Icon = item.icon;
  const allowed = canAccess(capabilities, item.required);
  const button = (
    <ListItemButton
      component={allowed ? NavLink : 'div'}
      to={allowed ? item.path : undefined}
      onClick={allowed ? onNavigate : undefined}
      aria-disabled={!allowed}
      sx={{
        minHeight: 44,
        mx: 1,
        borderRadius: 1,
        pl: inset ? 4 : 2,
        color: allowed ? 'text.primary' : 'text.secondary',
        '&.active': {
          bgcolor: designTokens.color.brandSoft,
          color: 'primary.dark',
          fontWeight: 800
        }
      }}
    >
      <ListItemIcon sx={{ minWidth: 34, color: 'inherit' }}>
        {allowed ? <Icon fontSize="small" /> : <LockOutlinedIcon fontSize="small" />}
      </ListItemIcon>
      <ListItemText
        primary={item.label}
        slotProps={{
          primary: {
            fontWeight: inset ? 500 : 700,
            fontSize: inset ? '0.9rem' : '0.95rem'
          }
        }}
      />
    </ListItemButton>
  );

  return allowed ? button : <Tooltip title="Not available for this session">{button}</Tooltip>;
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
    return <ProjectUsersPage definition={definition} data={data} capabilities={capabilities} />;
  }
  if (definition.key === 'users') {
    return <UsersPage definition={definition} data={data} />;
  }
  if (definition.key === 'test-suites') {
    return <SuiteManagementPage definition={definition} data={data} capabilities={capabilities} />;
  }
  if (definition.key === 'test-cycles') {
    return <CycleManagementPage definition={definition} data={data} capabilities={capabilities} />;
  }
  if (definition.key.startsWith('requirements')) {
    return (
      <RequirementManagementPage definition={definition} data={data} capabilities={capabilities} />
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
    <PageFrame screenId="UI-02" title={data.projects.scopeLabel} description="Project Dashboard">
      <Stack spacing={3}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h6" component="h2">
              {data.projects.scopeLabel === 'All Projects'
                ? 'project view'
                : 'Assigned project view'}
            </Typography>
            <Typography color="text.secondary">
              {data.projects.projects.length} accessible project
              {data.projects.projects.length === 1 ? '' : 's'}
            </Typography>
          </Box>
          {data.projects.canCreateProject && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => {
                setDialogOpen(true);
              }}
            >
              Create Project
            </Button>
          )}
        </Stack>
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
  { value: 'MANAGE_ASSIGNMENTS', label: 'Manage Assignments' }
];

function UsersPage({ definition, data }: { definition: RouteDefinition; data: ShellData }) {
  const [drawerOpen, setDrawerOpen] = useState(false);
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
      projectIds: data.projects.projects.map((project) => project.id)
    }
  ];
  const userRows = data.fixtureMode ? fixtureUsers : (usersQuery.data?.users ?? []);

  const assignmentOptionsQuery = useQuery({
    queryKey: ['create-user-assignment-options', projectIds, accountKey],
    enabled: drawerOpen && projectIds.length > 0,
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

  const passwordValid =
    password.length >= 10 &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /\d/.test(password) &&
    /[^A-Za-z0-9]/.test(password);
  const canSubmit =
    firstName.trim().length > 0 &&
    lastName.trim().length > 0 &&
    email.trim().length > 0 &&
    passwordValid &&
    password === confirmPassword &&
    permissions.includes('VIEW') &&
    (role === 'ADMINISTRATOR' || projectIds.length > 0);

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
              </TableRow>
            </TableHead>
            <TableBody>
              {usersQuery.isLoading && !data.fixtureMode ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">
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
          if (!createMutation.isPending) {
            setDrawerOpen(false);
            resetForm();
          }
        }}
        PaperProps={{
          component: 'form',
          onSubmit: (event: FormEvent<HTMLFormElement>) => {
            event.preventDefault();
            setFormError(null);
            createMutation.mutate();
          },
          sx: { width: { xs: '100%', sm: 600, md: 680 }, maxWidth: '100%' }
        }}
      >
        <Stack sx={{ height: '100%' }}>
          <Box sx={{ px: { xs: 2, sm: 3 }, py: 2.5 }}>
            <Typography variant="h5" component="h2" fontWeight={800}>
              Create User
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 0.5 }}>
              Add login details, access scope and permissions.
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
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value);
                }}
                error={password.length > 0 && !passwordValid}
                helperText="10+ characters with upper, lower, number and special character."
                required
                fullWidth
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          aria-label={showPassword ? 'Hide password' : 'Show password'}
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
                label="Confirm Password"
                type={showConfirmPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(event) => {
                  setConfirmPassword(event.target.value);
                }}
                error={confirmPassword.length > 0 && password !== confirmPassword}
                helperText={
                  confirmPassword.length > 0 && password !== confirmPassword
                    ? 'Passwords do not match.'
                    : 'Re-enter the password.'
                }
                required
                fullWidth
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          aria-label={
                            showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'
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
                  setRole(event.target.value as UserRole);
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
              <FormHelperText>Select projects before choosing suites and cycles.</FormHelperText>
            </FormControl>
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
                        disabled={option.value === 'VIEW'}
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
              disabled={createMutation.isPending}
              onClick={() => {
                setDrawerOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={!canSubmit || createMutation.isPending}
            >
              {createMutation.isPending ? 'Creating…' : 'Create User'}
            </Button>
          </Stack>
        </Stack>
      </Drawer>
    </PageFrame>
  );
}

function ProjectUsersPage({
  definition,
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
}) {
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
  const canManageUsers = canAccess(capabilities, ['PROJECT_MANAGE_USERS']);

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
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
}) {
  const [projectId, setProjectId] = useState(data.projects.projects[0]?.id ?? '');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedSuiteId, setSelectedSuiteId] = useState('');
  const [editing, setEditing] = useState<ProjectSuiteAssignmentSummary | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const { accountKey, acquireAccessToken } = useShellAccess(data);
  const queryClient = useQueryClient();
  const canManage = canAccess(capabilities, ['PROJECT_MANAGE_SUITES']);
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
    queryKey: ['suites', accountKey],
    enabled: !data.fixtureMode,
    queryFn: async () => {
      const token = await acquireAccessToken();
      return getSuites(token);
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
              <FormControl fullWidth disabled={!canManage}>
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
                disabled={!canManage}
                fullWidth
              />
              <TextField
                label="Description"
                value={description}
                onChange={(event) => {
                  setDescription(event.target.value);
                }}
                disabled={!canManage}
                fullWidth
              />
              <Button
                type="submit"
                variant="contained"
                disabled={!canManage || saveMutation.isPending || name.trim().length === 0}
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
                          disabled={!canManage}
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
                          disabled={!canManage}
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
                          disabled={!canManage}
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
  data,
  capabilities
}: {
  definition: RouteDefinition;
  data: ShellData;
  capabilities: Set<Capability>;
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
  const canManage = canAccess(capabilities, ['PROJECT_MANAGE_CYCLES']);
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
                disabled={!canManage}
                fullWidth
              />
              <TextField
                label="Start Date"
                type="date"
                value={startDate}
                onChange={(event) => {
                  setStartDate(event.target.value);
                }}
                disabled={!canManage}
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
                disabled={!canManage}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                label="Description"
                value={description}
                onChange={(event) => {
                  setDescription(event.target.value);
                }}
                disabled={!canManage}
                fullWidth
              />
              <Button
                type="submit"
                variant="contained"
                disabled={!canManage || saveMutation.isPending || name.trim().length === 0}
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
                          disabled={!canManage}
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
                          disabled={!canManage}
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
  const navigate = useNavigate();
  const [projectId, setProjectId] = useState(data.projects.projects[0]?.id ?? '');
  const [suiteAssignmentId, setSuiteAssignmentId] = useState('');
  const [cycleId, setCycleId] = useState('');
  const [header, setHeader] = useState('');
  const [description, setDescription] = useState('');
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
  const requirements = data.fixtureMode ? [] : (requirementsQuery.data?.requirements ?? []);
  const requirementCapabilities = useMemo(() => {
    const merged = new Set(capabilities);
    for (const capability of projectAccessQuery.data?.capabilities ?? []) {
      merged.add(capability);
    }
    return merged;
  }, [capabilities, projectAccessQuery.data?.capabilities]);

  useEffect(() => {
    setSuiteAssignmentId('');
    setCycleId('');
    setFeedback(null);
  }, [projectId]);

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
            label="View Requirements"
          />
        </Tabs>
        {selectorPanel}
        {feedback && (
          <Alert severity={feedback.includes('could not') ? 'error' : 'success'}>{feedback}</Alert>
        )}
        {definition.key === 'requirements' && (
          <Alert severity="info">
            Choose Generate Requirements, Add Manually, or View Requirements to continue.
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
            loading={requirementsQuery.isLoading && !data.fixtureMode}
            canApprove={canAccess(requirementCapabilities, ['REQUIREMENT_APPROVE'])}
            canDelete={canAccess(requirementCapabilities, ['REQUIREMENT_DELETE_UNLINKED'])}
            busy={approveMutation.isPending || deleteMutation.isPending}
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

function RequirementTable({
  requirements,
  loading,
  canApprove,
  canDelete,
  busy,
  onApprove,
  onDelete
}: {
  requirements: RequirementSummary[];
  loading: boolean;
  canApprove: boolean;
  canDelete: boolean;
  busy: boolean;
  onApprove: (requirement: RequirementSummary) => void;
  onDelete: (requirement: RequirementSummary) => void;
}) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table aria-label="Requirements table">
        <TableHead>
          <TableRow>
            <TableCell>ReqID</TableCell>
            <TableCell>Header</TableCell>
            <TableCell>Suite</TableCell>
            <TableCell>Cycle</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Created</TableCell>
            <TableCell>Actions</TableCell>
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
            <TableRow key={requirement.id} hover>
              <TableCell>{requirement.reqId}</TableCell>
              <TableCell>
                <Typography fontWeight={700}>{requirement.header}</Typography>
                <Typography variant="body2" color="text.secondary">
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
                />
              </TableCell>
              <TableCell>{new Date(requirement.createdDate).toLocaleDateString()}</TableCell>
              <TableCell>
                <Stack direction="row" spacing={1}>
                  {canApprove && requirement.status === 'Draft' && (
                    <Button
                      size="small"
                      disabled={busy}
                      onClick={() => {
                        onApprove(requirement);
                      }}
                    >
                      Approve
                    </Button>
                  )}
                  {canDelete && (
                    <Button
                      size="small"
                      color="error"
                      disabled={busy}
                      onClick={() => {
                        onDelete(requirement);
                      }}
                    >
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
  children
}: {
  screenId: string;
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <Stack spacing={3} data-screen-id={screenId}>
      <Stack spacing={1}>
        <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center"></Stack>
        <Typography component="h1" variant="h4" fontWeight={800}>
          {title}
        </Typography>
        <Typography color="text.secondary" maxWidth={920}>
          {description}
        </Typography>
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
    <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
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
        borderRadius: 3,
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.05)'
      }}
    >
      <CardContent>
        <Stack direction="row" spacing={2} alignItems="center">
          <Avatar
            sx={{
              width: 52,
              height: 52,
              bgcolor: 'rgba(46, 125, 50, 0.10)',
              color: 'success.main'
            }}
          >
            {icon}
          </Avatar>
          <Box>
            <Typography variant="body2" color="text.secondary">
              {title}
            </Typography>
            <Typography variant="h4" component="p" fontWeight={800}>
              {count}
            </Typography>
            <Typography variant="body2" color="text.secondary">
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
  emptyTitle
}: {
  ariaLabel: string;
  columns: GridColumn[];
  rows: GridRow[];
  page: number;
  pageSize: number;
  total: number;
  selectable?: boolean;
  emptyTitle: string;
}) {
  const [sortKey, setSortKey] = useState(columns[0]?.key ?? '');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [selected, setSelected] = useState<Set<string>>(new Set());

  return (
    <Paper variant="outlined">
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
      <TablePagination
        component="div"
        count={total}
        page={page}
        rowsPerPage={pageSize}
        rowsPerPageOptions={[5, 10, 25]}
        onPageChange={() => undefined}
        onRowsPerPageChange={() => undefined}
      />
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
          width: 40,
          height: 40,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          fontSize: '0.875rem',
          fontWeight: 700
        }}
      >
        {initials}
      </Avatar>
      <Box sx={{ ml: 1.5, minWidth: 130, display: { xs: 'none', sm: 'block' } }}>
        <Typography variant="body2" fontWeight={700} noWrap>
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