import test from 'node:test';
import assert from 'node:assert/strict';

function computeCanReviewRequests(userRole, communitySettings) {
  return (
    userRole === 'Admin' ||
    (userRole === 'Sub-admin' && communitySettings.allowSubadminsApproveRequests !== false)
  );
}

function computeNeedsPermissionToJoin(community, communitySettings) {
  return Boolean(
    communitySettings.needsApproval ||
      communitySettings.privacy === 'Private' ||
      communitySettings.privacy === 'Approval required' ||
      community.policy === 'Approval required' ||
      community.policy === 'Application'
  );
}

function getCommunityTabs({ userRole, community, communitySettings, hasRequestsTab = true }) {
  const needsPermission = computeNeedsPermissionToJoin(community, communitySettings);
  const canReview = computeCanReviewRequests(userRole, communitySettings);
  const showRequests = needsPermission && canReview;

  return [
    'Chat',
    'Channels',
    'Members',
    'Polls',
    'Resources',
    ...(showRequests && hasRequestsTab ? ['Requests'] : []),
    'Settings',
  ];
}

test('Admin sees Requests tab beside Settings when community requires permission', () => {
  const community = { policy: 'Approval required', role: 'Admin' };
  const communitySettings = { needsApproval: true, allowSubadminsApproveRequests: true };
  const tabs = getCommunityTabs({ userRole: 'Admin', community, communitySettings });

  assert.ok(tabs.includes('Requests'));
  const requestsIndex = tabs.indexOf('Requests');
  const settingsIndex = tabs.indexOf('Settings');
  assert.equal(requestsIndex + 1, settingsIndex, 'Requests must be positioned right beside Settings');
});

test('Sub-admin sees Requests tab when admin toggle allowSubadminsApproveRequests is true', () => {
  const community = { privacy: 'Private', role: 'Sub-admin' };
  const communitySettings = { needsApproval: true, allowSubadminsApproveRequests: true };
  const tabs = getCommunityTabs({ userRole: 'Sub-admin', community, communitySettings });

  assert.ok(tabs.includes('Requests'));
  assert.equal(tabs.indexOf('Requests') + 1, tabs.indexOf('Settings'));
});

test('Sub-admin does NOT see Requests tab when admin toggle allowSubadminsApproveRequests is disabled', () => {
  const community = { privacy: 'Private', role: 'Sub-admin' };
  const communitySettings = { needsApproval: true, allowSubadminsApproveRequests: false };
  const tabs = getCommunityTabs({ userRole: 'Sub-admin', community, communitySettings });

  assert.ok(!tabs.includes('Requests'), 'Sub-admin should not see Requests when permission is toggled off');
});

test('Regular Member never sees Requests tab', () => {
  const community = { privacy: 'Private', role: 'Member' };
  const communitySettings = { needsApproval: true, allowSubadminsApproveRequests: true };
  const tabs = getCommunityTabs({ userRole: 'Member', community, communitySettings });

  assert.ok(!tabs.includes('Requests'), 'Member should not see Requests tab');
});

test('Nobody sees Requests tab if community does not require permission to join', () => {
  const community = { policy: 'Open', privacy: 'Public' };
  const communitySettings = { needsApproval: false, privacy: 'Public', allowSubadminsApproveRequests: true };
  const adminTabs = getCommunityTabs({ userRole: 'Admin', community, communitySettings });
  const subAdminTabs = getCommunityTabs({ userRole: 'Sub-admin', community, communitySettings });

  assert.ok(!adminTabs.includes('Requests'));
  assert.ok(!subAdminTabs.includes('Requests'));
});

test('Approving request adds applicant to members list and removes from pending requests', () => {
  let members = [{ id: 'u-1', name: 'Admin User', role: 'Admin' }];
  let requests = [{ id: 'req-1', name: 'Rohan Joshi', title: 'Fullstack Dev' }];

  const reqToApprove = requests[0];
  members = [...members, { id: reqToApprove.id, name: reqToApprove.name, role: 'Member', title: reqToApprove.title }];
  requests = requests.filter(r => r.id !== reqToApprove.id);

  assert.equal(members.length, 2);
  assert.equal(requests.length, 0);
  assert.equal(members[1].name, 'Rohan Joshi');
  assert.equal(members[1].role, 'Member');
});

test('Declining request removes applicant from pending requests without adding to members', () => {
  let members = [{ id: 'u-1', name: 'Admin User', role: 'Admin' }];
  let requests = [{ id: 'req-1', name: 'Rohan Joshi', title: 'Fullstack Dev' }];

  const reqToDecline = requests[0];
  requests = requests.filter(r => r.id !== reqToDecline.id);

  assert.equal(members.length, 1);
  assert.equal(requests.length, 0);
});
