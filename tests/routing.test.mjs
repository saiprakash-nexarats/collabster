import test from 'node:test';
import assert from 'node:assert/strict';
import { VALID_PAGES, resolvePage, parseRoute, formatRoute } from '../src/domain.mjs';

test('VALID_PAGES contains all core modules', () => {
  const expected = [
    'Home', 'Saved', 'Explore', 'Learn', 'Career', 'Mentor',
    'Projects', 'Company', 'Investment', 'Local services',
    'Events', 'Community', 'Messages', 'Notifications', 'Profile', 'Settings'
  ];

  for (const page of expected) {
    assert.ok(VALID_PAGES.includes(page), `VALID_PAGES should include ${page}`);
  }
});

test('resolvePage resolves hash routes including deep links and case insensitivity', () => {
  assert.equal(resolvePage(''), 'Home');
  assert.equal(resolvePage('#'), 'Home');
  assert.equal(resolvePage('#Home'), 'Home');
  assert.equal(resolvePage('#/feed'), 'Home');
  assert.equal(resolvePage('#learn'), 'Learn');
  assert.equal(resolvePage('#/learning'), 'Learn');
  assert.equal(resolvePage('#CAREER'), 'Career');
  assert.equal(resolvePage('#Mentor'), 'Mentor');
  assert.equal(resolvePage('#Projects'), 'Projects');
  assert.equal(resolvePage('#Company'), 'Company');
  assert.equal(resolvePage('#Local%20services'), 'Local services');
  assert.equal(resolvePage('#/services'), 'Local services');
  assert.equal(resolvePage('#nonexistent-page'), 'NotFound');
});

test('parseRoute parses all primary canonical routes', () => {
  const cases = [
    ['#/feed', 'Home'],
    ['#/explore', 'Explore'],
    ['#/learning', 'Learn'],
    ['#/career', 'Career'],
    ['#/mentorship', 'Mentor'],
    ['#/projects', 'Projects'],
    ['#/companies', 'Company'],
    ['#/investments', 'Investment'],
    ['#/services', 'Local services'],
    ['#/events', 'Events'],
    ['#/communities', 'Community'],
    ['#/messages', 'Messages'],
    ['#/notifications', 'Notifications'],
    ['#/profile', 'Profile'],
    ['#/saved', 'Saved'],
    ['#/settings', 'Settings']
  ];

  for (const [hash, expectedPage] of cases) {
    const route = parseRoute(hash);
    assert.equal(route.page, expectedPage, `Route for ${hash} should be ${expectedPage}`);
    assert.equal(route.canonicalHash, hash);
    assert.equal(route.isNotFound, false);
    assert.equal(route.isLegacy, false);
  }
});

test('parseRoute handles entity deep links and canonicalizes aliases', () => {
  const msgRoute = parseRoute('#/messages/conv-1234');
  assert.equal(msgRoute.page, 'Messages');
  assert.equal(msgRoute.entityType, 'conversation');
  assert.equal(msgRoute.entityId, 'conv-1234');

  const commRoute = parseRoute('#/communities/comm-5678');
  assert.equal(commRoute.page, 'Community');
  assert.equal(commRoute.entityType, 'community');
  assert.equal(commRoute.entityId, 'comm-5678');

  // Alternate legacy alias normalization: #/community/:id -> canonical #/communities/:id
  const commAlt = parseRoute('#/community/comm-5678');
  assert.equal(commAlt.page, 'Community');
  assert.equal(commAlt.entityType, 'community');
  assert.equal(commAlt.entityId, 'comm-5678');
  assert.equal(commAlt.canonicalHash, '#/communities/comm-5678');
  assert.equal(commAlt.isLegacy, true);

  const eventRoute = parseRoute('#/events/event-meetup-1');
  assert.equal(eventRoute.page, 'Events');
  assert.equal(eventRoute.entityType, 'event');
  assert.equal(eventRoute.entityId, 'event-meetup-1');

  const jobRoute = parseRoute('#/career/job-architect');
  assert.equal(jobRoute.page, 'Career');
  assert.equal(jobRoute.entityType, 'job');
  assert.equal(jobRoute.entityId, 'job-architect');

  const projRoute = parseRoute('#/projects/proj-collobster');
  assert.equal(projRoute.page, 'Projects');
  assert.equal(projRoute.entityType, 'project');
  assert.equal(projRoute.entityId, 'proj-collobster');

  const courseRoute = parseRoute('#/learning/course-flutter');
  assert.equal(courseRoute.page, 'Learn');
  assert.equal(courseRoute.entityType, 'course');
  assert.equal(courseRoute.entityId, 'course-flutter');

  const profileRoute = parseRoute('#/profile/user-ananya');
  assert.equal(profileRoute.page, 'Profile');
  assert.equal(profileRoute.entityType, 'profile');
  assert.equal(profileRoute.entityId, 'user-ananya');
});

test('parseRoute handles legacy bookmarks and marks isLegacy for redirection', () => {
  const legacyCases = [
    ['#Home', 'Home', '#/feed'],
    ['#Explore', 'Explore', '#/explore'],
    ['#Learn', 'Learn', '#/learning'],
    ['#Career', 'Career', '#/career'],
    ['#Mentor', 'Mentor', '#/mentorship'],
    ['#Projects', 'Projects', '#/projects'],
    ['#Company', 'Company', '#/companies'],
    ['#Investment', 'Investment', '#/investments'],
    ['#Local services', 'Local services', '#/services'],
    ['#Events', 'Events', '#/events'],
    ['#Community', 'Community', '#/communities'],
    ['#Messages', 'Messages', '#/messages'],
    ['#Notifications', 'Notifications', '#/notifications'],
    ['#Profile', 'Profile', '#/profile'],
    ['#Saved', 'Saved', '#/saved'],
    ['#Settings', 'Settings', '#/settings']
  ];

  for (const [legacy, expectedPage, canonical] of legacyCases) {
    const route = parseRoute(legacy);
    assert.equal(route.page, expectedPage);
    assert.equal(route.canonicalHash, canonical);
    assert.equal(route.isLegacy, true);
  }
});

test('formatRoute formats canonical routes and entity deep links', () => {
  assert.equal(formatRoute('Home'), '#/feed');
  assert.equal(formatRoute('Events'), '#/events');
  assert.equal(formatRoute('Community'), '#/communities');
  assert.equal(formatRoute('Messages', 'conv-123'), '#/messages/conv-123');
  assert.equal(formatRoute('Events', 'evt-456'), '#/events/evt-456');
});
