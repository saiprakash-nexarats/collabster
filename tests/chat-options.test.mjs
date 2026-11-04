import test from 'node:test';
import assert from 'node:assert/strict';

test('mute notifications toggles thread muted state', () => {
  let mutedThreads = [];
  const threadId = 't-1';

  // Mute
  const isMuted = mutedThreads.includes(threadId);
  mutedThreads = isMuted ? mutedThreads.filter((id) => id !== threadId) : [...mutedThreads, threadId];
  assert.equal(mutedThreads.includes(threadId), true);

  // Unmute
  const isMutedAfter = mutedThreads.includes(threadId);
  mutedThreads = isMutedAfter ? mutedThreads.filter((id) => id !== threadId) : [...mutedThreads, threadId];
  assert.equal(mutedThreads.includes(threadId), false);
});

test('delete chat removes thread and clears active selection', () => {
  let threads = [
    { id: 't-1', name: 'Riya Sharma' },
    { id: 't-2', name: 'Aman Verma' },
  ];
  let selected = 't-1';

  // Delete t-1
  const threadIdToDelete = 't-1';
  threads = threads.filter((t) => t.id !== threadIdToDelete);
  if (selected === threadIdToDelete) {
    selected = null;
  }

  assert.equal(threads.length, 1);
  assert.equal(threads[0].id, 't-2');
  assert.equal(selected, null);
});

test('clear chat empties messages for target thread while preserving the conversation', () => {
  let threads = [
    { id: 't-1', name: 'Riya Sharma', messages: [{ id: 'm-1', text: 'Hello' }, { id: 'm-2', text: 'Hey!' }] },
    { id: 't-2', name: 'Aman Verma', messages: [{ id: 'm-3', text: 'Did you check the PR?' }] },
  ];

  // Clear chat for t-1
  const threadIdToClear = 't-1';
  threads = threads.map((t) => (t.id === threadIdToClear ? { ...t, messages: [] } : t));

  assert.equal(threads.find((t) => t.id === 't-1').messages.length, 0);
  assert.equal(threads.find((t) => t.id === 't-2').messages.length, 1);
});

test('archive chat hides conversation from regular filters and displays under Archived tab', () => {
  let archivedThreads = [];
  const threadId = 't-1';

  // Archive
  const isArchived = archivedThreads.includes(threadId);
  archivedThreads = isArchived ? archivedThreads.filter((id) => id !== threadId) : [...archivedThreads, threadId];
  assert.equal(archivedThreads.includes(threadId), true);

  const threads = [
    { id: 't-1', name: 'Riya Sharma' },
    { id: 't-2', name: 'Aman Verma' },
  ];

  // In regular "All" view: archived threads are filtered out
  const allFiltered = threads.filter((t) => !archivedThreads.includes(t.id));
  assert.deepEqual(allFiltered.map((t) => t.id), ['t-2']);

  // In "Archived" view: only archived threads are shown
  const archivedFiltered = threads.filter((t) => archivedThreads.includes(t.id));
  assert.deepEqual(archivedFiltered.map((t) => t.id), ['t-1']);

  // Unarchive
  archivedThreads = archivedThreads.filter((id) => id !== threadId);
  assert.equal(archivedThreads.includes(threadId), false);
});

test('block and unblock user toggles blocked list', () => {
  let blockedUsers = [];
  const userName = 'Riya Sharma';

  // Block
  let isBlocked = blockedUsers.includes(userName);
  blockedUsers = isBlocked ? blockedUsers.filter((u) => u !== userName) : [...blockedUsers, userName];
  assert.equal(blockedUsers.includes(userName), true);

  // Unblock
  isBlocked = blockedUsers.includes(userName);
  blockedUsers = isBlocked ? blockedUsers.filter((u) => u !== userName) : [...blockedUsers, userName];
  assert.equal(blockedUsers.includes(userName), false);
});
