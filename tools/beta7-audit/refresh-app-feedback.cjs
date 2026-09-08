'use strict';
// Anonymous GET-only incremental evidence. Never touches the user's browser or credentials.
const fs = require('node:fs/promises');
const path = require('node:path');
const {fetch, ProxyAgent} = require('undici');
const {ORIGIN, assertPublicReadUrl, safePost, flattenComments, collectPages, sanitizeText} = require('./audit.cjs');
const root = path.resolve(process.argv[2]);
if (!root.startsWith('D:\\NovalPie\\agent-bridge\\artifacts\\')) throw new Error('Expected project evidence directory');
const proxy = new ProxyAgent('http://127.0.0.1:7890');
const requests = [];
async function get(relative, json = true) {
  const url = assertPublicReadUrl(ORIGIN + relative);
  const response = await fetch(url, {dispatcher: proxy, redirect: 'error', signal: AbortSignal.timeout(25000),
    headers: {'user-agent': 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/128.0 Mobile Safari/537.36'}});
  requests.push({path: relative, method: 'GET', status: response.status});
  if (!response.ok) { await response.body?.cancel(); throw new Error(`HTTP ${response.status}`); }
  const chunks = []; let size = 0;
  for await (const chunk of response.body) { size += chunk.length; if (size > 10 * 1024 * 1024) throw new Error('Capture limit'); chunks.push(chunk); }
  const text = Buffer.concat(chunks).toString('utf8');
  return json ? JSON.parse(text) : text;
}
(async () => {
  await fs.mkdir(root, {recursive: true});
  const summary = {at: new Date().toISOString(), session: 'anonymous', requests};
  try {
    const html = await get('/', false);
    summary.buildId = html.match(/buildId:"([^"]+)"/)?.[1] ?? null;
    const feed = await get('/api/posts?type=feedback&page=1&limit=50');
    summary.feedbackTotal = feed.pagination?.total;
    await fs.writeFile(path.join(root, 'feedback-latest.json'), JSON.stringify({posts: feed.posts.map(safePost), pagination: feed.pagination}, null, 2));
    for (const item of feed.posts.filter(item => Number(item.id) > 1920)) {
      const detail = await get(`/api/posts/${item.id}`);
      const threadPost = safePost(detail.post ?? detail.data?.post ?? detail.data ?? detail);
      const replies = await collectPages(p => get(`/api/posts/${item.id}/comments?page=${p}&limit=50`), 'comments');
      await fs.writeFile(path.join(root, `feedback-${item.id}.json`), JSON.stringify({post: threadPost,
        comments: flattenComments(item.id, replies.items), complete: replies.complete, gaps: replies.gaps}, null, 2));
    }
    const raw = await get('/api/posts/1871');
    const post = safePost(raw.post ?? raw.data?.post ?? raw.data ?? raw);
    const page = await collectPages(p => get(`/api/posts/1871/comments?page=${p}&limit=50`), 'comments');
    const comments = flattenComments(1871, page.items);
    const nested = page.items.filter(c => c.replies_pagination || c.has_more_replies || c.replies_has_more).map(c => c.id);
    summary.complete = page.complete && nested.length === 0;
    summary.comments = comments.length;
    summary.latest = comments.map(c => c.createdAt).filter(Boolean).sort().at(-1);
    await fs.writeFile(path.join(root, 'app-thread-1871.json'), JSON.stringify({post, comments, gaps: page.gaps, nestedPaging: nested, complete: summary.complete}, null, 2));
  } catch (error) { summary.error = sanitizeText(error.message); summary.complete = false; process.exitCode = 1; }
  finally { await fs.writeFile(path.join(root, 'summary.json'), JSON.stringify(summary, null, 2)); await proxy.close(); }
  console.log(JSON.stringify(summary));
})();
