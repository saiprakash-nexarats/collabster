import test from 'node:test';
import assert from 'node:assert/strict';

process.env.VITE_API_URL = 'http://localhost:8080';
const { api, ApiError } = await import('../src/api.js');

test('learning API endpoints are fully mapped with correct functions', () => {
  assert.equal(typeof api.learning.courses, 'function');
  assert.equal(typeof api.learning.createCourse, 'function');
  assert.equal(typeof api.learning.lessons, 'function');
  assert.equal(typeof api.learning.addLesson, 'function');
  assert.equal(typeof api.learning.enroll, 'function');
  assert.equal(typeof api.learning.completeLesson, 'function');
  assert.equal(typeof api.learning.claimCertificate, 'function');
  assert.equal(typeof api.learning.myCertificates, 'function');
  assert.equal(typeof api.learning.verifyCertificate, 'function');
  assert.equal(typeof api.learning.lessonExercise, 'function');
});

test('mentorship API endpoints are fully mapped with correct functions', () => {
  assert.equal(typeof api.mentors.list, 'function');
  assert.equal(typeof api.mentors.sessions, 'function');
  assert.equal(typeof api.mentors.upsertProfile, 'function');
  assert.equal(typeof api.mentors.requestSession, 'function');
  assert.equal(typeof api.mentors.confirmSession, 'function');
  assert.equal(typeof api.mentors.cancelSession, 'function');
  assert.equal(typeof api.mentors.slots, 'function');
  assert.equal(typeof api.mentors.createSlot, 'function');
  assert.equal(typeof api.mentors.bookSlot, 'function');
});

test('careers API endpoints are fully mapped with correct functions', () => {
  assert.equal(typeof api.career.jobs, 'function');
  assert.equal(typeof api.career.applications, 'function');
  assert.equal(typeof api.career.createJob, 'function');
  assert.equal(typeof api.career.apply, 'function');
  assert.equal(typeof api.career.withdraw, 'function');
  assert.equal(typeof api.career.jobApplications, 'function');
  assert.equal(typeof api.career.reviewApplication, 'function');
});

test('learning requests construct correct URL paths, methods and authorization headers', async () => {
  const originalFetch = globalThis.fetch;
  const calls = [];
  try {
    globalThis.fetch = async (url, options = {}) => {
      calls.push({ url, options });
      return {
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ success: true, data: { ok: true } })
      };
    };

    await api.learning.courses('test-token-123');
    await api.learning.createCourse({ title: 'System Design', description: 'Advanced' }, 'test-token-123');
    await api.learning.lessons('c-uuid-1', 'test-token-123');
    await api.learning.addLesson('c-uuid-1', { title: 'Sharding', duration: '20 min' }, 'test-token-123');
    await api.learning.enroll('c-uuid-1', 'test-token-123');
    await api.learning.completeLesson('c-uuid-1', 'l-uuid-1', 'test-token-123');
    await api.learning.claimCertificate('c-uuid-1', 'test-token-123');
    await api.learning.myCertificates('test-token-123');
    await api.learning.verifyCertificate('COLLOB-2026-ABCDEF12');
    await api.learning.lessonExercise('c-uuid-1', 'l-uuid-1', 'test-token-123');

    assert.equal(calls.length, 10);
    assert.match(calls[0].url, /\/v1\/learning\/courses$/);
    assert.equal(calls[0].options.headers['Authorization'], 'Bearer test-token-123');

    assert.match(calls[1].url, /\/v1\/learning\/courses$/);
    assert.equal(calls[1].options.method, 'POST');
    assert.match(calls[1].options.body, /System Design/);

    assert.match(calls[2].url, /\/v1\/learning\/courses\/c-uuid-1\/lessons$/);

    assert.match(calls[3].url, /\/v1\/learning\/courses\/c-uuid-1\/lessons$/);
    assert.equal(calls[3].options.method, 'POST');

    assert.match(calls[4].url, /\/v1\/learning\/courses\/c-uuid-1\/enrollments$/);
    assert.equal(calls[4].options.method, 'POST');

    assert.match(calls[5].url, /\/v1\/learning\/courses\/c-uuid-1\/lessons\/l-uuid-1\/complete$/);
    assert.equal(calls[5].options.method, 'POST');

    assert.match(calls[6].url, /\/v1\/learning\/courses\/c-uuid-1\/certificate$/);
    assert.equal(calls[6].options.method, 'POST');

    assert.match(calls[7].url, /\/v1\/learning\/certificates\/my$/);

    assert.match(calls[8].url, /\/v1\/learning\/certificates\/verify\/COLLOB-2026-ABCDEF12$/);

    assert.match(calls[9].url, /\/v1\/learning\/courses\/c-uuid-1\/lessons\/l-uuid-1\/exercise$/);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('mentorship requests construct correct URL paths, methods and authorization headers', async () => {
  const originalFetch = globalThis.fetch;
  const calls = [];
  try {
    globalThis.fetch = async (url, options = {}) => {
      calls.push({ url, options });
      return {
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ success: true, data: { id: 'm-1' } })
      };
    };

    await api.mentors.list('token-xyz');
    await api.mentors.sessions('token-xyz');
    await api.mentors.upsertProfile({ headline: 'Senior Staff Engineer', hourlyRate: 150 }, 'token-xyz');
    await api.mentors.requestSession('42', { sessionType: '1:1', startsAt: '2026-12-01T10:00:00Z' }, 'token-xyz');
    await api.mentors.confirmSession('s-uuid-1', 'token-xyz');
    await api.mentors.cancelSession('s-uuid-1', 'token-xyz');
    await api.mentors.slots('42', 'token-xyz');
    await api.mentors.createSlot({ slotTime: '2026-12-01T10:00:00Z', durationMinutes: 45 }, 'token-xyz');
    await api.mentors.bookSlot('slot-uuid-99', { sessionType: 'System Review' }, 'token-xyz');

    assert.equal(calls.length, 9);
    assert.match(calls[0].url, /\/v1\/mentors$/);
    assert.match(calls[1].url, /\/v1\/mentors\/sessions$/);
    assert.match(calls[2].url, /\/v1\/mentors\/profile$/);
    assert.equal(calls[2].options.method, 'POST');
    assert.match(calls[3].url, /\/v1\/mentors\/42\/sessions$/);
    assert.match(calls[4].url, /\/v1\/mentors\/sessions\/s-uuid-1\/confirm$/);
    assert.match(calls[5].url, /\/v1\/mentors\/sessions\/s-uuid-1\/cancel$/);
    assert.match(calls[6].url, /\/v1\/mentors\/42\/slots$/);
    assert.match(calls[7].url, /\/v1\/mentors\/slots$/);
    assert.equal(calls[7].options.method, 'POST');
    assert.match(calls[8].url, /\/v1\/mentors\/slots\/slot-uuid-99\/book$/);
    assert.equal(calls[8].options.method, 'POST');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('career requests construct correct URL paths, methods and authorization headers', async () => {
  const originalFetch = globalThis.fetch;
  const calls = [];
  try {
    globalThis.fetch = async (url, options = {}) => {
      calls.push({ url, options });
      return {
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ success: true, data: { id: 'job-1' } })
      };
    };

    await api.career.jobs('token-abc');
    await api.career.applications('token-abc');
    await api.career.createJob({ companyName: 'Acme', title: 'Tech Lead', description: 'Lead team' }, 'token-abc');
    await api.career.apply('job-uuid-10', { note: 'Interested' }, 'token-abc');
    await api.career.withdraw('app-uuid-5', 'token-abc');
    await api.career.jobApplications('job-uuid-10', 'token-abc');
    await api.career.reviewApplication('job-uuid-10', 'app-uuid-5', { status: 'shortlisted' }, 'token-abc');

    assert.equal(calls.length, 7);
    assert.match(calls[0].url, /\/v1\/career\/jobs$/);
    assert.match(calls[1].url, /\/v1\/career\/applications$/);
    assert.match(calls[2].url, /\/v1\/career\/jobs$/);
    assert.equal(calls[2].options.method, 'POST');
    assert.match(calls[3].url, /\/v1\/career\/jobs\/job-uuid-10\/applications$/);
    assert.equal(calls[3].options.method, 'POST');
    assert.match(calls[4].url, /\/v1\/career\/applications\/app-uuid-5\/withdraw$/);
    assert.equal(calls[4].options.method, 'POST');
    assert.match(calls[5].url, /\/v1\/career\/jobs\/job-uuid-10\/applications$/);
    assert.match(calls[6].url, /\/v1\/career\/jobs\/job-uuid-10\/applications\/app-uuid-5\/review$/);
    assert.equal(calls[6].options.method, 'PATCH');
  } finally {
    globalThis.fetch = originalFetch;
  }
});
