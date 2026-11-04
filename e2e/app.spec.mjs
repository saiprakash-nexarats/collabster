import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());
});

test('visitor can enter the interactive preview and navigate to Community', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: /Bring your ideas closer/i })).toBeVisible();

  await page.getByRole('button', { name: 'Explore Live Demo' }).click();
  // Phase 3 canonical SPA routing navigates to #/feed (canonical for Home)
  await expect(page).toHaveURL(/#\/(feed|Home)$/);
  await expect(page.locator('#main-content')).toBeVisible();

  // Test navigation to canonical community route #/communities
  await page.goto('/#/communities');
  await expect(page).toHaveURL(/#\/(communities|Community)$/);
  await expect(page.locator('#main-content')).toBeVisible();

  // Test legacy route redirection: navigating to legacy #Community redirects to canonical #/communities
  await page.goto('/#Community');
  await expect(page).toHaveURL(/#\/(communities|Community)$/);
  await expect(page.locator('#main-content')).toBeVisible();
});

test('mobile preview has no horizontal document overflow', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'Explore Live Demo' }).click();
  await expect(page.getByRole('main')).toBeVisible();

  const dimensions = await page.evaluate(() => ({ scrollWidth: document.documentElement.scrollWidth, viewportWidth: window.innerWidth }));
  expect(dimensions.scrollWidth).toBeLessThanOrEqual(dimensions.viewportWidth);
});
