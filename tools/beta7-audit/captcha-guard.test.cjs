'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync(require('node:path').resolve(__dirname,
  '../../app/src/main/java/com/novalpie/nativeapp/ui/AuthCaptchaScreen.kt'), 'utf8');
const guard = source.match(/const val CAPTCHA_ANONYMOUS_SOURCE_GUARD = """([\s\S]*?)"""/)[1];
const recovery = source.match(/const val CAPTCHA_ANONYMOUS_LOGIN_RECOVERY = """([\s\S]*?)"""/)[1];

test('captcha hides auth only in its document, never alters the shared jar or stored login', () => {
  const cookies = 'auth_token=synthetic-login; cf_clearance=synthetic-clearance; theme=dark';
  let currentCookies = cookies;
  const values = new Map([['auth_token', 'synthetic-login'], ['theme', 'dark']]);
  class Storage {
    getItem(key) { return values.get(key) ?? null; }
    setItem(key, value) { values.set(key, value); }
    removeItem(key) { values.delete(key); }
  }
  class Document {}
  Object.defineProperty(Document.prototype, 'cookie', {get() {return currentCookies;}, set(value) {currentCookies = value;}});
  const document = new Document();
  const context = {Document, document, localStorage: new Storage(), sessionStorage: new Storage(), location: {replace() {}}};
  context.window = context;
  vm.runInNewContext(guard, context);
  assert.equal(context.localStorage.getItem('auth_token'), null);
  assert.equal(context.localStorage.getItem('theme'), 'dark');
  assert.equal(document.cookie.includes('auth_token='), false);
  assert.equal(document.cookie.includes('cf_clearance='), true);
  context.localStorage.setItem('auth_token', 'new-token');
  context.localStorage.removeItem('auth_token');
  document.cookie = 'auth_token=; path=/; max-age=0';
  vm.runInNewContext(recovery, context);
  assert.equal(values.get('auth_token'), 'synthetic-login');
  assert.equal(currentCookies, cookies);
  assert.equal(Object.getOwnPropertyDescriptor(Document.prototype, 'cookie').get.call(new Document()), cookies);
});
