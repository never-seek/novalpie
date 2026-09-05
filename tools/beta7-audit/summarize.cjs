'use strict';
const fs=require('node:fs/promises');
const path=require('node:path');
const crypto=require('node:crypto');
const {sanitizeText,matchReadEvidence}=require('./audit.cjs');
const sourceRoot=path.resolve(process.argv[2]);
const browserRoot=path.resolve(process.argv[3]);
(async()=>{
  const sourceSummary=JSON.parse(await fs.readFile(path.join(sourceRoot,'site/analysis-v2-summary.json'),'utf8'));
  const contracts=JSON.parse(await fs.readFile(path.join(sourceRoot,'site/api-candidates-v2.json'),'utf8'));
  const pages=[];
  for(const name of await fs.readdir(browserRoot)) {
    if(!name.endsWith('.json'))continue;
    const page=JSON.parse(await fs.readFile(path.join(browserRoot,name),'utf8'));
    if(!Array.isArray(page.controls))continue;
    // Older probe revisions excluded field values but captured dynamic button labels. Never retain
    // these on private/configuration surfaces; the source AST still provides static control names.
    if(page.privateSurface) {
      for(const control of page.controls) {
        control.label=sanitizeText(control.label);
        if(control.label.length>48 || /(?:secret|token|password|api.?key|cookie)\s*[=:]/i.test(control.label))control.label='[private-dynamic-label-omitted]';
      }
      page.headings=[];
      await fs.writeFile(path.join(browserRoot,name),JSON.stringify(page,null,2));
    }
    pages.push({file:name,url:page.url,intendedRoute:page.intendedRoute??null,title:page.title,sessionState:page.sessionState,viewport:page.viewport,controls:page.controls.length,visibleControls:page.controls.filter(c=>c.inViewport).length,unlabelledControls:page.controls.filter(c=>!c.label).length,networkStatuses:page.networkStatusEvidence??[],screenshot:page.screenshot??null,capturedAt:page.capturedAt,scope:'page shell or named panel only; full interaction coverage pending'});
  }
  const observed=pages.flatMap(page=>page.networkStatuses).filter(item=>item.method==='GET'&&item.status>=200&&item.status<300);
  const apiLedger=contracts.map(contract=>{
    const matched=matchReadEvidence(contract,observed);
    return {id:'API-'+crypto.createHash('sha256').update(contract.key).digest('hex').slice(0,10),...contract,readEvidence:matched,writeEvidence:[],status:matched.length?'read-status-observed-schema-pending':'source-discovered',appMapping:'pending'};
  });
  const feedback=JSON.parse(await fs.readFile(path.join(sourceRoot,'feedback/summary.json'),'utf8'));
  const threads=JSON.parse(await fs.readFile(path.join(sourceRoot,'feedback/thread-index.json'),'utf8'));
  const topicCounts={};
  for(const thread of threads) {
    if(!thread.file)continue;
    const content=JSON.parse(await fs.readFile(path.join(sourceRoot,thread.file),'utf8'));
    for(const topic of content.candidate.topics)topicCounts[topic]=(topicCounts[topic]??0)+1;
  }
  const summary={generatedAt:new Date().toISOString(),source:sourceSummary,feedback:{feedbackPosts:feedback.feeds.feedback.expectedTotal,discussionPosts:feedback.feeds.discussion.expectedTotal,selectedThreads:feedback.selectedThreads,completeThreads:feedback.completeThreads,commentCount:feedback.commentCount,topicCounts,humanReview:'in-progress, not completed by keyword classification'},browser:{pageOrPanelCaptures:pages.length,authenticated:pages.filter(p=>p.sessionState==='authenticated-observed').length,anonymous:pages.filter(p=>p.sessionState==='anonymous-observed').length,observedGetContracts:apiLedger.filter(a=>a.readEvidence.length).length,normalNonAdminAccount:'not yet available/verified'},releaseReady:false};
  await fs.writeFile(path.join(sourceRoot,'site/api-ledger.json'),JSON.stringify(apiLedger,null,2));
  await fs.writeFile(path.join(browserRoot,'page-index.json'),JSON.stringify(pages,null,2));
  await fs.writeFile(path.join(sourceRoot,'baseline-summary.json'),JSON.stringify(summary,null,2));
  console.log(JSON.stringify(summary));
})();
