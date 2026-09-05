param(
    [string]$Session = 'novalpie-beta7-guest',
    [string]$Name,
    [string]$Route,
    [string]$ClickRole,
    [string]$ClickName,
    [string]$SessionState = 'authenticated-observed',
    [string]$OutputRoot = 'D:\NovalPie\agent-bridge\artifacts\beta7-baseline\20260905-browser',
    [switch]$Screenshot
)
$ErrorActionPreference = 'Stop'
if ($Name -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Use a simple evidence name' }
if ($Route -and ($Route -notmatch '^/' -or $Route.StartsWith('//'))) { throw 'Only source-relative routes are accepted' }
if ($ClickRole -and $ClickRole -notin @('button','link','tab')) { throw 'Unsupported control role' }
# Never call this with save/send/delete/purchase controls. This script records read-only panels.
if ($ClickName -match '删除|发送|发布|购买|支付|退出登录|清除|签到|提交|保存|应用') { throw 'Mutating controls are not part of a baseline probe' }
$captureDirectory = [System.IO.Path]::GetFullPath($OutputRoot)
if (!$captureDirectory.StartsWith('D:\NovalPie\agent-bridge\artifacts\beta7-baseline\', [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected evidence directory' }
New-Item -ItemType Directory -Force -Path $captureDirectory | Out-Null
$config = @{
    route=$Route; clickRole=$ClickRole; clickName=$ClickName; sessionState=$SessionState
    screenshot=if($Screenshot){Join-Path $captureDirectory "$Name.png"}else{$null}
} | ConvertTo-Json -Compress
function Get-PublicRequestMetadata {
    $lines = & npx --yes --package '@playwright/cli' playwright-cli --raw "--session=$Session" requests
    foreach($line in $lines) {
        if($line -notmatch '^(\d+)\. \[(GET|POST|PUT|PATCH|DELETE|HEAD)\] (https://novalpie\.cc/api/[^ ]+) => \[(\d+)\]') {continue}
        $requestIndex=[int]$Matches[1];$method=$Matches[2];$rawUri=$Matches[3];$status=[int]$Matches[4]
        $uri=[Uri]$rawUri
        $queryKeys=if($uri.Query){($uri.Query.TrimStart('?') -split '&' | ForEach-Object {($_ -split '=',2)[0]}) -join ','}else{''}
        [pscustomobject]@{index=$requestIndex;method=$method;url=$uri.GetLeftPart([UriPartial]::Path);queryKeys=$queryKeys;status=$status;evidenceLevel='browser-response-status'}
    }
}
$beforeRequests=@(Get-PublicRequestMetadata)
$lastRequestIndex=if($beforeRequests.Count){($beforeRequests | Measure-Object index -Maximum).Maximum}else{0}
$probe = @'
async (page) => {
  const config = CONFIG_PLACEHOLDER;
  const requests=[];
  const responses=[];
  const schemas=[];
  const pendingSchemas=[];
  const safeUrl=raw=>{const url=new URL(raw);return url.origin+url.pathname+(url.search?'?{'+[...url.searchParams.keys()].join(',')+'}':'')};
  const sourceApi=raw=>{try{const u=new URL(raw);return u.origin==='https://novalpie.cc'&&u.pathname.startsWith('/api/')}catch{return false}};
  const schema=(value,depth=0)=>{if(value===null)return 'null';if(depth>3)return Array.isArray(value)?'array':typeof value;if(Array.isArray(value))return {type:'array',item:value.length?schema(value[0],depth+1):null};if(typeof value==='object')return Object.fromEntries(Object.entries(value).map(([key,item])=>[key,/token|password|cookie|key|secret|email/i.test(key)?'[sensitive-field-omitted]':schema(item,depth+1)]));return typeof value};
  const onRequest=request=>{if(sourceApi(request.url()))requests.push({url:safeUrl(request.url()),method:request.method(),resourceType:request.resourceType()})};
  const onResponse=response=>{
    if(!sourceApi(response.url()))return;
    responses.push({url:safeUrl(response.url()),status:response.status(),method:response.request().method()});
    if(response.request().method()==='GET' && !/\/messages|\/workspace|\/admin|\/sessions/.test(new URL(response.url()).pathname)) {
      pendingSchemas.push(response.json().then(value=>{schemas.push({url:safeUrl(response.url()),schema:schema(value)})}).catch(()=>{}));
    }
  };
  page.on('request',onRequest);page.on('response',onResponse);
  try {
  if(config.route) {
    await page.goto('https://novalpie.cc'+config.route,{waitUntil:'domcontentloaded'});
    await page.locator('main').first().waitFor({state:'visible',timeout:12000}).catch(()=>{});
  }
  const beforeUrl=page.url();
  if(config.clickRole && config.clickName) {
    await page.getByRole(config.clickRole,{name:config.clickName,exact:true}).click({timeout:7000});
  }
  // Site JSON can arrive after the shell. Wait for a short control-count stabilization, bounded.
  let previous=''; let stable=0;
  for(let i=0;i<12 && stable<3;i++) {
    const fingerprint=await page.locator('main').first().evaluate(e=>e.querySelectorAll('button,input,a,select').length+':'+e.innerText.length).catch(()=>'');
    stable=fingerprint===previous?stable+1:0; previous=fingerprint;
    await page.waitForTimeout(300);
  }
  const evidence = await page.evaluate(() => {
    const redact=s=>String(s??'').replace(/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi,'[REDACTED]').replace(/\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}(?:\.[A-Za-z0-9_-]+)?/g,'[REDACTED]');
    const safeHref=raw=>{try{const url=new URL(raw,location.href);return url.origin+url.pathname+(url.search?'?{'+[...url.searchParams.keys()].join(',')+'}':'')}catch{return null}};
    const privateSurface=/^\/(messages|workspace|admin)/.test(location.pathname);
    const controls=Array.from(document.querySelectorAll('button,input,select,textarea,a,[role="button"],[role="tab"]')).filter(e=>{const b=e.getBoundingClientRect();return b.width>0 && b.height>0 && getComputedStyle(e).visibility!=='hidden'}).map(e=>{
      const bounds=e.getBoundingClientRect();
      const isField=/^(INPUT|TEXTAREA|SELECT)$/.test(e.tagName);
      let label=e.getAttribute('aria-label')||e.getAttribute('title')||e.getAttribute('placeholder')||(!isField?e.innerText:'')||'';
      if(privateSurface) {
        if(e.tagName==='A') label='[private-link-label-omitted]';
        else if(!e.hasAttribute('aria-label')&&!e.hasAttribute('title')&&!e.hasAttribute('placeholder') && !/^(刷新|返回|编辑|删除|保存|取消|确定|关闭|展开|收起|启用|禁用|搜索|全部|未读|已读|标为已读|标记已读|标记全部已读|星标|设置|系统通知|用户互动|小说更新|帖子回复|评论回复|私信|选择|全选|上|下|上一页|下一页|跳转|API管理|Cookie管理|自助翻译|状态信息|清空队列|上传新书|新建配置|调度日志|待审核|已通过|已拒绝|全部请求|一键通过及忽略|\d+)$/.test(label.trim())) label='[private-dynamic-label-omitted]';
      }
      return {tag:e.tagName.toLowerCase(),role:e.getAttribute('role'),label:redact(label).trim().slice(0,160),type:e.getAttribute('type'),name:e.getAttribute('name'),id:e.id||null,disabled:Boolean(e.disabled)||e.getAttribute('aria-disabled')==='true',required:Boolean(e.required),checked:isField&&e.type==='checkbox'?Boolean(e.checked):null,minimum:e.getAttribute('min'),maximum:e.getAttribute('max'),href:e.hasAttribute('href')?safeHref(e.getAttribute('href')):null,options:e.tagName==='SELECT'?Array.from(e.options).map(o=>({label:redact(o.text),value:o.value,disabled:o.disabled})):undefined,bounds:{x:Math.round(bounds.x),y:Math.round(bounds.y),width:Math.round(bounds.width),height:Math.round(bounds.height)},inViewport:bounds.bottom>0&&bounds.top<innerHeight};
    });
    return {url:safeHref(location.href),title:redact(document.title),viewport:{width:innerWidth,height:innerHeight,touch:navigator.maxTouchPoints,userAgent:navigator.userAgent},headings:privateSurface?[]:Array.from(document.querySelectorAll('h1,h2,h3,h4')).map(e=>redact(e.innerText).trim()).filter(Boolean),controls,privateSurface};
  });
  if(config.screenshot && !evidence.privateSurface) {
    await page.screenshot({path:config.screenshot,scale:'css',mask:[page.locator('input[type="password"]'),page.locator('input[type="email"]')]});
  }
  await Promise.all(pendingSchemas);
  return {...evidence,requests,responses,schemas,sessionState:config.sessionState,evidenceLevel:'rendered-controls',capturedAt:new Date().toISOString(),action:config.clickName?{role:config.clickRole,name:config.clickName,beforeUrl,afterUrl:page.url()}:null,screenshot:config.screenshot&&!evidence.privateSurface?config.screenshot:null};
  } finally {page.removeListener('request',onRequest);page.removeListener('response',onResponse)}
}
'@
$probe = $probe.Replace('CONFIG_PLACEHOLDER', $config)
$probeFile = Join-Path $captureDirectory "$Name.probe.js"
$probe | Set-Content -LiteralPath $probeFile -Encoding utf8
& node --check $probeFile
if ($LASTEXITCODE -ne 0) { throw 'Invalid probe code; browser was not changed' }
$rawResult = & npx --yes --package '@playwright/cli' playwright-cli --raw "--session=$Session" run-code --filename $probeFile
if ($LASTEXITCODE -ne 0) { throw "Browser probe failed: $rawResult" }
$result = ($rawResult -join "`n") | ConvertFrom-Json
if (!$result.controls -or !$result.capturedAt) { throw 'Probe did not return structured page evidence' }
$afterRequests=@(Get-PublicRequestMetadata | Where-Object {$_.index-gt$lastRequestIndex})
$result | Add-Member -NotePropertyName networkStatusEvidence -NotePropertyValue $afterRequests -Force
$result | Add-Member -NotePropertyName intendedRoute -NotePropertyValue $Route -Force
$result | ConvertTo-Json -Depth 14 | Set-Content -LiteralPath (Join-Path $captureDirectory "$Name.json") -Encoding utf8
[pscustomobject]@{Name=$Name;Url=$result.url;Title=$result.title;Session=$result.sessionState;Controls=$result.controls.Count;Viewport="$($result.viewport.width)x$($result.viewport.height)";Evidence=(Join-Path $captureDirectory "$Name.json")} | ConvertTo-Json -Compress
