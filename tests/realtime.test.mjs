import test from 'node:test';
import assert from 'node:assert/strict';
import {parseRealtimeFrames} from '../src/realtime.js';

test('parses a STOMP message event without losing its JSON payload',()=>{
 const [event]=parseRealtimeFrames('MESSAGE\ndestination:/user/queue/updates\ncontent-type:application/json\n\n{"type":"message.created","payload":{"message":{"id":"m1"}}}\0');
 assert.equal(event.command,'MESSAGE');
 assert.equal(event.headers.destination,'/user/queue/updates');
 assert.equal(event.body.type,'message.created');
 assert.equal(event.body.payload.message.id,'m1');
});

test('keeps valid frames when a malformed payload arrives beside them',()=>{
 const frames=parseRealtimeFrames('CONNECTED\nversion:1.2\n\n\0MESSAGE\n\nnot-json\0');
 assert.equal(frames.length,2);
 assert.equal(frames[0].command,'CONNECTED');
 assert.equal(frames[1].body,null);
});

test('normalizes CRLF STOMP frames for broker compatibility',()=>{
  const frames=parseRealtimeFrames(['CONNECTED','version:1.2','',''].join('\r\n')+'\0');
  assert.equal(frames[0].command,'CONNECTED');
  assert.equal(frames[0].headers.version,'1.2');
});
