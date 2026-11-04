import {test, expect} from '@playwright/test';

test.beforeEach(async ({page}) => {
  await page.addInitScript(() => localStorage.clear());
});

test('visitor can enter the interactive preview and navigate to Community', async ({page}) => {
  await page.goto('/');
  await expect(page.getByRole('heading', {name: /Bring your ideas closer/i})).toBeVisible();

  await page.getByRole('button', {name: 'Explore Live Demo'}).click();
  await expect(page).toHaveURL(/#Home$/);
  await expect(page.locator('#main-content')).toBeVisible();

  await page.goto('/#Community');
  await expect(page).toHaveURL(/#Community$/);
  await expect(page.locator('#main-content')).toBeVisible();
});

test('mobile preview has no horizontal document overflow', async ({page}) => {
  await page.goto('/');
  await page.getByRole('button', {name: 'Explore Live Demo'}).click();
  await expect(page.getByRole('main')).toBeVisible();

  const dimensions = await page.evaluate(() => ({scrollWidth: document.documentElement.scrollWidth, viewportWidth: window.innerWidth}));
  expect(dimensions.scrollWidth).toBeLessThanOrEqual(dimensions.viewportWidth);
});
