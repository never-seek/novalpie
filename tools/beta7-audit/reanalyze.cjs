'use strict';
const fs=require('node:fs/promises');
const path=require('node:path');
const crypto=require('node:crypto');
const {analyzeChunk}=require('./audit.cjs');
const root=path.resolve(process.argv[2]);
const canonical = value => value.replace(/\{[^}]+\}/g,'{param}');
(async()=>{
  const manifest=JSON.parse(await fs.readFile(path.join(root,'site/manifest.json'),'utf8'));
  const analyses=[];
  const verifiedBases={};
  for(const entry of require('./http-wrapper-bases.json')) {
    const asset=manifest.assets.find(a=>a.url===entry.url);
    if(!asset) continue;
    const bytes=await fs.readFile(path.join(root,asset.file));
    if(crypto.createHash('sha256').update(bytes).digest('hex')!==entry.sha256) throw new Error('Reviewed HTTP wrapper changed; re-review required');
    verifiedBases[`${entry.url}#${entry.exportName}`]=entry.basePath;
  }
  for(const asset of manifest.assets.filter(a=>a.url.endsWith('.js'))) {
    analyses.push(analyzeChunk(asset.url,await fs.readFile(path.join(root,asset.file),'utf8'),{verifiedBases}));
  }
  const calls=analyses.flatMap(a=>a.apiCalls);
  const contracts=[...new Set(calls.map(c=>`${c.method} ${canonical(c.pathTemplate)}`))].sort().map(key=>{
    const sites=calls.filter(c=>`${c.method} ${canonical(c.pathTemplate)}`===key);
    return {key,method:sites[0].method,pathTemplate:canonical(sites[0].pathTemplate),queryFields:[...new Set(sites.flatMap(c=>c.queryFields))].sort(),bodyFields:[...new Set(sites.flatMap(c=>c.bodyFields))].sort(),evidenceLevel:'source-discovered',callSites:sites};
  });
  const controls=analyses.flatMap(a=>a.controls);
  const routes=[...new Set(analyses.flatMap(a=>a.routes).map(r=>r.path))].sort().map(route=>({path:route,definitions:analyses.flatMap(a=>a.routes).filter(r=>r.path===route)}));
  const result={analyzedAt:new Date().toISOString(),buildId:manifest.buildId,assets:manifest.assets.length,routes:routes.length,apiCandidates:contracts.length,controlCandidates:controls.length,parseErrors:analyses.flatMap(a=>a.parseErrors),runtimeVerifiedControls:0};
  await fs.writeFile(path.join(root,'site/api-candidates-v2.json'),JSON.stringify(contracts,null,2));
  await fs.writeFile(path.join(root,'site/control-candidates-v2.json'),JSON.stringify(controls,null,2));
  await fs.writeFile(path.join(root,'site/routes-v2.json'),JSON.stringify(routes,null,2));
  await fs.writeFile(path.join(root,'site/analysis-v2-summary.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify(result));
})();
