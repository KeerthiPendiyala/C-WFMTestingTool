import { expect, test } from '@playwright/test';

test('UI-01 login shell renders with SSO first', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Welcome Back' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Sign in with SSO' })).toBeVisible();
});

test('Administrator shell exposes all-project workspace', async ({ page }) => {
  await page.goto('/projects?shellRole=administrator');
  await expect(page.getByRole('heading', { name: 'All Projects' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create Project' })).toBeVisible();
  await expect(page.getByRole('table', { name: 'Project dashboard grid' })).toContainText(
    'Australian Broadcasting Corporation'
  );
  await expect(page.getByRole('table', { name: 'Project dashboard grid' })).toContainText('3');
  await expect(
    page.getByRole('navigation', { name: 'Primary' }).getByText('Pre Defined Test Cases')
  ).toBeVisible();
  await page.screenshot({ path: 'test-results/admin-shell.png', fullPage: true });
});

test('Test Manager shell uses assigned-project workspace', async ({ page }) => {
  await page.goto('/projects?shellRole=test-manager');
  await expect(page.getByRole('heading', { name: 'My Projects' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create Project' })).toHaveCount(0);
  await expect(page.getByText('Australian Broadcasting Corporation')).toBeVisible();
  await page.screenshot({ path: 'test-results/test-manager-shell.png', fullPage: true });
});

test('Manage Project & Users exposes details and assignments', async ({ page }) => {
  await page.goto(
    '/projects/users?projectId=4f4092d5-e1bb-4db5-905e-b0420f025e27&shellRole=administrator'
  );
  await expect(page.getByRole('heading', { name: 'Manage Project & Users' })).toBeVisible();
  await expect(page.getByText('Project Key: ABC')).toBeVisible();
  await page.getByRole('tab', { name: 'Assign Users' }).click();
  await expect(page.getByRole('table', { name: 'Project users table' })).toContainText(
    'Mina Manager'
  );
  await expect(page.getByRole('button', { name: 'Add User' })).toBeVisible();
  await page.screenshot({ path: 'test-results/project-users.png', fullPage: true });
});

test('UI-04 Manage Test Suites shows project assignments', async ({ page }) => {
  await page.goto('/test-suites?shellRole=test-manager');
  await expect(page.getByRole('heading', { name: 'Manage Test Suites' })).toBeVisible();
  await expect(page.getByRole('table', { name: 'Assigned suites table' })).toContainText(
    'Timekeeping'
  );
  await expect(page.getByRole('button', { name: 'Assign Suite' })).toBeVisible();
  await page.screenshot({ path: 'test-results/test-suites.png', fullPage: true });
});

test('UI-05 Manage Test Cycles shows project cycles', async ({ page }) => {
  await page.goto('/test-cycles?shellRole=test-manager');
  await expect(page.getByRole('heading', { name: 'Manage Test Cycles' })).toBeVisible();
  await expect(page.getByRole('table', { name: 'Test cycles table' })).toContainText(
    'Cycle 1 - Timekeeping Baseline'
  );
  await expect(page.getByRole('button', { name: 'Create Cycle' })).toBeVisible();
  await page.screenshot({ path: 'test-results/test-cycles.png', fullPage: true });
});

test('UI-13 remains usable at a narrower viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 900 });
  await page.goto('/test-cases/view-export?shellRole=administrator');
  await expect(page.getByRole('heading', { name: 'View / Export Test Cases' })).toBeVisible();
  await page.getByRole('button', { name: 'Open navigation' }).click();
  await expect(page.getByText('View / Export').first()).toBeVisible();
  await page.waitForTimeout(350);
  await page.screenshot({ path: 'test-results/view-export-mobile.png', fullPage: true });
});
