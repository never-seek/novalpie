'use strict';
// CLI cannot attach an Android WebView; Playwright's Android API attaches the existing app only.
const {_android} = require('C:/Users/86188/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async () => {
  const device = (await _android.devices())[0];
  if (!device) throw new Error('No connected QA device');
  const page = await (await device.webView({pkg: 'com.novalpie.app.debug'})).page();
  const snapshot = await page.evaluate(() => ({path: location.pathname, viewport: {w: innerWidth, h: innerHeight},
    ready: document.readyState, hasTurnstile: typeof window.turnstile?.render === 'function',
    scripts: [...document.scripts].map(x=>{try {const u=new URL(x.src);return u.origin+u.pathname;} catch{return '';}}).filter(Boolean),
    buttons: [...document.querySelectorAll('button')].map(x=>({text:x.textContent.trim(),top:x.getBoundingClientRect().top})),
    frameCount: document.querySelectorAll('iframe').length,
    shadowHosts: [...document.querySelectorAll('*')].filter(x=>x.shadowRoot).map(x=>({tag:x.tagName,id:x.id})),
  }));
  console.log(JSON.stringify({...snapshot, frames: page.frames().map(f=>{try{const u=new URL(f.url());return u.origin+u.pathname;}catch{return '(empty)';}})}));
  const target = page.getByRole('button', {name: 'Turnstile', exact: true});
  if (await target.count()) await target.scrollIntoViewIfNeeded();
  await device.close();
})();
