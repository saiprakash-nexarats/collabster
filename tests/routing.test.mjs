import test from 'node:test';
import assert from 'node:assert/strict';
import { VALID_PAGES, resolvePage } from '../src/domain.mjs';

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
  assert.equal(resolvePage('#learn'), 'Learn');
  assert.equal(resolvePage('#CAREER'), 'Career');
  assert.equal(resolvePage('#Mentor'), 'Mentor');
  assert.equal(resolvePage('#Projects'), 'Projects');
  assert.equal(resolvePage('#Company'), 'Company');
  assert.equal(resolvePage('#Local%20services'), 'Local services');
  assert.equal(resolvePage('#nonexistent-page'), 'NotFound');
});
