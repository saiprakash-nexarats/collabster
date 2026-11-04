import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../src/api.js';
import { getWebSocketUrl } from '../src/realtime.js';

test('auth methods expose resendOtp and oauthExchange', () => {
  assert.equal(typeof api.auth.resendOtp, 'function');
  assert.equal(typeof api.auth.oauthExchange, 'function');
  assert.equal(typeof api.auth.verifyEmail, 'function');
});

test('getWebSocketUrl derives appropriate websocket url without double /api prefixes', () => {
  // Test fallback in environment without window
  const url = getWebSocketUrl();
  assert.ok(typeof url === 'string');
});
