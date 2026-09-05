'use strict';
// Only reads the current site's public source/feedback. No browser storage or auth headers.
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const {fetch, ProxyAgent} = require('undici');
const {ORIGIN, assertPublicReadUrl, sanitizeText, safePost, flattenComments, collectPages, analyzeChunk, classifyFeedbackCandidate} = require('./audit.cjs');

const args = process.argv.slice(2);
function arg(name, fallback) { const i = args.indexOf(name); return i >= 0 ? args[i + 1] : fallback; }
const outputRoot = path.resolve(arg('--out', path.resolve(__dirname, '../../../agent-bridge/artifacts/beta7-baseline')));
const stage = arg('--stage', 'all');
if (!['all', 'site', 'feedback'].includes(stage)) throw new Error('stage must be all/site/feedback');
const dispatcher = new ProxyAgent(arg('--proxy', 'http://127.0.0.1:7890'));
const startedAt = new Date().toISOString();
const requestLog = [];
const failures = [];
let nextRequestAt = 0;
let totalBytes = 0;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
async function writeJson(relative, object) {
  const target = path.resolve(outputRoot, relative);
  if (!target.startsWith(outputRoot + path.sep)) throw new Error('Output escaped capture directory');
  await fs.mkdir(path.dirname(target), {recursive: true});
  await fs.writeFile(target, JSON.stringify(object, null, 2) + '\n', 'utf8');
}
async function get(url) {
  assertPublicReadUrl(url);
  const scheduled = Math.max(Date.now(), nextRequestAt);
  nextRequestAt = scheduled + 350;
  await delay(Math.max(0, scheduled - Date.now()));
  const start = Date.now();
  let response;
  try {
    response = await fetch(url, {
      dispatcher, redirect: 'error', method: 'GET',
      headers: {'user-agent': 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Mobile Safari/537.36', accept: '*/*'},
      signal: AbortSignal.timeout(25000),
    });
    if (!response.ok) { await response.body?.cancel(); throw new Error(`HTTP ${response.status}`); }
    const chunks = [];
    let bytes = 0;
    for await (const chunk of response.body) {
      bytes += chunk.length;
      if (bytes > 10 * 1024 * 1024) throw new Error('Response exceeded source/JSON safety limit');
      chunks.push(chunk);
    }
    totalBytes += bytes;
    if (totalBytes > 160 * 1024 * 1024) throw new Error('Capture safety limit reached; incomplete');
    const buffer = Buffer.concat(chunks);
    requestLog.push({url, method:'GET', session:'anonymous', status:response.status, durationMs:Date.now()-start, bytes, sha256:sha(buffer), capturedAt:new Date().toISOString()});
    return buffer.toString('utf8');
  } catch (error) {
    requestLog.push({url, method:'GET', session:'anonymous', status:response?.status ?? null, durationMs:Date.now()-start, error:sanitizeText(error.message)});
    throw error;
  }
}
async function getJson(url) { return JSON.parse(await get(url)); }
async function pool(items, concurrency, job) {
  let cursor = 0;
  await Promise.all(Array.from({length: Math.min(concurrency, items.length)}, async () => {
    while (cursor < items.length) { const item = items[cursor++]; await job(item); }
  }));
}

async function captureSite() {
  const html = await get(ORIGIN + '/');
  const buildId = html.match(/buildId:"([^"]+)"/)?.[1] ?? null;
  await fs.mkdir(path.join(outputRoot, 'site/assets'), {recursive:true});
  await fs.writeFile(path.join(outputRoot, 'site/index.html'), html, 'utf8');
  const pending = [...new Set([...html.matchAll(/(?:src|href|data-src)=["'](\/_nuxt\/[^"']+\.(?:js|css))["']/g)].map(m => ORIGIN + m[1]))];
  const queued = new Set(pending);
  const analyses = [];
  const manifest = [];
  let assetTotal = 0;
  while (pending.length) {
    const batch = pending.splice(0, 3);
    await pool(batch, 3, async url => {
      if (++assetTotal > 512) { failures.push({stage:'site',url,error:'Asset safety limit reached'}); return; }
      try {
        const content = await get(url);
        const file = `site/assets/${path.basename(new URL(url).pathname)}`;
        await fs.writeFile(path.join(outputRoot, file), content, 'utf8');
        const digest = sha(Buffer.from(content));
        manifest.push({url,file,sha256:digest,bytes:Buffer.byteLength(content)});
        if (url.endsWith('.js')) {
          const analysis = analyzeChunk(url, content);
          analyses.push(analysis);
          for (const dependency of analysis.dependencies) if (!queued.has(dependency)) {queued.add(dependency); pending.push(dependency);}
          if (analysis.parseErrors.length) failures.push({stage:'parse',url,errors:analysis.parseErrors});
        }
        if (manifest.length % 15 === 0) console.log(`SITE assets=${manifest.length} remaining=${pending.length}`);
      } catch (error) { failures.push({stage:'site',url,error:sanitizeText(error.message)}); }
    });
  }
  // Multiple modern and legacy chunks can describe one route; retain source provenance.
  const allRoutes = analyses.flatMap(a => a.routes);
  const routes = [...new Set(allRoutes.map(r => r.path))].sort().map(route => ({path:route,definitions:allRoutes.filter(r=>r.path===route)}));
  const apiCalls = analyses.flatMap(a => a.apiCalls);
  const contracts = [...new Set(apiCalls.map(a => `${a.method} ${a.pathTemplate}`))].sort().map(key => {
    const callSites = apiCalls.filter(a => `${a.method} ${a.pathTemplate}` === key);
    return {key, method:callSites[0].method, pathTemplate:callSites[0].pathTemplate, queryFields:[...new Set(callSites.flatMap(c=>c.queryFields))].sort(), bodyFields:[...new Set(callSites.flatMap(c=>c.bodyFields))].sort(), evidenceLevel:'source-discovered',callSites};
  });
  await writeJson('site/manifest.json', {startedAt,buildId,anonymous:true,assets:manifest,failures:failures.filter(f=>f.stage==='site'||f.stage==='parse')});
  await writeJson('site/routes.json', routes);
  await writeJson('site/api-candidates.json', contracts);
  await writeJson('site/control-candidates.json', analyses.flatMap(a => a.controls));
  const summary = {buildId,assetCount:manifest.length,routeCount:routes.length,apiCandidates:contracts.length,controlCandidates:analyses.reduce((sum,a)=>sum+a.controls.length,0),failures:failures.filter(f=>f.stage==='site'||f.stage==='parse').length, runtimeVerifiedControls:0};
  await writeJson('site/summary.json', summary);
  console.log('SITE_COMPLETE ' + JSON.stringify(summary));
  return summary;
}

async function captureFeedback() {
  const feeds = {};
  const relevant = new Map();
  for (const type of ['feedback','discussion']) {
    const result = await collectPages(async page => {
      const response = await getJson(`${ORIGIN}/api/posts?type=${type}&page=${page}&limit=50`);
      const safe = {...response, posts:response.posts.map(safePost)};
      await writeJson(`feedback/${type}/page-${String(page).padStart(3,'0')}.json`, {posts:safe.posts,pagination:response.pagination});
      console.log(`FEED ${type} page=${page}/${response.pagination?.pages} items=${safe.posts.length}`);
      return safe;
    }, 'posts');
    feeds[type] = {...result, items:undefined};
    await writeJson(`feedback/${type}/posts.json`, result.items);
    for (const post of result.items) {
      const candidate = classifyFeedbackCandidate(post.title + '\n' + post.content);
      if (type === 'feedback' || candidate.topics.some(topic => topic !== 'account')) relevant.set(post.id,{...post,candidate});
    }
  }
  // Known App feedback hubs are included even if they moved type or title since the feed snapshot.
  for (const id of [1871,1828,1827,1796,1742]) if (!relevant.has(id)) relevant.set(id,{id,candidate:{topics:['app'],reviewStatus:'needs-human-review',verdict:null}});
  const threadResults = [];
  let completeCount = 0;
  await pool([...relevant.values()], 2, async post => {
    const file = `feedback/threads/${post.id}.json`;
    try {
      const detailPayload = await getJson(`${ORIGIN}/api/posts/${post.id}`);
      const rawDetail = detailPayload.post ?? detailPayload.data?.post ?? detailPayload.data ?? detailPayload;
      if (!rawDetail.id || detailPayload.success === false) throw new Error('Post detail missing/rejected');
      const detail = safePost(rawDetail);
      const result = await collectPages(page => getJson(`${ORIGIN}/api/posts/${post.id}/comments?page=${page}&limit=50`), 'comments');
      const comments = flattenComments(post.id, result.items);
      const nestedPaging = result.items.filter(c=>c.replies_pagination || c.has_more_replies || c.replies_has_more).map(c=>c.id);
      const complete = result.complete && nestedPaging.length === 0;
      await writeJson(file, {post:detail,candidate:classifyFeedbackCandidate(detail.title+'\n'+detail.content+'\n'+comments.map(c=>c.content).join('\n')),comments,pagination:{...result,items:undefined},nestedPaging,complete,reviewStatus:'needs-human-review'});
      threadResults.push({postId:post.id,file,title:detail.title,type:detail.type,commentCount:comments.length,complete,updatedAt:detail.updatedAt});
      if(complete) completeCount++;
      if(threadResults.length % 10 === 0) console.log(`THREADS complete=${completeCount} processed=${threadResults.length}/${relevant.size}`);
    } catch (error) {
      const failure={postId:post.id,stage:'thread',error:sanitizeText(error.message)};
      failures.push(failure);
      threadResults.push({...failure,complete:false});
    }
    // An interrupted run keeps its fetched evidence and explicit partial index.
    await writeJson('feedback/progress.json',{feeds,selectedThreads:relevant.size,processedThreads:threadResults.length,completeCount});
  });
  await writeJson('feedback/thread-index.json',threadResults.sort((a,b)=>b.postId-a.postId));
  const summary={feeds,selectedThreads:relevant.size,processedThreads:threadResults.length,completeThreads:completeCount,commentCount:threadResults.reduce((sum,t)=>sum+(t.commentCount??0),0),failures:failures.filter(f=>f.stage==='thread')};
  await writeJson('feedback/summary.json', summary);
  console.log('FEEDBACK_COMPLETE ' + JSON.stringify({selected:relevant.size,complete:completeCount,comments:summary.commentCount,failures:summary.failures.length}));
  return summary;
}

(async () => {
  await fs.mkdir(outputRoot,{recursive:true});
  const manifest={startedAt,status:'running',stage,anonymous:true,outputRoot};
  await writeJson('capture-status.json',manifest);
  try {
    if(stage==='all'||stage==='site') manifest.site=await captureSite();
    if(stage==='all'||stage==='feedback') manifest.feedback=await captureFeedback();
    manifest.status=failures.length ? 'finished-with-gaps':'captured-pending-human-review';
  } catch(error){ manifest.status='interrupted-with-gaps';failures.push({stage:'capture',error:sanitizeText(error.message)});process.exitCode=1; }
  finally {
    await writeJson('requests.json',requestLog);
    await writeJson('capture-status.json',{...manifest,finishedAt:new Date().toISOString(),totalBytes,requests:requestLog.length,failures});
    await dispatcher.close();
  }
})();
