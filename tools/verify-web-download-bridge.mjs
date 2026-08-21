import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import vm from "node:vm";

const projectRoot = resolve(import.meta.dirname, "..");
const bridgeSource = readFileSync(
  resolve(projectRoot, "app/src/main/java/com/novalpie/nativeapp/ui/WebDownloadBridge.kt"),
  "utf8",
);
const scriptMarker = 'internal const val BLOB_DOWNLOAD_SCRIPT = """';
const scriptStart = bridgeSource.indexOf(scriptMarker);
assert.notEqual(scriptStart, -1, "Cannot find BLOB_DOWNLOAD_SCRIPT in WebDownloadBridge.kt");
const scriptBodyStart = bridgeSource.indexOf("\n", scriptStart) + 1;
const scriptBodyEnd = bridgeSource.indexOf("\n\"\"\"", scriptBodyStart);
assert.notEqual(scriptBodyEnd, -1, "Cannot find BLOB_DOWNLOAD_SCRIPT terminator");
const bridgeScript = bridgeSource.slice(scriptBodyStart, scriptBodyEnd);

function makeBridgeHarness({ rawUploadSucceeds }) {
  const blobUrls = new Map();
  const rawUploads = [];
  const chunkSessions = new Map();
  let nextBlobUrl = 1;
  let nextChunkSession = 1;

  class FakeAnchor {
    constructor(href, filename) {
      this.href = href;
      this.tagName = "A";
      this.parentNode = null;
      this.attrs = { href, download: filename };
    }

    getAttribute(name) {
      return this.attrs[name] ?? null;
    }

    click() {
      throw new Error("The bridge should intercept Blob anchor clicks before the native browser path");
    }
  }

  class FakeFileReader {
    readAsDataURL(blob) {
      blob.arrayBuffer().then(
        (buffer) => {
          this.result = `data:application/octet-stream;base64,${Buffer.from(buffer).toString("base64")}`;
          this.onload?.();
        },
        (error) => {
          this.error = error;
          this.onerror?.();
        },
      );
    }
  }

  const document = {
    title: "NovalPie Blob Regression",
    addEventListener() {},
  };
  const urlApi = {
    createObjectURL(blob) {
      const href = `blob:novalpie-regression-${nextBlobUrl++}`;
      blobUrls.set(href, blob);
      return href;
    },
    revokeObjectURL() {},
  };
  const androidDownload = {
    getBlobUploadUrl(_filename, _mime, key) {
      return `http://127.0.0.1:17777/novalpie-blob-upload?key=${encodeURIComponent(key)}`;
    },
    startBlobDownload(_filename, _mime, _totalBytes, key) {
      const id = `chunk-${nextChunkSession++}`;
      chunkSessions.set(id, { key, chunks: [], finished: false });
      return id;
    },
    appendBlobDownloadChunk(id, base64Chunk) {
      const session = chunkSessions.get(id);
      if (!session) return false;
      session.chunks.push(Buffer.from(base64Chunk, "base64"));
      return true;
    },
    finishBlobDownload(id) {
      const session = chunkSessions.get(id);
      if (!session) return false;
      session.finished = true;
      return true;
    },
    failBlobDownload(id) {
      chunkSessions.delete(id);
    },
  };
  const context = vm.createContext({
    AndroidDownload: androidDownload,
    Blob,
    Buffer,
    FileReader: FakeFileReader,
    HTMLAnchorElement: FakeAnchor,
    Map,
    Promise,
    Set,
    URL: urlApi,
    WeakMap,
    console,
    document,
    fetch: async (url, options = {}) => {
      const text = String(url);
      if (text.startsWith("blob:")) {
        const blob = blobUrls.get(text);
        assert.ok(blob, `Unknown simulated Blob URL: ${text}`);
        return { blob: async () => blob };
      }
      if (text.startsWith("http://127.0.0.1:17777/")) {
        rawUploads.push({ url: text, body: options.body });
        return { ok: rawUploadSucceeds };
      }
      throw new Error(`Unexpected simulated fetch target: ${text}`);
    },
    setTimeout() { return 0; },
    window: null,
  });
  context.window = context;
  vm.runInContext(bridgeScript, context, { filename: "BLOB_DOWNLOAD_SCRIPT.js" });

  return {
    createAnchor(blob, filename) {
      return new context.HTMLAnchorElement(context.URL.createObjectURL(blob), filename);
    },
    createBlobUrl(blob) {
      return context.URL.createObjectURL(blob);
    },
    capture(url, filename, mime) {
      return context.window.__NOVALPIE_CAPTURE_BLOB_DOWNLOAD__(url, filename, mime);
    },
    rawUploads,
    chunkSessions,
  };
}

async function verifyRawBlobDeduplication() {
  const harness = makeBridgeHarness({ rawUploadSucceeds: true });
  const epub = new Blob(["PK\u0003\u0004novalpie-regression"], { type: "application/epub+zip" });
  const firstAnchor = harness.createAnchor(epub, "novel.epub");
  const secondUrl = harness.createBlobUrl(epub);
  const secondAnchor = new firstAnchor.constructor(secondUrl, "novel.epub");

  await Promise.all([
    firstAnchor.click(),
    harness.capture(firstAnchor.href, "novel.epub", "application/epub+zip"),
    secondAnchor.click(),
  ]);

  assert.equal(harness.rawUploads.length, 1, "One Blob identity must create one raw upload");
  assert.equal(harness.rawUploads[0].body, epub, "The bridge must upload the original Blob object");
  assert.equal(harness.chunkSessions.size, 0, "A successful raw upload must not enter the Base64 fallback");

  await harness.capture(secondUrl, "novel.epub", "application/epub+zip");
  assert.equal(harness.rawUploads.length, 1, "Completed Blob URLs must remain idempotent");

  // A page can recreate an invisible anchor after a completed download. It gets a new blob URL
  // for the same Blob object, so this verifies that completion is keyed by Blob identity rather
  // than only by the first URL.
  const recreatedUrl = harness.createBlobUrl(epub);
  await harness.capture(recreatedUrl, "novel.epub", "application/epub+zip");
  assert.equal(harness.rawUploads.length, 1, "A post-completion Blob URL must not upload again");
}

async function verifyChunkFallbackDeduplication() {
  const harness = makeBridgeHarness({ rawUploadSucceeds: false });
  const epub = new Blob(["PK\u0003\u0004fallback-regression"], { type: "application/epub+zip" });
  const firstAnchor = harness.createAnchor(epub, "fallback.epub");
  const secondUrl = harness.createBlobUrl(epub);
  const secondAnchor = new firstAnchor.constructor(secondUrl, "fallback.epub");

  await Promise.all([
    firstAnchor.click(),
    harness.capture(firstAnchor.href, "fallback.epub", "application/epub+zip"),
    secondAnchor.click(),
  ]);

  assert.equal(harness.rawUploads.length, 1, "Fallback must reuse the one raw-upload attempt");
  assert.equal(harness.chunkSessions.size, 1, "One Blob identity must create one chunk fallback session");
  const session = [...harness.chunkSessions.values()][0];
  assert.equal(session.finished, true, "Fallback session must finish exactly once");
  assert.deepEqual(Buffer.concat(session.chunks), Buffer.from(await epub.arrayBuffer()));
}

await verifyRawBlobDeduplication();
await verifyChunkFallbackDeduplication();

console.log(JSON.stringify({
  status: "pass",
  checks: [
    "same Blob across multiple blob: URLs uploads once",
    "a recreated Blob URL remains complete after the first transfer",
    "DownloadListener capture shares the page click transfer",
    "raw upload fallback creates one chunk session",
    "fallback bytes equal the original Blob bytes",
  ],
}, null, 2));
