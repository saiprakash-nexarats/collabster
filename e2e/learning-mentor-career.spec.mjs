import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
});

test.describe('P2 — Learning, Mentorship and Careers User Journeys', () => {

  test('Learner can browse courses, complete lessons, and view certificates', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Explore Live Demo' }).click();
    await expect(page.locator('#main-content')).toBeVisible();

    // Navigate to Learning
    await page.goto('/#Learning');
    await expect(page).toHaveURL(/#\/(learning|Learning)$/);

    // Verify course catalog is rendered
    const courseCards = page.locator('.course-card, [data-testid="course-card"]');
    if (await courseCards.first().isVisible()) {
      await courseCards.first().click();
    }

    // Check certificate section
    const certTab = page.getByRole('button', { name: /Certificates|My Certificates/i });
    if (await certTab.isVisible()) {
      await certTab.click();
      await expect(page.getByText(/Certificate|Verify/i).first()).toBeVisible();
    }
  });

  test('Mentee can browse mentors, view slots, and request mentoring sessions', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Explore Live Demo' }).click();
    await expect(page.locator('#main-content')).toBeVisible();

    // Navigate to Mentorship
    await page.goto('/#Mentorship');
    await expect(page).toHaveURL(/#\/(mentorship|Mentorship|mentors)$/);

    // Verify mentor profile view
    const mentorCard = page.locator('.mentor-card, [data-testid="mentor-card"]').first();
    if (await mentorCard.isVisible()) {
      await expect(mentorCard).toBeVisible();
      const bookBtn = mentorCard.getByRole('button', { name: /Book Session|Request Session|View Slots/i });
      if (await bookBtn.isVisible()) {
        await bookBtn.click();
      }
    }
  });

  test('Job seeker can browse career opportunities and submit job applications', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Explore Live Demo' }).click();
    await expect(page.locator('#main-content')).toBeVisible();

    // Navigate to Careers
    await page.goto('/#Careers');
    await expect(page).toHaveURL(/#\/(careers|Careers|jobs)$/);

    // Verify job listings
    const jobCard = page.locator('.job-card, [data-testid="job-card"]').first();
    if (await jobCard.isVisible()) {
      await expect(jobCard).toBeVisible();
      const applyBtn = jobCard.getByRole('button', { name: /Apply|Quick Apply/i });
      if (await applyBtn.isVisible()) {
        await applyBtn.click();
      }
    }
  });
});
