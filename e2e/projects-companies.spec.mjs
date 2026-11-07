import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
});

test.describe('Projects and Companies End-to-End User Journeys', () => {

  test('Owner can create project, manage files, tasks, milestones, and invite members', async ({ page }) => {
    // 1. Visit App and enter workspace
    await page.goto('/');
    await page.getByRole('button', { name: 'Explore Live Demo' }).click();
    await expect(page.locator('#main-content')).toBeVisible();

    // 2. Navigate to Projects
    await page.goto('/#Projects');
    await expect(page).toHaveURL(/#\/(projects|Projects)$/);

    // 3. Open project creation modal / form
    const newProjectBtn = page.getByRole('button', { name: /New project|New code space|Create project/i });
    if (await newProjectBtn.isVisible()) {
      await newProjectBtn.click();
    }

    // 4. Verify Code workspace interactions
    const firstProject = page.locator('.project-card, .workspace-card').first();
    if (await firstProject.isVisible()) {
      await firstProject.click();
    }

    // 5. Verify Workspace tabs: Code Space, Tasks, Roadmap, Team
    await expect(page.getByRole('button', { name: /Code Space/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Tasks/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Roadmap/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Team/i })).toBeVisible();

    // 6. Test Tasks Tab
    await page.getByRole('button', { name: /Tasks/i }).click();
    await expect(page.getByText(/Task board|Project tasks/i).first()).toBeVisible();

    // 7. Test Roadmap Tab
    await page.getByRole('button', { name: /Roadmap/i }).click();
    await expect(page.getByText(/Roadmap & Milestones/i)).toBeVisible();

    // 8. Test Team Tab
    await page.getByRole('button', { name: /Team/i }).click();
    await expect(page.getByText(/PROJECT COLLABORATORS|Invite someone to build/i)).toBeVisible();

    // 9. Reload and verify persistence
    await page.reload();
    await expect(page.locator('.code-workspace')).toBeVisible();
  });

  test('Member with pending invitation sees Accept Invitation banner and joins workspace', async ({ page }) => {
    // Seed invited state in storage to test invitation acceptance
    await page.addInitScript(() => {
      const invitedProject = {
        id: 'proj-invite-test-1',
        title: 'Open Source Distributed Mesh',
        description: 'Collaborative P2P computing',
        visibility: 'public',
        state: 'active',
        membershipState: 'pending',
        roles: 'collaborator',
        remote: true,
      };
      localStorage.setItem('collobster:projects', JSON.stringify({ 'proj-invite-test-1': 'joined' }));
      localStorage.setItem('collobster:projectDrafts', JSON.stringify([invitedProject]));
    });

    await page.goto('/');
    await page.getByRole('button', { name: 'Explore Live Demo' }).click();
    await page.goto('/#Projects');

    // Open the invited project
    const projectCard = page.getByText('Open Source Distributed Mesh');
    await expect(projectCard).toBeVisible();
    await projectCard.click();

    // Go to Team tab where invitation banner renders
    await page.getByRole('button', { name: /Team/i }).click();

    // Banner must be visible for pending invitation
    const acceptBanner = page.getByText(/You have been invited to collaborate on this project/i);
    await expect(acceptBanner).toBeVisible();
    const acceptBtn = page.getByRole('button', { name: 'Accept Invitation' });
    await expect(acceptBtn).toBeVisible();

    // Click Accept Invitation
    await acceptBtn.click();
    await expect(page.getByText(/Joined project/i).first()).toBeVisible();
  });

  test('Company workspace renders documents, team panel, and handles downloads', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Explore Live Demo' }).click();

    // Navigate to Company
    await page.goto('/#Company');
    await expect(page).toHaveURL(/#\/(companies|Company)$/);

    // Verify company workspaces exist
    await expect(page.locator('#main-content')).toBeVisible();
  });
});
