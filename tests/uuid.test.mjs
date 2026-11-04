import test from 'node:test';
import assert from 'node:assert/strict';
import {uuid} from '../src/uuid.js';

test('uses a native UUID implementation when one is available',()=>{
  assert.equal(uuid({randomUUID:()=> 'native-id'}),'native-id');
});

test('creates a valid UUID from getRandomValues without randomUUID',()=>{
  const id=uuid({getRandomValues:bytes=>bytes.fill(0)});
  assert.match(id,/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
});

test('still produces an identifier when Web Crypto is unavailable',()=>{
  assert.match(uuid(undefined),/^[0-9a-f-]+$/);
});
