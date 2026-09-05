'use strict';
const fs=require('node:fs/promises');
const path=require('node:path');

function normalizePath(value) {
  return value.split('?')[0].replace(/\$\{[^}]+\}|\$[A-Za-z_]\w*|\{[^}]+\}/g,'{param}');
}

/** Native source references only. Presence is not a protocol or runtime verification result. */
function extractNativeCalls(source,file='app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt') {
  const functions=[...source.matchAll(/(?:suspend\s+)?fun\s+([A-Za-z_]\w*)\s*\(/g)];
  const calls=[];
  for(let i=0;i<functions.length;i++) {
    const start=functions[i].index,end=functions[i+1]?.index??source.length;
    const body=source.slice(start,end);
    for(const match of body.matchAll(/\b(get|post|put|patch|delete|postUnauthenticated|putUnauthenticated)\(\s*"([^"\r\n]+)"/g)) {
      if(!match[2].startsWith('/api/'))continue;
      const offset=start+match.index;
      calls.push({method:match[1].replace('Unauthenticated','').toUpperCase(),pathTemplate:normalizePath(match[2]),
        nativeFunction:functions[i][1],file,line:source.slice(0,offset).split('\n').length,
        parameterKeyCandidates:[...new Set([...body.matchAll(/(?:\.put\(\s*|[,(]\s*)"([a-z][a-z0-9_]*)"\s*(?:,|to\s)/g)].map(x=>x[1]))],
        status:'native-source-reference'});
    }
  }
  return calls;
}

async function generate(evidenceRoot,repositoryRoot) {
  const calls=extractNativeCalls(await fs.readFile(path.join(repositoryRoot,'app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt'),'utf8'));
  const website=JSON.parse(await fs.readFile(path.join(evidenceRoot,'site/api-ledger.json'),'utf8'));
  const mapped=website.map(api=>({id:api.id,method:api.method,path:api.pathTemplate,queryFields:api.queryFields,bodyFields:api.bodyFields,
    source:api.callSites.map(site=>({url:site.sourceUrl,offset:site.offset,baseResolution:site.baseResolution??'expression'})),
    native:calls.filter(call=>call.method===api.method && call.pathTemplate===normalizePath(api.pathTemplate)),
    readEvidence:api.readEvidence,writeEvidence:api.writeEvidence,status:'source-map; protocol/runtime verification pending'}));
  const nativeOnly=calls.filter(call=>!website.some(api=>api.method===call.method&&normalizePath(api.pathTemplate)===call.pathTemplate));
  const report={generatedAt:new Date().toISOString(),kind:'static-correspondence-not-verification',websiteCandidates:mapped,nativeOutsideStaticCandidates:nativeOnly};
  const output=path.join(evidenceRoot,'site/native-api-correspondence.json');
  await fs.writeFile(output,JSON.stringify(report,null,2));
  const rows=mapped.map(api=>`| ${api.id} | ${api.method} | \`${api.path.replace(/\|/g,'\\|')}\` | ${api.native.length?api.native.map(n=>`\`${n.nativeFunction}:${n.line}\``).join(', '):'未匹配/需人工解析wrapper'} |`);
  const markdown=['# 网站接口与原生代码静态对应','',
    '自动索引，仅说明方法/路径字符串有对应关系，不代表参数、权限、响应或实际写入已经验证。动态wrapper与旧兼容分支待人工核对。',
    '',`来源：${website.length}个当前站点前端候选；原生数据层${calls.length}个直接调用引用。缺失项可能是间接调用，不能仅凭本表判定未实现。`,
    '', '| ID | 方法 | 网站路径 | 原生函数:行 |','|---|---|---|---|',...rows,
    '',`另有${nativeOnly.length}个原生调用未被当前网站静态提取器对应，完整清单在外部证据site/native-api-correspondence.json；包含reader/session-key等间接路径，须继续补源站映射。`,''].join('\n');
  await fs.writeFile(path.join(repositoryRoot,'docs/beta7/api-source-map.md'),markdown);
  return {output,website:website.length,nativeCalls:calls.length,matched:mapped.filter(x=>x.native.length).length,unmatched:mapped.filter(x=>!x.native.length).length,nativeOnly:nativeOnly.length};
}
if(require.main===module)generate(path.resolve(process.argv[2]),path.resolve(process.argv[3])).then(value=>console.log(JSON.stringify(value)));
module.exports={extractNativeCalls,normalizePath,generate};
