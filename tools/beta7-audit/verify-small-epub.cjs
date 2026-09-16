'use strict';
// Bounded local QA only. Does not export prose or read any authentication state.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {inflateRawSync, crc32} = require('node:zlib');
const input = path.resolve(process.argv[2]);
if (!input.startsWith('D:\\NovalPie\\agent-bridge\\artifacts\\') || !input.endsWith('.epub')) throw Error('Expected scoped test EPUB');
if (fs.statSync(input).size > 64 * 1024 * 1024) throw Error('Use streaming large-book validator');
const data = fs.readFileSync(input), files = new Map();
function assert(value, message) { if (!value) throw Error(message); }
let end = data.length - 22;
while (end >= Math.max(0, data.length - 65557) && data.readUInt32LE(end) !== 0x06054b50) end--;
assert(end >= 0, 'Missing ZIP directory');
const count = data.readUInt16LE(end + 10); let cursor = data.readUInt32LE(end + 16), inflated = 0;
assert(count !== 65535 && cursor !== 0xffffffff, 'Use ZIP64 streaming validator');
for (let index = 0; index < count; index++) {
  assert(data.readUInt32LE(cursor) === 0x02014b50, 'Bad central entry');
  const method = data.readUInt16LE(cursor + 10), crc = data.readUInt32LE(cursor + 16), size = data.readUInt32LE(cursor + 20);
  const rawSize = data.readUInt32LE(cursor + 24), nameLength = data.readUInt16LE(cursor + 28);
  const extra = data.readUInt16LE(cursor + 30), comment = data.readUInt16LE(cursor + 32), offset = data.readUInt32LE(cursor + 42);
  const name = data.subarray(cursor + 46, cursor + 46 + nameLength).toString('utf8');
  assert(!files.has(name) && !name.includes('..'), 'Duplicate or unsafe entry');
  assert(method === 0 || method === 8, 'Unsupported compression');
  assert(data.readUInt32LE(offset) === 0x04034b50, 'Bad local entry');
  const start = offset + 30 + data.readUInt16LE(offset + 26) + data.readUInt16LE(offset + 28);
  assert(start + size <= data.length, 'Truncated ZIP body');
  const packed = data.subarray(start, start + size);
  const body = method === 0 ? packed : inflateRawSync(packed, {maxOutputLength: 64 * 1024 * 1024});
  inflated += body.length; assert(inflated <= 128 * 1024 * 1024, 'Expanded budget exceeded');
  assert(body.length === rawSize && crc32(body) === crc, `CRC/size mismatch: ${name}`);
  files.set(name, body); cursor += 46 + nameLength + extra + comment;
}
assert(files.get('mimetype')?.toString() === 'application/epub+zip', 'Bad EPUB MIME');
const chapters = [...files.keys()].filter(name => /^OEBPS\/chapter-\d+\.xhtml$/.test(name));
let references = 0;
for (const name of chapters) {
  for (const match of files.get(name).toString().matchAll(/<img\b[^>]*\bsrc=["']([^"']+)["']/g)) {
    assert(files.has(path.posix.normalize(path.posix.join(path.posix.dirname(name), match[1]))), 'Missing image resource'); references++;
  }
}
const images = [...files].filter(([name]) => name.startsWith('OEBPS/images/')).map(([name, body]) => {
  const magic = body.subarray(0, 12).toString('hex');
  assert(magic.startsWith('ffd8ff') || magic.startsWith('89504e47') || magic.startsWith('47494638') || (magic.startsWith('52494646') && body.toString('ascii',8,12)==='WEBP'), 'Not an original image format');
  return {name, bytes: body.length, sha256: crypto.createHash('sha256').update(body).digest('hex')};
});
const report = {file: input, bytes: data.length, sha256: crypto.createHash('sha256').update(data).digest('hex'), entries: count,
  chapterCount: chapters.length, bodyImageReferences: references, images, crcAndSizePassed: true, missingResources: [],
  sourceByteComparisonPerformed: false};
if (process.argv[3]) {
  const output = path.resolve(process.argv[3]);
  assert(output.startsWith('D:\\NovalPie\\agent-bridge\\artifacts\\') && output.endsWith('.json'), 'Expected scoped report');
  fs.writeFileSync(output, JSON.stringify(report, null, 2));
}
console.log(JSON.stringify(report));
