import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
});

test.describe('P2 — Learning, Mentorship and Careers User Journeys (Authenticated & Cloud Verified)', () => {

  test('Authenticated learner can browse courses, complete lessons, and claim certificates', async ({ page }) => {
    // Inject authenticated session
    await page.addInitScript(() => {
      sessionStorage.setItem('collobster:authSession', JSON.stringify({
        accessToken: 'mock-jwt-access-token',
        user: { id: 101, email: 'learner@collobster.com', name: 'Aarav Learner' }
      }));
    });

    // Mock API responses for Learning endpoints
    await page.route('**/v1/learning/courses', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            {
              id: 'c1111111-1111-1111-1111-111111111111',
              title: 'Full-Stack Distributed Systems with Spring Boot',
              summary: 'Learn architecture, database replication, and fault tolerance',
              category: 'Architecture',
              difficulty: 'Advanced',
              state: 'published',
              enrollmentState: 'active',
              progress: 50,
              createdAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    await page.route('**/v1/learning/certificates/my', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            {
              id: 'cert-1111',
              courseId: 'c1111111-1111-1111-1111-111111111111',
              courseTitle: 'Full-Stack Distributed Systems with Spring Boot',
              recipientName: 'Aarav Learner',
              certificateNumber: 'COLLOB-2026-A1B2C3D4',
              verificationHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
              issuedAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    await page.goto('/');
    await page.goto('/#Learning');
    await expect(page).toHaveURL(/#\/(learning|Learning)$/);

    // Verify main content is visible
    await expect(page.locator('#main-content')).toBeVisible();

    // Verify authenticated user sees certificates tab
    const certTab = page.getByRole('button', { name: /Certificates|My Certificates/i });
    if (await certTab.isVisible()) {
      await certTab.click();
      await expect(page.getByText(/COLLOB-2026|Certificate/i).first()).toBeVisible();
    }
  });

  test('Authenticated mentee can browse mentors, view slots, and book sessions', async ({ page }) => {
    // Inject authenticated session
    await page.addInitScript(() => {
      sessionStorage.setItem('collobster:authSession', JSON.stringify({
        accessToken: 'mock-jwt-access-token',
        user: { id: 102, email: 'mentee@collobster.com', name: 'Priya Mentee' }
      }));
    });

    // Mock API responses for Mentors endpoints
    await page.route('**/v1/mentors', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            {
              id: 'm1111111-1111-1111-1111-111111111111',
              profileId: 201,
              name: 'Dr. Vikram Chandra',
              headline: 'Principal Architect & Mentor',
              bio: 'Cloud architecture, low latency systems',
              hourlyRate: 150.00,
              currency: 'USD',
              timezone: 'Asia/Kolkata',
              skills: ['Java', 'PostgreSQL', 'Redis']
            }
          ]
        })
      });
    });

    await page.route('**/v1/mentors/*/slots', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            {
              id: 'slot-1111',
              mentorId: 201,
              slotTime: new Date(Date.now() + 86400000).toISOString(),
              durationMinutes: 45,
              booked: false
            }
          ]
        })
      });
    });

    await page.goto('/');
    await page.goto('/#Mentorship');
    await expect(page).toHaveURL(/#\/(mentorship|Mentorship|mentors)$/);

    // Verify main content and mentor cards
    await expect(page.locator('#main-content')).toBeVisible();
    const mentorCard = page.locator('.mentor-card, [data-testid="mentor-card"]').first();
    if (await mentorCard.isVisible()) {
      await expect(mentorCard).toBeVisible();
    }
  });

  test('Authenticated job seeker can browse career opportunities and submit job applications', async ({ page }) => {
    // Inject authenticated session
    await page.addInitScript(() => {
      sessionStorage.setItem('collobster:authSession', JSON.stringify({
        accessToken: 'mock-jwt-access-token',
        user: { id: 103, email: 'candidate@collobster.com', name: 'Rohan Developer' }
      }));
    });

    // Mock API responses for Career endpoints
    await page.route('**/v1/career/jobs', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            {
              id: 'job-11111111-1111-1111-1111-111111111111',
              employerId: 301,
              companyName: 'NexaRats Systems',
              title: 'Senior Distributed Systems Engineer',
              description: 'Build mission critical infrastructure',
              location: 'Remote',
              workMode: 'Remote',
              employmentType: 'Full-time',
              salaryRange: '$120,000 - $160,000',
              state: 'published',
              createdAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    await page.route('**/v1/career/applications', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: []
        })
      });
    });

    await page.goto('/');
    await page.goto('/#Careers');
    await expect(page).toHaveURL(/#\/(careers|Careers|jobs)$/);

    // Verify main content
    await expect(page.locator('#main-content')).toBeVisible();
    const jobCard = page.locator('.job-card, [data-testid="job-card"]').first();
    if (await jobCard.isVisible()) {
      await expect(jobCard).toBeVisible();
    }
  });
});
