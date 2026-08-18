# UI Screen Map

The UI baseline uses a light green Material UI theme and the thirteen supplied screen concepts from `docs/source/UKG_QA_Test_Management_Implementation_V5.0 (1).docx`. Source image names refer to the embedded DOCX media inventory inspected during baseline creation.

## Navigation

Primary navigation order is Projects, Test Suites, Test Cycles, Requirements, Test Cases, Roles & Permissions, Users, and Audit Logs as role-appropriate.

Requirement Management contains:

- Generate Requirements
- Add Manually
- Manage Requirements

Test Cases contains:

- Through Requirements
- Adhoc Test Cases
- Pre Defined Test Cases
- Manage Pre Defined Test Cases
- View / Export

## Screens

| ID | Screen Concept | Source Image | Primary Requirements | Notes |
| --- | --- | --- | --- | --- |
| UI-01 | Login - SSO | `image2.png` | AUTH-01, AUTH-02, AUTH-03 | Shows Entra SSO plus password form. Password form is development/mock only and blocked in production. |
| UI-02 | Project Dashboard | `image3.png`, `image4.png` | RBAC-02, RBAC-03, PROJ-01 | Includes Test Manager assigned-project view and Administrator all-projects view with Create Project. |
| UI-03 | Manage Project & Users | `image5.png` | AUTH-01, RBAC-03, PROJ-01 | Pre-provision users with first name, last name, email, and project role. Seed examples: Australian Broadcasting Corporation users. |
| UI-04 | Manage Test Suites | `image6.png` | RBAC-03, RBAC-04, PROJ-01 | Project-scoped suites. Seed examples: Timekeeping, Integration, Personas. |
| UI-05 | Manage Test Cycles | `image7.png` | RBAC-03, RBAC-04, PROJ-01 | Project-scoped cycles with date range and description. |
| UI-06 | Manage Requirements | `image8.png` | REQ-01, UI-REQ-02 | Hub for Generate Requirements, Add Manually, and Manage Requirements after project/suite/cycle selection. |
| UI-07 | Upload Requirement Document / Generate Requirements | `image9.png` | REQ-01, REQ-02, NFR-02 | Supports any readable document format with secure upload and AI extraction. |
| UI-08 | Add Requirement Manually | `image10.png` | REQ-01, REQ-02 | Manual input is header, description, acceptance criteria, and dependencies; generated fields include ReqID, suite, cycle, date, status. |
| UI-09 | Manage Requirements | `image11.png` | REQ-02, REQ-03, REQ-04 | List and edit requirements for a selected project with optional suite/cycle filters; approve and delete actions are role/policy gated. |
| UI-10 | Manage Test Cases Through Requirements | `image12.png` | TC-01, TC-02, TC-03, TC-04, TC-05 | Select combined ReqID-header; generate by AI, add manually, upload CSV, and maintain linked test cases. |
| UI-11 | Manage Adhoc Test Cases | `image13.png` | TC-03, TC-04, TC-05, TC-06 | Manual or CSV creation and maintenance with ReqID null/blank. |
| UI-12 | Generate Pre Defined Test Cases | `image14.png` | P2-01, P2-02 | Test Manager/Admin selects one PD-prefixed Test Suite and downloads the created predefined templates as CSV with `Test Case Header,Description`. |
| UI-13 | View / Export Test Cases | `image15.png` | VIEW-01, VIEW-02, REPORT-01 | Project-first list with dependent suite/cycle filters, other filters, pagination, sorting, selection, CSV export, and PDF export. |
| UI-14 | Roles & Permissions | Product extension | RBAC-05, RBAC-06, RBAC-07 | Administrator-only role list and create/edit form with individual permission toggles and Select All. |
| UI-15 | Audit Logs | Product extension | NFR-01, NFR-02 | Independent navigation destination for recorded security and application activity. |
| UI-16 | Manage Pre Defined Test Cases | Product extension | P2-01, P2-02 | Suite-scoped predefined template creation and maintenance. Test Suite is mandatory and limited to suites whose display name starts with `PD-`; Test Case Header and Test Case Description are mandatory. |

## Theme And Accessibility

- Use Material UI with a light green theme consistent with the supplied screens.
- Use accessible labels, keyboard navigation, visible focus, sufficient contrast, and predictable tab order.
- Tables must support server-side pagination/sorting and responsive behavior without hiding authorization-critical state.
- Status display values must preserve the exact allowed enum values unless a separate display-label decision is approved.
