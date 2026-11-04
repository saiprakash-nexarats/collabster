import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../src/api.js';

test('auth methods expose resendOtp and oauthExchange', () => {
  assert.equal(typeof api.auth.resendOtp, 'function');
  assert.equal(typeof api.auth.oauthExchange, 'function');
  assert.equal(typeof api.auth.verifyEmail, 'function');
});
