'use strict';
const test=require('node:test');
const assert=require('node:assert/strict');
const {extractNativeCalls,normalizePath}=require('./native-map.cjs');
test('Kotlin source mapping keeps method/function/line but does not claim runtime success',()=>{
  const kotlin='suspend fun book(id:Long) = get("/api/novels/$id/detail")\nsuspend fun save(id:Long) = post("/api/novels/${id}/block", JSONObject())';
  const calls=extractNativeCalls(kotlin);
  assert.deepEqual(calls.map(c=>[c.method,c.pathTemplate,c.nativeFunction,c.line]),[['GET','/api/novels/{param}/detail','book',1],['POST','/api/novels/{param}/block','save',2]]);
  assert.ok(calls.every(c=>c.status==='native-source-reference'));
  assert.notEqual(normalizePath('/api/v2/users/me'),normalizePath('/api/users/me'));
});
