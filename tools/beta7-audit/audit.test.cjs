'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const {
  assertPublicReadUrl, sanitizeText, safePost, flattenComments,
  collectPages, analyzeChunk, classifyFeedbackCandidate,
} = require('./audit.cjs');

test('capture allowlist rejects auth, private data, off-site URLs and mutating actions', () => {
  for (const path of ['/', '/_nuxt/a.js', '/api/posts?type=feedback&page=1', '/api/posts/1871/comments']) {
    assert.equal(assertPublicReadUrl(`https://novalpie.cc${path}`).origin, 'https://novalpie.cc');
  }
  for (const url of ['https://novalpie.cc/api/users/me', 'https://novalpie.cc/api/messages', 'https://example.org/api/posts', 'https://u:p@novalpie.cc/api/posts']) {
    assert.throws(() => assertPublicReadUrl(url), /public read/);
  }
  assert.throws(() => assertPublicReadUrl('https://novalpie.cc/api/posts', 'POST'), /public read/);
});

test('public excerpts redact credentials and mail addresses before local persistence', () => {
  const raw = 'email=user@example.com password="example-credential-123" Cookie: session=abc123\nBearer abcdefghijklmnop123456';
  const safe = sanitizeText(raw);
  assert.ok(!safe.includes('user@example.com'));
  assert.ok(!safe.includes('example-credential-123'));
  assert.ok(!safe.includes('abc123'));
  assert.ok(!safe.includes('abcdefghijklmnop123456'));
  assert.ok(safe.includes('[REDACTED]'));
});

test('only required public post fields survive capture', () => {
  const post = safePost({id: 1, title: '下载', content: '不能下载', type: 'feedback', created_at: '2026-09-05', token: 'secret', author_email: 'private@example.com', author_name: '反馈者'});
  assert.equal(post.id, 1);
  assert.equal(post.content, '不能下载');
  assert.ok(!JSON.stringify(post).includes('secret'));
  assert.ok(!JSON.stringify(post).includes('private@example.com'));
});

test('pagination follows returned page size and de-duplicates without silently truncating', async () => {
  const seen = [];
  const pages = {
    1: {posts: [{id: 1}, {id: 2}], pagination: {page: 1, limit: 2, pages: 3, total: 5}},
    2: {posts: [{id: 2}, {id: 3}], pagination: {page: 2, limit: 2, pages: 3, total: 5}},
    3: {posts: [{id: 4}, {id: 5}], pagination: {page: 3, limit: 2, pages: 3, total: 5}},
  };
  const result = await collectPages(async page => { seen.push(page); return pages[page]; }, 'posts');
  assert.deepEqual(seen, [1, 2, 3]);
  assert.deepEqual(result.items.map(x => x.id), [1, 2, 3, 4, 5]);
  assert.equal(result.complete, true);
  assert.equal(result.duplicates, 1);
});

test('a disappeared or capped page stays incomplete instead of claiming full coverage', async () => {
  const result = await collectPages(async page => ({posts: page === 1 ? [{id: 1}] : [], pagination: {page, pages: 2, total: 2, limit: 1}}), 'posts');
  assert.equal(result.complete, false);
  assert.ok(result.gaps.length > 0);
});

test('all nested replies retain provenance and independent identifiers', () => {
  const items = flattenComments(1871, [{id: 7, content: '根', replies: [{id: 7, content: '子', replies: [{id: 8, content: '孙'}]}]}]);
  assert.equal(items.length, 3);
  assert.equal(new Set(items.map(x => x.key)).size, 3);
  assert.equal(items[2].postId, 1871);
  assert.equal(items[2].depth, 2);
  assert.equal(items[2].rootCommentId, 7);
});

test('source analysis links routes, handlers and concatenated API contracts without evaluation', () => {
  const js = 'const routes=[{name:"reader",path:"/book/:bookId()/:chapterId()",meta:{middleware:["auth"]},component:()=>import("./Reader.js")}];'
    + 'const api={async createRule(id,a,b){return await N("".concat(P(),"/users/me/glossaries"),{method:"POST",body:{novel_id:id,source_name:a,target_name:b}})}};'
    + 'const el=ok?m("button",{onClick:save,title:"提交规则",disabled:busy},"发布"):null;';
  const result = analyzeChunk('https://novalpie.cc/_nuxt/entry.js', js);
  assert.equal(result.routes[0].path, '/book/:bookId()/:chapterId()');
  assert.deepEqual(result.routes[0].middleware, ['auth']);
  assert.ok(result.dependencies.includes('https://novalpie.cc/_nuxt/Reader.js'));
  const request = result.apiCalls.find(x => x.pathTemplate === '/api/users/me/glossaries');
  assert.equal(request.method, 'POST');
  assert.deepEqual(request.bodyFields, ['novel_id', 'source_name', 'target_name']);
  assert.equal(request.functionName, 'createRule');
  assert.equal(request.evidenceLevel, 'source-discovered');
  assert.ok(result.controls.some(x => x.labels.includes('发布') && x.events.onClick === 'save' && x.disabled === 'busy'));
  assert.ok(result.controls[0].conditions.includes('ok'));
});

test('interpolated and method-unknown endpoints are never presented as actual GET evidence', () => {
  const result = analyzeChunk('https://novalpie.cc/_nuxt/chunk.js', 'async function toggle(id,method){return await $fetch(`${base}/posts/${id}/like`,{method,body:{type:1}})}');
  assert.equal(result.apiCalls[0].pathTemplate, '/api/posts/{id}/like');
  assert.equal(result.apiCalls[0].method, 'dynamic');
  assert.equal(result.apiCalls[0].evidenceLevel, 'source-discovered');
});

test('candidate classification preserves ambiguity and never marks feedback fixed', () => {
  const feedback = classifyFeedbackCandidate('下载图片一直失败，但后来重试又正常了');
  assert.ok(feedback.topics.includes('download'));
  assert.equal(feedback.reviewStatus, 'needs-human-review');
  assert.notEqual(feedback.verdict, 'fixed');
});

test('versioned source APIs and spread request fields are retained', () => {
  const js = 'async function send(id,extra){const common={novel_id:id};return N(`${base}/api/v2/novels/${id}/chapters/request`,{method:"POST",body:{...common,chapter_number:1,...extra}})}';
  const call = analyzeChunk('https://novalpie.cc/_nuxt/entry.js', js).apiCalls[0];
  assert.equal(call.pathTemplate, '/api/v2/novels/{id}/chapters/request');
  assert.deepEqual(call.bodyFields, ['novel_id', 'chapter_number']);
  assert.deepEqual(call.unresolvedBodySpreads, ['extra']);
});

test('request field lookup respects lexical scopes in minified bundles', () => {
  const js = 'const t={outside:1};async function first(){const t={page:1};return N(`${base}/posts`,{method:"GET",query:t})}async function second(){const t={type:1};return N(`${base}/comments`,{method:"GET",query:t})}';
  const calls = analyzeChunk('https://novalpie.cc/_nuxt/entry.js', js).apiCalls;
  assert.deepEqual(calls[0].queryFields, ['page']);
  assert.deepEqual(calls[1].queryFields, ['type']);
});

test('method-style HTTP wrappers keep their relative URL and query/body distinction explicit', () => {
  const js = 'const base="/users";async function blocks(page){return client.get("".concat(base,"/me/blocks"),{page,limit:20})}async function ban(id){return client.post(`${base}/${id}/block`)}';
  const calls = analyzeChunk('https://novalpie.cc/_nuxt/profile.js', js).apiCalls;
  assert.equal(calls[0].pathTemplate, '/api/users/me/blocks');
  assert.equal(calls[0].method, 'GET');
  assert.deepEqual(calls[0].queryFields, ['page','limit']);
  assert.equal(calls[0].baseResolution, 'wrapper-base-unverified');
  assert.equal(calls[1].method, 'POST');
  assert.equal(calls[1].pathTemplate, '/api/users/{id}/block');
});

test('manually verified imported HTTP base overrides only the matching receiver', () => {
  const js='import{r as api}from"./Http.js";const base="/users";api.get(`${base}/me/blocks`,{page:1});other.get(`${base}/me`)';
  const verifiedBases={'https://novalpie.cc/_nuxt/Http.js#r':'/api/v2'};
  const calls=analyzeChunk('https://novalpie.cc/_nuxt/User.js',js,{verifiedBases}).apiCalls;
  assert.equal(calls[0].pathTemplate,'/api/v2/users/me/blocks');
  assert.equal(calls[0].baseResolution,'verified-import-base');
  assert.equal(calls[1].baseResolution,'wrapper-base-unverified');
});
