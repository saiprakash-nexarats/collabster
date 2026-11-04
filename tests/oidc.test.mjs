import test from 'node:test';
import assert from 'node:assert/strict';
import {decodeJwt} from '../src/oidc.js';

function jwtFor(claims){
  const payload=Buffer.from(JSON.stringify(claims)).toString('base64url');
  return `header.${payload}.signature`;
}

test('decodes UTF-8 JWT claims without mojibake',()=>{
  const claims=decodeJwt(jwtFor({sub:'member-1',name:'Jürgen François',city:'Bengaluru'}));
  assert.equal(claims.name,'Jürgen François');
  assert.equal(claims.city,'Bengaluru');
});

test('returns an empty claim set for an invalid JWT',()=>{
  assert.deepEqual(decodeJwt('invalid'),{});
});
