'use strict';
const ts = require('typescript');

const ORIGIN = 'https://novalpie.cc';
const API_PREFIX = /^\/(?:api\/)?(?:v\d+\/)?(?:admin|sessions|users|posts|comments|novels|chapters|reader|favorites|tags|messages|workspace|shop|downloads|translations|recommendations|reports|verification-codes|password-resets|political-exam|health|uploads)(?:\/|\?|$)/;

function assertPublicReadUrl(value, method = 'GET') {
  const url = new URL(value);
  const allowedPath = url.pathname === '/' ||
    /^\/_nuxt\/[A-Za-z0-9._-]+\.(?:js|css)$/.test(url.pathname) ||
    /^\/api\/posts(?:\/\d+(?:\/comments)?)?$/.test(url.pathname);
  if (method !== 'GET' || url.origin !== ORIGIN || url.username || url.password || !allowedPath) {
    throw new Error(`Not an allowed public read: ${url.origin}${url.pathname}`);
  }
  return url;
}

function sanitizeText(value) {
  return String(value ?? '')
    .replace(/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----[\s\S]*?(?:-----END [^-]+-----|$)/g, '[REDACTED]')
    .replace(/\b(?:Bearer\s+)[A-Za-z0-9._~+\/-]+=*/gi, 'Bearer [REDACTED]')
    .replace(/\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}(?:\.[A-Za-z0-9_-]+)?/g, '[REDACTED]')
    .replace(/\bgh[pousr]_[A-Za-z0-9]{20,}|\bgithub_pat_[A-Za-z0-9_]{20,}|\bsk-[A-Za-z0-9_-]{16,}/g, '[REDACTED]')
    .replace(/\b(?:set-cookie|cookie)\s*:\s*[^\r\n<]+/gi, 'Cookie: [REDACTED]')
    .replace(/((?:password|passwd|secret|api[_-]?key|access[_-]?token|refresh[_-]?token|csrf[_-]?token|authorization|cookie|密码|口令|密钥)\s*[=:：]\s*)(?:"[^"]*"|'[^']*'|[^\s,;<>]+)/gi, '$1[REDACTED]')
    .replace(/([?&](?:token|key|signature|auth|password|secret)=)[^&#\s)]+/gi, '$1[REDACTED]')
    .replace(/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi, '[REDACTED]');
}

function safePost(raw) {
  return {
    id: Number(raw.id),
    type: sanitizeText(raw.type),
    title: sanitizeText(raw.title),
    content: sanitizeText(raw.content),
    authorName: sanitizeText(raw.author_name ?? raw.authorName ?? raw.username),
    createdAt: raw.created_at ?? raw.createdAt ?? null,
    updatedAt: raw.updated_at ?? raw.updatedAt ?? null,
    commentCount: raw.comment_count ?? raw.commentCount ?? null,
    replyCount: raw.reply_count ?? raw.replyCount ?? null,
    hasPoll: Boolean(raw.has_poll ?? raw.hasPoll),
  };
}

function flattenComments(postId, roots) {
  const result = [];
  const visit = (items, rootId, depth, parentKey) => {
    for (const raw of items ?? []) {
      const id = raw.id ?? raw.comment_id ?? raw.reply_id;
      const key = `${parentKey}/${depth === 0 ? 'comment' : 'reply'}:${id}`;
      result.push({
        key, id, postId, rootCommentId: rootId ?? id, parentKey: depth ? parentKey : null, depth,
        content: sanitizeText(raw.content),
        authorName: sanitizeText(raw.author_name ?? raw.authorName ?? raw.user_name ?? raw.username),
        createdAt: raw.created_at ?? raw.createdAt ?? null,
        updatedAt: raw.updated_at ?? raw.updatedAt ?? null,
      });
      visit(raw.replies ?? raw.children, rootId ?? id, depth + 1, key);
    }
  };
  visit(roots, null, 0, `post:${postId}`);
  return result;
}

async function collectPages(fetchPage, arrayKey, maxPages = 500) {
  const items = new Map();
  const pages = [];
  const gaps = [];
  let expectedPages = 1;
  let expectedTotal = 0;
  let duplicates = 0;
  for (let page = 1; page <= expectedPages && page <= maxPages; page++) {
    let response;
    try { response = await fetchPage(page); }
    catch (error) { gaps.push({page, error: sanitizeText(error.message)}); continue; }
    const payload = response?.data?.[arrayKey] ? response.data : response;
    const batch = payload?.[arrayKey];
    const pagination = payload?.pagination ?? response?.pagination;
    if (!Array.isArray(batch) || !pagination || response?.success === false) {
      gaps.push({page, error: 'Missing list/pagination or rejected response'});
      continue;
    }
    const total = Number(pagination.total ?? 0);
    const limit = Number(pagination.limit ?? pagination.page_size ?? 0);
    const pageCount = Number(pagination.pages ?? pagination.total_pages ?? (limit > 0 ? Math.ceil(total / limit) : 0));
    if (!Number.isFinite(pageCount) || pageCount < 0 || !Number.isFinite(total) || total < 0) {
      gaps.push({page, error: 'Invalid pagination metadata'});
      continue;
    }
    expectedPages = Math.max(expectedPages, pageCount);
    expectedTotal = Math.max(expectedTotal, total);
    if (Number(pagination.page ?? page) !== page) gaps.push({page, error: 'Server returned a different page'});
    if (batch.length === 0 && total > 0 && page <= pageCount) gaps.push({page, error: 'Empty page before advertised end'});
    pages.push({page, itemCount: batch.length, pagination});
    for (const item of batch) {
      const id = item.id;
      if (id == null) { gaps.push({page, error: 'Item missing id'}); continue; }
      if (items.has(id)) duplicates++;
      items.set(id, item);
    }
  }
  if (expectedPages > maxPages) gaps.push({error: 'Page safety limit reached', expectedPages, maxPages});
  if (items.size < expectedTotal) gaps.push({error: 'Unique count below advertised total', unique: items.size, expectedTotal});
  return {items: [...items.values()], pages, expectedTotal, expectedPages, duplicates, gaps, complete: gaps.length === 0};
}

function classifyFeedbackCandidate(text) {
  const rules = {
    app: /\bapp\b|apk|安卓|原生|客户端|安装包|模拟器/i,
    reader: /阅读器|翻页|页眉|页脚|滚动|截断|音量键|断点|横屏|字号|字体|行距/i,
    tts: /tts|听书|语音|朗读|发声|音色/i,
    replacement: /替换|术语|全局规则|公共规则/i,
    download: /epub|txt|下载|压缩|缺图|插图|插画|插画|图片失败/i,
    forum: /论坛|评论|回复|投票|黑幕|剧透|fold|超链接|书卡|表情/i,
    search: /搜索|筛选|标签|收藏|书架|阅读进度/i,
    account: /登录|登陆|签到|头像|徽章|badge|背包|私信|消息|个人主页|装扮/i,
    authoring: /上传|编辑器|目录|分章|翻译|工作区|获取新章|添加新书/i,
    reliability: /卡顿|掉帧|卡死|闪退|白屏|无法|不能|失败|bug|错误|异常|超时|空白|报错/i,
  };
  const topics = Object.entries(rules).filter(([, rule]) => rule.test(text)).map(([name]) => name);
  return {topics, reviewStatus: 'needs-human-review', verdict: null};
}

function analyzeChunk(url, source, {verifiedBases={}}={}) {
  const file = ts.createSourceFile(url, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.JS);
  const dependencies = new Set();
  const routes = [];
  const apiCalls = [];
  const controls = [];
  const importedBases=new Map();
  const nodeScopes = new WeakMap();
  const walk = (node, visit) => { visit(node); ts.forEachChild(node, child => walk(child, visit)); };
  const textOf = (node, max = 600) => node ? sanitizeText(node.getText(file).slice(0, max)) : null;
  const keyOf = node => ts.isIdentifier(node) || ts.isStringLiteral(node) ? node.text : node.getText(file);
  const bind = (node, parentScope) => {
    const scope = ts.isSourceFile(node) || ts.isBlock(node) || ts.isFunctionLike(node)
      ? {parent:parentScope,bindings:new Map()} : parentScope;
    nodeScopes.set(node,scope);
    if (ts.isVariableDeclaration(node) && ts.isIdentifier(node.name)) scope.bindings.set(node.name.text,node.initializer ?? null);
    if (ts.isParameter(node) && ts.isIdentifier(node.name)) scope.bindings.set(node.name.text,null);
    ts.forEachChild(node, child => bind(child,scope));
  };
  bind(file,null);
  for(const statement of file.statements) {
    if(!ts.isImportDeclaration(statement) || !ts.isStringLiteral(statement.moduleSpecifier)) continue;
    const moduleUrl=new URL(statement.moduleSpecifier.text,url).href;
    const imports=statement.importClause?.namedBindings;
    if(imports && ts.isNamedImports(imports)) for(const item of imports.elements) {
      const externalName=(item.propertyName??item.name).text;
      const base=verifiedBases[`${moduleUrl}#${externalName}`];
      if(base) importedBases.set(item.name.text,base);
    }
  }
  const resolve = (node, seen = new Set()) => {
    if (node && ts.isIdentifier(node) && !seen.has(node)) {
      for(let scope=nodeScopes.get(node);scope;scope=scope.parent) {
        if(!scope.bindings.has(node.text)) continue;
        const init=scope.bindings.get(node.text);
        if(!init) return node;
        seen.add(node);
        return resolve(init,seen);
      }
    }
    return node;
  };
  const props = node => {
    node = resolve(node);
    return ts.isObjectLiteralExpression(node ?? {}) ? new Map(node.properties.filter(p => p.name).map(p => [keyOf(p.name), p])) : new Map();
  };
  const valueOf = (node, key) => { const p = props(node).get(key); return p?.initializer ?? (p && ts.isShorthandPropertyAssignment(p) ? p.name : null); };
  const literal = node => ts.isStringLiteralLike(node ?? {}) ? node.text : undefined;
  const expressionTemplate = node => {
    if (!node) return '';
    if(ts.isIdentifier(node)) {
      const expanded=resolve(node);
      if(expanded!==node && ts.isStringLiteralLike(expanded)) return expanded.text;
    }
    if (ts.isStringLiteralLike(node) || ts.isNumericLiteral(node)) return node.text;
    if (ts.isTemplateExpression(node)) return node.head.text + node.templateSpans.map(span => expressionTemplate(span.expression) + span.literal.text).join('');
    if (ts.isBinaryExpression(node) && node.operatorToken.kind === ts.SyntaxKind.PlusToken) return expressionTemplate(node.left) + expressionTemplate(node.right);
    if (ts.isCallExpression(node) && ts.isPropertyAccessExpression(node.expression) && node.expression.name.text === 'concat') {
      return expressionTemplate(node.expression.expression) + node.arguments.map(expressionTemplate).join('');
    }
    return `{${node.getText(file).slice(0, 100)}}`;
  };
  const apiPath = node => {
    let template = expressionTemplate(node);
    if (template.startsWith(ORIGIN)) template = template.slice(ORIGIN.length);
    // apiBase is an expression; only the following source-owned path is a contract candidate.
    if (template.startsWith('{') && template.includes('}/')) template = template.slice(template.indexOf('}/') + 1);
    if (!API_PREFIX.test(template)) return null;
    return template.startsWith('/api/') ? template : `/api${template}`;
  };
  const requestFields = (node, visited = new Set()) => {
    const resolved = resolve(node);
    if (!resolved || !ts.isObjectLiteralExpression(resolved) || visited.has(resolved)) return {fields:[],unresolvedSpreads:[]};
    visited.add(resolved);
    const fields=[];
    const unresolvedSpreads=[];
    for(const property of resolved.properties) {
      if(ts.isSpreadAssignment(property)) {
        const spread=resolve(property.expression);
        if(spread && ts.isObjectLiteralExpression(spread)) {
          const nested=requestFields(spread,visited);
          fields.push(...nested.fields);unresolvedSpreads.push(...nested.unresolvedSpreads);
        } else unresolvedSpreads.push(textOf(property.expression));
      } else if(property.name) fields.push(keyOf(property.name));
    }
    return {fields:[...new Set(fields)],unresolvedSpreads};
  };
  const strings = node => {
    const values = [];
    if (node) walk(node, child => { if (ts.isStringLiteralLike(child)) values.push(child.text); });
    return [...new Set(values)];
  };
  const labelStrings = node => {
    if (!node) return [];
    if (ts.isStringLiteralLike(node)) return [sanitizeText(node.text.trim())].filter(Boolean);
    if (ts.isArrayLiteralExpression(node)) return node.elements.flatMap(labelStrings);
    if (ts.isCallExpression(node)) return node.arguments.slice(2).flatMap(labelStrings);
    if (ts.isBinaryExpression(node)) return labelStrings(node.left).concat(labelStrings(node.right));
    if (ts.isParenthesizedExpression(node)) return labelStrings(node.expression);
    if (ts.isConditionalExpression(node)) return labelStrings(node.whenTrue).concat(labelStrings(node.whenFalse));
    return [];
  };
  const functionName = node => {
    for (let p = node.parent; p; p = p.parent) {
      if (ts.isMethodDeclaration(p) || ts.isFunctionDeclaration(p)) return p.name ? keyOf(p.name) : '(anonymous)';
      if (ts.isPropertyAssignment(p)) return keyOf(p.name);
      if (ts.isVariableDeclaration(p) && ts.isIdentifier(p.name)) return p.name.text;
    }
    return null;
  };
  const conditionsFor = node => {
    const conditions = [];
    let child = node;
    for (let p = node.parent; p && !ts.isFunctionLike(p); child = p, p = p.parent) {
      if (ts.isConditionalExpression(p) && child !== p.condition) {
        conditions.push((child === p.whenFalse ? 'NOT ' : '') + textOf(p.condition, 300));
      }
      if (ts.isIfStatement(p)) conditions.push(textOf(p.expression, 300));
    }
    return conditions;
  };
  walk(file, node => {
    if (ts.isStringLiteralLike(node) && /^(?:\.\/|\/_nuxt\/)[A-Za-z0-9._-]+\.(?:js|css)$/.test(node.text)) {
      const dependency = new URL(node.text, url);
      if (dependency.origin === ORIGIN && dependency.pathname.startsWith('/_nuxt/')) dependencies.add(dependency.href);
    }
    if (ts.isObjectLiteralExpression(node)) {
      const path = literal(valueOf(node, 'path'));
      const name = literal(valueOf(node, 'name'));
      const component = valueOf(node, 'component');
      if (path?.startsWith('/') && name && component) {
        const metadata = valueOf(node, 'meta');
        routes.push({name, path, middleware: strings(valueOf(metadata, 'middleware')), metaExpression: textOf(metadata), componentDependencies: strings(component).filter(x => x.endsWith('.js')), sourceUrl: url, offset: node.pos, evidenceLevel: 'source-discovered'});
      }
    }
    if (!ts.isCallExpression(node)) return;
    const args = node.arguments;
    const pathTemplate = args[0] ? apiPath(args[0]) : null;
    const options = args[1];
    const optionProps = props(options);
    const wrapperMethod=ts.isPropertyAccessExpression(node.expression) && /^(get|post|put|patch|delete|head)$/i.test(node.expression.name.text)
      ? node.expression.name.text.toUpperCase() : null;
    if(pathTemplate && wrapperMethod && !optionProps.has('method')) {
      const fields=requestFields(options);
      const receiver=node.expression.expression;
      const verifiedBase=ts.isIdentifier(receiver)?importedBases.get(receiver.text):null;
      const hasExplicitApi=expressionTemplate(args[0]).startsWith('/api/');
      const resolvedPath=verifiedBase&&!hasExplicitApi?verifiedBase+pathTemplate.slice(4):pathTemplate;
      apiCalls.push({pathTemplate:resolvedPath,method:wrapperMethod,baseResolution:verifiedBase?'verified-import-base':hasExplicitApi?'explicit-api-prefix':'wrapper-base-unverified',queryFields:wrapperMethod==='GET'?fields.fields:[],bodyFields:wrapperMethod==='GET'?[]:fields.fields,unresolvedQuerySpreads:wrapperMethod==='GET'?fields.unresolvedSpreads:[],unresolvedBodySpreads:wrapperMethod==='GET'?[]:fields.unresolvedSpreads,functionName:functionName(node),callExpression:textOf(node,1400),sourceUrl:url,offset:node.pos,evidenceLevel:'source-discovered'});
    }
    if (pathTemplate && ['method', 'headers', 'body', 'query', 'params'].some(key => optionProps.has(key))) {
      const methodNode = valueOf(options, 'method');
      const method = methodNode ? literal(methodNode) ?? 'dynamic' : 'GET';
      const query=requestFields(valueOf(options,'query') ?? valueOf(options,'params'));
      const body=requestFields(valueOf(options,'body'));
      apiCalls.push({pathTemplate, method: method.toUpperCase() === 'DYNAMIC' ? 'dynamic' : method.toUpperCase(), queryFields:query.fields, bodyFields:body.fields, unresolvedQuerySpreads:query.unresolvedSpreads, unresolvedBodySpreads:body.unresolvedSpreads, queryExpression: textOf(valueOf(options, 'query')), bodyExpression: textOf(valueOf(options, 'body')), functionName: functionName(node), callExpression: textOf(node, 1400), sourceUrl: url, offset: node.pos, evidenceLevel: 'source-discovered'});
    }
    const tag = literal(args[0]);
    if (!['button', 'input', 'select', 'textarea', 'a', 'summary', 'option'].includes(tag)) return;
    const attributes = args[1];
    const attributesMap = props(attributes);
    const events = {};
    for (const [key, property] of attributesMap) {
      if (/^on[A-Z]|^onUpdate:/.test(key)) events[key] = textOf(property.initializer ?? property.name);
    }
    const labels = [...new Set([...labelStrings(args[2]), ...['title', 'aria-label', 'placeholder'].map(key => literal(valueOf(attributes, key))).filter(Boolean)])].map(sanitizeText);
    controls.push({tag, labels, attributesReference: ts.isIdentifier(attributes ?? {}) ? attributes.text : null, events, disabled: textOf(valueOf(attributes, 'disabled')), href: textOf(valueOf(attributes, 'href')), conditions: conditionsFor(node), sourceUrl: url, offset: node.pos, evidenceLevel: 'source-discovered', runtimeStatus: 'pending'});
  });
  return {sourceUrl: url, dependencies: [...dependencies].sort(), routes, apiCalls, controls, parseErrors: file.parseDiagnostics.map(d => ({offset:d.start,message:ts.flattenDiagnosticMessageText(d.messageText, ' ')}))};
}

module.exports = { ORIGIN, assertPublicReadUrl, sanitizeText, safePost, flattenComments, collectPages, analyzeChunk, classifyFeedbackCandidate };
