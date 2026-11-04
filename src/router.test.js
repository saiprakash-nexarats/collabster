import { describe, it, expect } from 'vitest';
import { parseRoute, formatRoute, resolvePage, VALID_PAGES, ROUTE_DEFS, ENTITY_ROUTES } from './router.js';
import { isDuplicateEvent, frame, parseRealtimeFrames } from './realtime.js';

describe('Phase 3 Router - Canonical Route Definitions', () => {
  it('covers all 16 required application views', () => {
    expect(VALID_PAGES).toHaveLength(16);
    expect(VALID_PAGES).toEqual([
      'Home',
      'Explore',
      'Learn',
      'Career',
      'Mentor',
      'Projects',
      'Company',
      'Investment',
      'Local services',
      'Events',
      'Community',
      'Messages',
      'Notifications',
      'Profile',
      'Saved',
      'Settings'
    ]);
  });

  it('correctly maps every primary canonical route', () => {
    const testCases = [
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

    for (const [hash, expectedPage] of testCases) {
      const parsed = parseRoute(hash);
      expect(parsed.page).toBe(expectedPage);
      expect(parsed.canonicalHash).toBe(hash);
      expect(parsed.isNotFound).toBe(false);
      expect(parsed.isLegacy).toBe(false);
      expect(resolvePage(hash)).toBe(expectedPage);
    }
  });

  it('correctly formats canonical routes with formatRoute', () => {
    expect(formatRoute('Home')).toBe('#/feed');
    expect(formatRoute('Explore')).toBe('#/explore');
    expect(formatRoute('Learn')).toBe('#/learning');
    expect(formatRoute('Events')).toBe('#/events');
    expect(formatRoute('Community')).toBe('#/communities');
    expect(formatRoute('Messages')).toBe('#/messages');
  });
});

describe('Phase 3 Router - Entity Deep Links', () => {
  it('parses conversation deep links', () => {
    const parsed = parseRoute('#/messages/conv-uuid-1234');
    expect(parsed.page).toBe('Messages');
    expect(parsed.entityType).toBe('conversation');
    expect(parsed.entityId).toBe('conv-uuid-1234');
    expect(parsed.canonicalHash).toBe('#/messages/conv-uuid-1234');
    expect(parsed.isNotFound).toBe(false);
  });

  it('parses community deep links and normalizes alternate paths', () => {
    const parsed = parseRoute('#/communities/comm-789');
    expect(parsed.page).toBe('Community');
    expect(parsed.entityType).toBe('community');
    expect(parsed.entityId).toBe('comm-789');

    const parsedAlt = parseRoute('#/community/comm-789');
    expect(parsedAlt.page).toBe('Community');
    expect(parsedAlt.entityType).toBe('community');
    expect(parsedAlt.canonicalHash).toBe('#/communities/comm-789');
    expect(parsedAlt.isLegacy).toBe(true);
  });

  it('parses event deep links', () => {
    const parsed = parseRoute('#/events/event-meetup-99');
    expect(parsed.page).toBe('Events');
    expect(parsed.entityType).toBe('event');
    expect(parsed.entityId).toBe('event-meetup-99');
  });

  it('parses job deep links', () => {
    const parsed = parseRoute('#/career/job-lead-dev');
    expect(parsed.page).toBe('Career');
    expect(parsed.entityType).toBe('job');
    expect(parsed.entityId).toBe('job-lead-dev');
  });

  it('parses project deep links', () => {
    const parsed = parseRoute('#/projects/proj-collobster-mobile');
    expect(parsed.page).toBe('Projects');
    expect(parsed.entityType).toBe('project');
    expect(parsed.entityId).toBe('proj-collobster-mobile');
  });

  it('parses course deep links', () => {
    const parsed = parseRoute('#/learning/course-flutter');
    expect(parsed.page).toBe('Learn');
    expect(parsed.entityType).toBe('course');
    expect(parsed.entityId).toBe('course-flutter');
  });

  it('parses profile deep links', () => {
    const parsed = parseRoute('#/profile/user-ananya');
    expect(parsed.page).toBe('Profile');
    expect(parsed.entityType).toBe('profile');
    expect(parsed.entityId).toBe('user-ananya');
  });

  it('formats entity deep links correctly', () => {
    expect(formatRoute('Messages', 'conv-1')).toBe('#/messages/conv-1');
    expect(formatRoute('Community', 'comm-1')).toBe('#/communities/comm-1');
    expect(formatRoute('Events', 'evt-1')).toBe('#/events/evt-1');
    expect(formatRoute('Learn', 'course-1')).toBe('#/learning/course-1');
  });
});

describe('Phase 3 Router - Legacy Bookmark Redirects', () => {
  it('redirects legacy bookmark routes to canonical URLs', () => {
    const legacyCases = [
      ['#Home', 'Home', '#/feed'],
      ['#/Home', 'Home', '#/feed'],
      ['#Explore', 'Explore', '#/explore'],
      ['#Learn', 'Learn', '#/learning'],
      ['#/learn', 'Learn', '#/learning'],
      ['#Career', 'Career', '#/career'],
      ['#Mentor', 'Mentor', '#/mentorship'],
      ['#Projects', 'Projects', '#/projects'],
      ['#Company', 'Company', '#/companies'],
      ['#Investment', 'Investment', '#/investments'],
      ['#Local services', 'Local services', '#/services'],
      ['#Local%20services', 'Local services', '#/services'],
      ['#/local-services', 'Local services', '#/services'],
      ['#Events', 'Events', '#/events'],
      ['#Community', 'Community', '#/communities'],
      ['#Messages', 'Messages', '#/messages'],
      ['#Notifications', 'Notifications', '#/notifications'],
      ['#Profile', 'Profile', '#/profile'],
      ['#Saved', 'Saved', '#/saved'],
      ['#Settings', 'Settings', '#/settings']
    ];

    for (const [legacyHash, expectedPage, canonicalHash] of legacyCases) {
      const parsed = parseRoute(legacyHash);
      expect(parsed.page).toBe(expectedPage);
      expect(parsed.canonicalHash).toBe(canonicalHash);
      expect(parsed.isLegacy).toBe(true);
    }
  });

  it('returns NotFound for invalid route hashes', () => {
    const invalidHashes = ['#/nonexistent-space', '#/bad/path/here', '#unknown'];
    for (const h of invalidHashes) {
      const parsed = parseRoute(h);
      expect(parsed.page).toBe('NotFound');
      expect(parsed.isNotFound).toBe(true);
      expect(resolvePage(h)).toBe('NotFound');
    }
  });
});

describe('Phase 3 Realtime - Deduplication & STOMP Protocol', () => {
  it('deduplicates identical events by ID', () => {
    const event = { id: 'evt-dup-1', type: 'message.created', text: 'Hello' };
    expect(isDuplicateEvent(event)).toBe(false);
    expect(isDuplicateEvent(event)).toBe(true);
    expect(isDuplicateEvent({ id: 'evt-dup-2', text: 'Another' })).toBe(false);
  });

  it('constructs well-formed STOMP frames', () => {
    const raw = frame('SUBSCRIBE', { id: 'sub-0', destination: '/user/queue/updates' });
    expect(raw).toContain('SUBSCRIBE\n');
    expect(raw).toContain('id:sub-0\n');
    expect(raw).toContain('destination:/user/queue/updates\n');
    expect(raw.endsWith('\0')).toBe(true);
  });

  it('parses STOMP frames correctly', () => {
    const stompMsg = 'MESSAGE\ndestination:/user/queue/updates\nmessage-id:101\n\n{"hello":"world"}\0';
    const parsed = parseRealtimeFrames(stompMsg);
    expect(parsed).toHaveLength(1);
    expect(parsed[0].command).toBe('MESSAGE');
    expect(parsed[0].headers.destination).toBe('/user/queue/updates');
    expect(parsed[0].body).toEqual({ hello: 'world' });
  });
});

