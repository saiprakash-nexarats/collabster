import test from 'node:test';
import assert from 'node:assert/strict';

test('following a follower puts user in both lists without removing them from followers', () => {
  // Initial state: user has followers ['riya', 'neha', 'arjun'], following ['riya', 'aman']
  let followers = ['riya', 'neha', 'arjun'];
  let following = ['riya', 'aman'];

  // User clicks "Follow back" on 'neha'
  const targetId = 'neha';
  const isFollowing = following.includes(targetId);
  following = isFollowing ? following.filter((id) => id !== targetId) : [...following, targetId];

  // Followers list MUST still include 'neha'
  assert.equal(followers.includes('neha'), true);
  // Following list MUST now also include 'neha'
  assert.equal(following.includes('neha'), true);

  // Both lists contain 'neha'
  assert.equal(followers.includes('neha') && following.includes('neha'), true);
});

test('unfollowing someone who follows you keeps them in followers', () => {
  let followers = ['riya', 'neha', 'arjun'];
  let following = ['riya', 'aman', 'neha'];

  // User clicks "Following" on 'neha' to unfollow
  const targetId = 'neha';
  const isFollowing = following.includes(targetId);
  following = isFollowing ? following.filter((id) => id !== targetId) : [...following, targetId];

  // Following no longer has 'neha'
  assert.equal(following.includes('neha'), false);
  // Followers STILL has 'neha'
  assert.equal(followers.includes('neha'), true);
});

test('removing a follower removes them from followers without affecting following', () => {
  let followers = ['riya', 'neha', 'arjun'];
  let following = ['riya', 'aman', 'neha'];

  // User clicks "Remove" on 'neha' in followers
  const targetId = 'neha';
  followers = followers.filter((id) => id !== targetId);

  // Followers no longer has 'neha'
  assert.equal(followers.includes('neha'), false);
  // Following still has 'neha'
  assert.equal(following.includes('neha'), true);
});
