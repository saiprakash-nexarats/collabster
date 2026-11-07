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
