'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
const {sanitizeText} = require('./audit.cjs');

const PRIVATE = /^\/api\/(?:v\d+\/)?(?:messages|workspace|admin|sessions)(?:\/|$)/;
const sensitive = /token|password|cookie|secret|email|api.?key|authorization/i;
function structure(value, depth = 0) {
  if (value === null) return 'null';
  if (depth > 5) return Array.isArray(value) ? 'array' : typeof value;
  if (Array.isArray(value)) return {type:'array',item:value.length ? structure(value[0],depth+1) : null};
  if (typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([key,item]) =>
    [key,sensitive.test(key) ? '[sensitive-field-omitted]' : structure(item,depth+1)]));
  return typeof value;
}

/** No body values, storage, headers or private labels are returned to the audit process. */
async function controls(page) {
  return page.evaluate(() => {
    const privateSurface=/^\/(messages|workspace|admin)/.test(location.pathname);
    const clean=value=>String(value??'').replace(/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi,'[redacted]');
    const list=[...document.querySelectorAll('button,input,textarea,select,a,[role="tab"],[role="button"],summary,[tabindex],[title]')].filter(e=>{
      const r=e.getBoundingClientRect();return r.width>0&&r.height>0&&getComputedStyle(e).visibility!=='hidden';
    }).map(e=>{
      const field=/^(INPUT|SELECT|TEXTAREA)$/.test(e.tagName),r=e.getBoundingClientRect();
      let label=e.getAttribute('aria-label')||e.getAttribute('title')||e.getAttribute('placeholder')||
        (field?[...(e.labels||[])].map(l=>l.innerText).join(' '):e.innerText);
      if(privateSurface && !e.hasAttribute('aria-label') && !e.hasAttribute('title') && !e.hasAttribute('placeholder') &&
        !/^(刷新|返回|编辑|删除|保存|取消|确定|关闭|展开|收起|启用|禁用|搜索|全部|未读|已读|标为已读|标记已读|标记全部已读|星标|设置|系统通知|用户互动|小说更新|帖子回复|评论回复|私信|选择|全选|上一页|下一页|跳转|API管理|Cookie管理|自助翻译|状态信息|清空队列|上传新书|新建配置|调度日志|待审核|已通过|已拒绝|全部请求|\d+)$/.test(label.trim())) label='[private-label-omitted]';
      let href=e.getAttribute('href');
      if(href){try{const u=new URL(href,location.href);href=u.origin+u.pathname+(u.search?'?{'+[...u.searchParams.keys()].join(',')+'}':'')}catch{href=null}}
      return {tag:e.tagName.toLowerCase(),role:e.getAttribute('role'),label:clean(label).trim().slice(0,120),
        icon:e.querySelector('svg')?.getAttribute('data-icon')||e.querySelector('.iconify')?.getAttribute('class')||null,
        type:e.getAttribute('type'),required:!!e.required,disabled:!!e.disabled,minimum:e.getAttribute('min'),maximum:e.getAttribute('max'),href,
        options:e.tagName==='SELECT'?[...e.options].map(o=>({label:clean(o.text),value:o.value})):undefined,
        bounds:{x:Math.round(r.x),y:Math.round(r.y),width:Math.round(r.width),height:Math.round(r.height)},inViewport:r.top<innerHeight&&r.bottom>0};
    });
    return {url:location.origin+location.pathname,title:clean(document.title),privateSurface,viewport:{width:innerWidth,height:innerHeight,touch:navigator.maxTouchPoints},controls:list};
  });
}

async function capture(page,{name,route,action,root='D:/NovalPie/agent-bridge/artifacts/beta7-baseline/20260905-mumu-web',screenshot=true}={}) {
  if(!/^[a-z0-9_-]+$/i.test(name))throw new Error('Evidence name required');
  const directory=path.resolve(root);
  if(!directory.toLowerCase().startsWith(path.resolve('D:/NovalPie/agent-bridge/artifacts/beta7-baseline').toLowerCase()+path.sep)) throw new Error('Unexpected evidence root');
  if(route && (!route.startsWith('/')||route.startsWith('//')))throw new Error('Only source-owned routes');
  const network=[],schemas=[],pending=[];
  const observe=response=>{
    const u=new URL(response.url());if(u.origin!=='https://novalpie.cc'||!u.pathname.startsWith('/api/'))return;
    const method=response.request().method();
    network.push({method,path:u.pathname,queryKeys:[...u.searchParams.keys()].filter(k=>!sensitive.test(k)),status:response.status()});
    if(method==='GET'&&!PRIVATE.test(u.pathname))pending.push(response.json().then(value=>schemas.push({method,path:u.pathname,status:response.status(),schema:structure(value)})).catch(()=>{}));
  };
  page.on('response',observe);
  try {
    if(route) {
      await page.goto('https://novalpie.cc'+route,{waitUntil:'domcontentloaded',timeout:25000});
      await page.waitForFunction(()=>document.title.trim().length>0,{},{timeout:12000}).catch(()=>{});
    }
    if(action)await action(page);
    await page.waitForTimeout(1500);
    const evidence=await controls(page);
    evidence.controls.forEach(control=>control.label=sanitizeText(control.label));
    evidence.sessionState='authenticated-native-webview';evidence.capturedAt=new Date().toISOString();
    evidence.level='rendered-controls';evidence.action=action?.name||null;
    let schemaTimer;
    await Promise.race([Promise.all(pending),new Promise(resolve=>{schemaTimer=setTimeout(resolve,5000)})]);
    clearTimeout(schemaTimer);
    evidence.network=network;evidence.schemas=schemas;
    await fs.mkdir(directory,{recursive:true});
    if(screenshot&&!evidence.privateSurface) {
      evidence.screenshot=path.join(directory,name+'.png');
      try {
        await page.screenshot({path:evidence.screenshot,scale:'css',timeout:8000,mask:[page.locator('input[type="email"],input[type="password"]')]});
      } catch {
        evidence.screenshot=null;
        evidence.screenshotFailure='capture-timeout-or-background-surface; visual verification pending';
      }
    }
    const file=path.join(directory,name+'.json');
    await fs.writeFile(file,JSON.stringify(evidence,null,2));
    return {file,url:evidence.url,title:evidence.title,viewport:evidence.viewport,controls:evidence.controls.length,network};
  } finally {page.removeListener('response',observe)}
}
module.exports={capture,controls,structure};
