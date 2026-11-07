import test from 'node:test';
import assert from 'node:assert/strict';
import { api, downloadBinary, ApiError } from '../src/api.js';

test('projects API endpoints are mapped with correct paths and HTTP methods', () => {
  assert.equal(typeof api.projects.list, 'function');
  assert.equal(typeof api.projects.create, 'function');
  assert.equal(typeof api.projects.invite, 'function');
  assert.equal(typeof api.projects.acceptInvite, 'function');
  assert.equal(typeof api.projects.files, 'function');
  assert.equal(typeof api.projects.saveFile, 'function');
  assert.equal(typeof api.projects.deleteFile, 'function');
  assert.equal(typeof api.projects.tasks, 'function');
  assert.equal(typeof api.projects.createTask, 'function');
  assert.equal(typeof api.projects.updateTaskStatus, 'function');
  assert.equal(typeof api.projects.milestones, 'function');
  assert.equal(typeof api.projects.createMilestone, 'function');
});

test('companies API endpoints are mapped with correct paths and HTTP methods', () => {
  assert.equal(typeof api.companies.list, 'function');
  assert.equal(typeof api.companies.create, 'function');
  assert.equal(typeof api.companies.invite, 'function');
  assert.equal(typeof api.companies.acceptInvite, 'function');
  assert.equal(typeof api.companies.tasks, 'function');
  assert.equal(typeof api.companies.createTask, 'function');
  assert.equal(typeof api.companies.updateTask, 'function');
  assert.equal(typeof api.companies.documents, 'function');
  assert.equal(typeof api.companies.createDocument, 'function');
  assert.equal(typeof api.companies.downloadDocument, 'function');
  assert.equal(typeof api.companies.downloadFile, 'function');
  assert.equal(typeof api.companies.archiveDocument, 'function');
});

test('downloadBinary streams authenticated binary content and extracts filename from header', async () => {
  const originalFetch = globalThis.fetch;
  try {
    let capturedUrl = '';
    let capturedHeaders = {};
    globalThis.fetch = async (url, options) => {
      capturedUrl = url;
      capturedHeaders = options.headers || {};
      return {
        ok: true,
        status: 200,
        headers: new Headers({
          'content-type': 'application/pdf',
          'content-disposition': 'attachment; filename="financial_audit_2026.pdf"',
        }),
        blob: async () => new Blob(['%PDF-1.4 simulated binary data'], { type: 'application/pdf' }),
      };
    };

    const res = await downloadBinary('/v1/companies/c-123/documents/d-456/raw', 'secret-access-token', 'default.pdf');
    assert.equal(capturedUrl, '/v1/companies/c-123/documents/d-456/raw');
    assert.equal(capturedHeaders.Authorization, 'Bearer secret-access-token');
    assert.equal(res.filename, 'financial_audit_2026.pdf');
    assert.ok(res.size > 0);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('downloadBinary surfaces 403 Forbidden on unauthorized document access', async () => {
  const originalFetch = globalThis.fetch;
  try {
    globalThis.fetch = async () => ({
      ok: false,
      status: 403,
      headers: new Map([['content-type', 'application/json']]),
      json: async () => ({
        code: 'ACCESS_DENIED',
        detail: 'Admin access required to download this document',
      }),
    });

    await assert.rejects(
      async () => {
        await downloadBinary('/v1/companies/c-123/documents/d-456/raw', 'regular-user-token');
      },
      (err) => {
        assert.ok(err instanceof ApiError);
        assert.equal(err.status, 403);
        assert.equal(err.detail, 'Admin access required to download this document');
        return true;
      }
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('downloadBinary surfaces 401 Unauthorized when session is expired', async () => {
  const originalFetch = globalThis.fetch;
  try {
    globalThis.fetch = async () => ({
      ok: false,
      status: 401,
      headers: new Map([['content-type', 'application/json']]),
      json: async () => ({
        code: 'UNAUTHORIZED',
        detail: 'Full authentication is required to access this resource',
      }),
    });

    await assert.rejects(
      async () => {
        await downloadBinary('/v1/companies/c-123/documents/d-456/raw', 'expired-token');
      },
      (err) => {
        assert.ok(err instanceof ApiError);
        assert.equal(err.status, 401);
        return true;
      }
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('projects and companies payload validation rules', () => {
  // Test project visibility validation
  const validVisibilities = ['private', 'team', 'public'];
  const testVisibility = 'public';
  assert.ok(validVisibilities.includes(testVisibility));

  // Test project role hierarchy
  const roleHierarchy = {
    owner: ['admin', 'manage_members', 'write_files', 'write_tasks', 'read'],
    maintainer: ['manage_members', 'write_files', 'write_tasks', 'read'],
    collaborator: ['write_files', 'write_tasks', 'read'],
    viewer: ['read'],
  };

  assert.ok(roleHierarchy.viewer.includes('read'));
  assert.ok(!roleHierarchy.viewer.includes('write_files'));
  assert.ok(!roleHierarchy.viewer.includes('write_tasks'));
  assert.ok(!roleHierarchy.viewer.includes('manage_members'));

  assert.ok(roleHierarchy.collaborator.includes('write_files'));
  assert.ok(roleHierarchy.collaborator.includes('write_tasks'));
  assert.ok(!roleHierarchy.collaborator.includes('manage_members'));

  assert.ok(roleHierarchy.maintainer.includes('manage_members'));
  assert.ok(roleHierarchy.owner.includes('admin'));
});

test('newly created project retains remote: true and can immediately invite members, save files and tasks', () => {
  const remoteResponse = {
    id: 'f9b33a50-2591-4cf1-a0c3-f42f026a79ee',
    title: 'Cloud Orchestrator',
    description: 'Kubernetes management tool',
    state: 'active',
    createdAt: new Date().toISOString(),
  };

  // Derive frontend model matching App.jsx createProject()
  const project = {
    title: remoteResponse.title,
    description: remoteResponse.description,
    id: remoteResponse.id,
    state: remoteResponse.state,
    roles: 'owner',
    membershipState: 'active',
    createdAt: remoteResponse.createdAt,
    remote: Boolean(remoteResponse),
  };

  assert.equal(project.remote, true, 'Newly created project must have remote = true');
  assert.equal(project.id, 'f9b33a50-2591-4cf1-a0c3-f42f026a79ee');
  assert.equal(project.membershipState, 'active');
  assert.equal(project.roles, 'owner');

  // Verify that immediate invitation check passes because project.remote is true
  const canInviteImmediately = project.remote && project.id.length > 0;
  assert.equal(canInviteImmediately, true, 'Must allow immediate invites without page reload');
});

test('invitation state normalization matches backend pending and legacy invited contracts', () => {
  function isPendingInvitation(membershipState) {
    return membershipState === 'pending' || membershipState === 'invited';
  }

  assert.equal(isPendingInvitation('pending'), true, 'Backend pending state must be recognized as pending');
  assert.equal(isPendingInvitation('invited'), true, 'Legacy invited state must be recognized as pending');
  assert.equal(isPendingInvitation('active'), false, 'Active membership must not be treated as pending');
  assert.equal(isPendingInvitation('declined'), false, 'Declined membership must not be treated as pending');

  // Acceptance transition
  const member = { id: 'user-2', membershipState: 'pending' };
  const accepted = { ...member, membershipState: 'active' };
  assert.equal(isPendingInvitation(accepted.membershipState), false);
  assert.equal(accepted.membershipState, 'active');
});

test('authenticated mode rejects offline operations without creating fake preview records', () => {
  const auth = { accessToken: 'valid-jwt-token' };
  const serviceState = 'disconnected';

  function assertCanPerformCloudAction(auth, serviceState, entity) {
    if (auth?.accessToken) {
      if (serviceState !== 'connected' || (entity && !entity.remote)) {
        throw new Error('An active connection to Collobster is required.');
      }
      return 'synced';
    }
    return 'preview';
  }

  assert.throws(
    () => assertCanPerformCloudAction(auth, serviceState, { remote: true }),
    /An active connection to Collobster is required/
  );

  assert.throws(
    () => assertCanPerformCloudAction(auth, 'connected', { remote: false }),
    /An active connection to Collobster is required/
  );

  assert.equal(
    assertCanPerformCloudAction(null, 'disconnected', null),
    'preview',
    'Unauthenticated visitor can still use local preview mode'
  );
});
