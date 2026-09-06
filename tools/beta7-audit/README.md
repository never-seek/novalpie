# Beta 7 baseline capture

Read-only, anonymous source/feedback inventory. This is NOT a runtime UI verifier and it never claims a reported bug is fixed.

```powershell
# Reuse the existing project dependency runtime on this workstation; do not install global tools.
$env:NODE_PATH='D:\NovalPie\minimal-commercial\node_modules'
node --test tools/beta7-audit/audit.test.cjs
node tools/beta7-audit/capture.cjs --stage all --out D:\NovalPie\agent-bridge\artifacts\beta7-baseline\20260905-public --proxy http://127.0.0.1:7890
```

For another workstation, use `npm install --ignore-scripts` inside this directory. Dependencies are pinned in package.json; no app dependency is changed.

- URL allowlist limits GETs to the source homepage/bundles and public forum endpoints. No auth, account or message APIs.
- Assets are copied as publicly served, never evaluated. TypeScript's JS AST recovers route definitions, source control/handler candidates and API call candidates.
- Endpoint candidates may contain unresolved expressions. Human review is mandatory before implementation; they are not runtime contracts.
- Captures exhaust server-advertised feed/comment pages, retain nested reply provenance, detect missing counts/pages, and record failures. A capture with gaps is never complete.
- Feedback candidates are keyword-indexed, not automatically adjudicated. Read the whole thread, including replies correcting the original report.
- Outputs stay outside the source repo; public excerpt fields are allowlisted and common credential/email forms are redacted. Browser storage and private messages are never read.
- No images/EPUBs are downloaded by this script. Evidence records are bounded at 10MB/response, 160MB/capture and 512 source assets; hitting a bound leaves an explicit gap.

## Device gates

`run-device-gate.ps1` requires the installed app hash to match the local Debug APK, records SDK/size/font/APK+test hashes, and parses the JUnit result (ADB exit 0 alone is not success).

Live TTS takes explicit authorized IDs; it reads real chapters and updates that book's reading position. Do not run it against unspecified books. The exported report contains IDs/counts/status only, not prose or credentials.

```powershell
.\tools\beta7-audit\run-device-gate.ps1 -TestClass com.novalpie.nativeapp.ui.NativeTtsLiveDeviceTest -EvidenceName live-tts-example -BookId 353686 -ChapterId 6072567
```

Synthetic message UI tests use a fake repository and cannot send real DMs. Do not collect screenshots or raw UI dumps from a real private conversation as test evidence.
