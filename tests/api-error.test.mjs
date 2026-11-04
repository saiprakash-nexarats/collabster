import test from 'node:test';
import assert from 'node:assert/strict';
import {ApiError} from '../src/api.js';

test('api errors preserve actionable server problem details',()=>{
 const error=new ApiError({status:400,code:'validation_failed',detail:'One or more fields are invalid',fields:{title:'must not be blank'}});
 assert.equal(error.name,'ApiError');
 assert.equal(error.status,400);
 assert.equal(error.code,'validation_failed');
 assert.equal(error.fields.title,'must not be blank');
 assert.equal(error.retryable,false);
});

test('network and server failures are safe to retry',()=>{
 assert.equal(new ApiError({status:0}).retryable,true);
 assert.equal(new ApiError({status:503}).retryable,true);
 assert.equal(new ApiError({status:409}).retryable,false);
});
