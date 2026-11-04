import test from 'node:test';
import assert from 'node:assert/strict';
import {canCreate,formatCurrency,joinState,matchOpportunity,rankFeed,searchItems,transition} from '../src/domain.mjs';

test('feed ranking favors relevant followed people',()=>{
 const posts=[{id:'a',authorId:'riya',tags:['Design'],likes:2,comments:1,createdAt:'2026-09-17T09:00:00Z'},{id:'b',authorId:'other',tags:['Java'],likes:99,comments:8,createdAt:'2026-09-16T09:00:00Z'}];
 assert.equal(rankFeed(posts,{mode:'For you',interests:['Design'],following:['riya']})[0].id,'a');
});
test('membership state respects verified communities',()=>{
 assert.equal(joinState('Verified only',false,false),'registration_required');
 assert.equal(joinState('Verified only',true,false),'verification_required');
 assert.equal(joinState('Approval required',true,true),'pending');
 assert.equal(joinState('Open',true,false),'member');
});
test('investment transitions reject invalid jumps',()=>{
 assert.equal(transition('investment','interested','meeting'),'meeting');
 assert.throws(()=>transition('investment','interested','closed'));
});
test('architecture state names remain valid alongside legacy UI aliases',()=>{
 assert.equal(transition('application','submitted','screening'),'screening');
 assert.equal(transition('application','interview','offer'),'offer');
 assert.equal(transition('booking','draft','requested'),'requested');
 assert.equal(transition('application','submitted','reviewing'),'reviewing');
});
test('payment and payout state transitions follow zero trust rules',()=>{
 assert.equal(transition('payment','created','completed'),'completed');
 assert.equal(transition('payment','created','failed'),'failed');
 assert.throws(()=>transition('payment','completed','failed'));
 assert.equal(transition('payout','pending','completed'),'completed');
 assert.equal(transition('payout','pending','rejected'),'rejected');
 assert.throws(()=>transition('payout','completed','pending'));
});
test('formatCurrency formats cents into localized currency representations',()=>{
 assert.equal(formatCurrency(49900,'INR'),'₹499');
 assert.equal(formatCurrency(150000,'INR'),'₹1,500');
 assert.equal(formatCurrency(2500,'USD'),'USD 25.00');
});
test('domain helpers tolerate missing optional values and invalid timestamps',()=>{
 assert.deepEqual(matchOpportunity(),{matched:[],missing:[],score:0});
 assert.equal(canCreate('Event'),false);
 assert.deepEqual(searchItems(undefined,undefined),[]);
 const ranked=rankFeed([{id:'draft',authorId:'me',tags:[],likes:0}],{now:Date.now()});
 assert.equal(Number.isFinite(ranked[0].score),true);
});

