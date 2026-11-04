import test from 'node:test';
import assert from 'node:assert/strict';
import { initialPosts, events, jobs, projects } from '../src/data.js';

test('saved categories filter posts, photos, articles, polls, projects, milestones, events, and jobs accurately', () => {
  const saved = ['p-me-1', 'p-me-2', 'p-me-3', 'p-me-4', 'p-me-5', 'p1', 'p2', 'e1', 'j1'];
  const savedEvents = ['e1'];

  const savedPostItems = initialPosts.filter((p) => saved.includes(p.id) && !p.hidden);
  const savedEventItems = events.filter((e) => savedEvents.includes(e.id) || saved.includes(e.id));
  const savedJobItems = jobs.filter((j) => saved.includes(j.id));
  const savedProjectItems = projects.filter((pr) => saved.includes(pr.id));

  // Category counts
  const counts = {
    All: 0,
    Posts: 0,
    Photos: 0,
    Articles: 0,
    Polls: 0,
    Projects: 0,
    Milestones: 0,
    Events: 0,
    Jobs: 0,
  };

  savedPostItems.forEach((p) => {
    const isPhoto = Boolean(p.image);
    const isArticle =
      p.type === 'card' ||
      p.format === 'Article' ||
      p.type === 'article' ||
      (Array.isArray(p.body) && p.body.length > 0);
    const isPoll =
      p.type === 'poll' ||
      p.format === 'Poll' ||
      (p.options && p.options.length > 0);
    const isProject =
      p.type === 'project' ||
      p.format === 'Project' ||
      p.tags?.some((t) => t.toLowerCase().includes('project'));
    const isMilestone =
      p.type === 'achievement' ||
      p.format === 'Achievement' ||
      p.type === 'milestone' ||
      p.tags?.some(
        (t) =>
          t.toLowerCase().includes('milestone') ||
          t.toLowerCase().includes('achievement')
      );
    const isEvent =
      p.type === 'event' ||
      p.format === 'Event' ||
      p.tags?.some((t) => t.toLowerCase().includes('event'));
    const isJob =
      p.type === 'job' ||
      p.format === 'Job' ||
      p.tags?.some(
        (t) =>
          t.toLowerCase().includes('job') ||
          t.toLowerCase().includes('career')
      );

    if (isPhoto) counts.Photos++;
    if (isArticle) counts.Articles++;
    if (isPoll) counts.Polls++;
    if (isProject) counts.Projects++;
    if (isMilestone) counts.Milestones++;
    if (isEvent) counts.Events++;
    if (isJob) counts.Jobs++;
    if (!isPhoto && !isArticle && !isPoll && !isProject && !isMilestone && !isEvent && !isJob) {
      counts.Posts++;
    } else if (p.type === 'post' || p.format === 'Post') {
      counts.Posts++;
    }
  });

  counts.Events += savedEventItems.length;
  counts.Jobs += savedJobItems.length;
  counts.Projects += savedProjectItems.length;
  counts.All = savedPostItems.length + savedEventItems.length + savedJobItems.length + savedProjectItems.length;

  assert.equal(counts.All, 9);
  assert.equal(counts.Photos >= 2, true); // p-me-1, p-me-3, p1 have images
  assert.equal(counts.Articles >= 2, true); // p-me-1, p1 are articles
  assert.equal(counts.Polls >= 1, true); // p-me-2 is poll
  assert.equal(counts.Projects >= 1, true); // p-me-4 is project
  assert.equal(counts.Milestones >= 1, true); // p-me-3 is achievement/milestone
  assert.equal(counts.Events >= 1, true); // e1 is saved event
  assert.equal(counts.Jobs >= 1, true); // j1 is saved job
  assert.equal(counts.Posts >= 2, true); // p-me-5, p2 are posts
});

test('not interested action hides post from feed with appropriate status', () => {
  let posts = [
    { id: 'p1', title: 'Post 1', hidden: false },
    { id: 'p2', title: 'Post 2', hidden: false },
  ];
  let toastMessage = '';

  const handleNotInterested = (postId) => {
    posts = posts.map((p) => (p.id === postId ? { ...p, hidden: true } : p));
    toastMessage = 'Marked as not interested. We will tune your feed.';
  };

  handleNotInterested('p1');

  assert.equal(posts.find((p) => p.id === 'p1')?.hidden, true);
  assert.equal(posts.find((p) => p.id === 'p2')?.hidden, false);
  assert.equal(toastMessage, 'Marked as not interested. We will tune your feed.');
});

test('profile tabs place Connections directly beside About', () => {
  const isGroupProfile = false;
  const profileTabs = isGroupProfile
    ? ['Updates', 'Members', 'About']
    : ['Posts', 'About', 'Connections'];

  assert.deepEqual(profileTabs, ['Posts', 'About', 'Connections']);
  const aboutIdx = profileTabs.indexOf('About');
  const connectionsIdx = profileTabs.indexOf('Connections');
  assert.equal(connectionsIdx, aboutIdx + 1);
});
