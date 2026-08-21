# Search Cover Delivery Contract

Last verified: 2026-08-16 (Collection/profile grid and long-press pass)

This document records the native Search cover behaviour that is required to match the
source site's dense, image-led mobile search results without trading away cover clarity.

## Rendering contract

- Grid and list cards request `novelThumbnailCoverUrl(book)` at 512 x 768 decode size.
  The original URL remains reserved for the stationary long-press zoom preview.
- The result grid is responsive: 2 columns on phones, then 4 / 5 / 6 columns at 720 / 900 /
  1000 dp. This mirrors the source's 2 / 4 / 5 / 6 progression while retaining a readable
  two-column portrait phone layout.
- A just-returned result page warms two responsive grid rows with
  `NovelCoverLoadPriority.Visible`, capped at eight valid covers. Those requests share the normal
  visible-card dispatcher, so they cannot be queued behind scroll look-ahead work. This protects
  the first fast scroll without reducing source cover quality.
- Scroll look-ahead is explicitly speculative: it follows the current responsive column count,
  is capped at eight URLs, and uses a two-request background dispatcher. It must never starve
  visible cards.
- A blank or host-only source `photo_url` is a source-missing cover, not a pending download.
  Cards render a branded low-contrast gradient, book icon, and clear `暂无封面` state rather than
  an ambiguous gray initial. A valid request that fails later uses the same state.
- Search cards keep the full normalized tag list in the wrapping rail. They do not truncate the
  visible source tags to four or replace the remainder with a `+N` badge.
- Search cover preview is a stationary long-press-only action. A normal tap remains the card's
  detail-navigation gesture, and movement past touch slop cancels preview.

## Runtime evidence

On MuMu (`127.0.0.1:16384`) with the existing `tcp:7890` reverse proxy, a live `c` search
returned 358 source results. The response was still in its skeleton state at 0.35 seconds;
by 1.35 seconds the valid first-row source cover was visible. The adjacent item had no source
cover URL and correctly displayed `暂无封面` rather than looking permanently loading.

- [0.35 s skeleton](../qa-screenshots/turn96-search-cover-priority/search-c-priority-0.35s.png)
- [1.35 s live results](../qa-screenshots/turn96-search-cover-priority/search-c-priority-1.35s.png)
- [3.0 s settled results](../qa-screenshots/turn96-search-cover-priority/search-c-priority-3.0s.png)
- [0.2 s after a fast result scroll](../qa-screenshots/turn96-search-cover-priority/search-c-scroll-0.2s.png)
- [1.2 s after a fast result scroll](../qa-screenshots/turn96-search-cover-priority/search-c-scroll-1.2s.png)
- [polished missing-cover card](../qa-screenshots/turn97-ui-parity/search-c-missing-cover-polished.png)

Debug APK SHA-256:

`55C21DCB4DBBA802099011A718B38099FAC52269E4B00B67A26387F1E01BB2F4`

## Regression commands

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.novalpie.nativeapp.data.NovalPieImageLoadingTest" --tests "com.novalpie.nativeapp.ui.SearchCoverPrefetchTest"
.\gradlew.bat testReleaseUnitTest
```

Turn 96 results: targeted Debug tests passed; full Release unit tests passed with zero failures.

## Turn 110 - first-scroll warm-cache regression

- A read-only source image response confirmed that the cover service delivers original assets (the
  checked file was 341,830 bytes) with `Cache-Control: max-age=2592000`. No lower-quality source
  thumbnail endpoint was substituted.
- The initial search warm-up now includes two two-card grid rows (up to four valid cover URLs),
  while the post-scroll batch remains capped at four URLs on the two-request speculative
  dispatcher. Display and warm-up retain the same 512 x 768 Coil cache key.
- A fresh live `b` search on MuMu showed the normal request skeleton at approximately 0.35 s and
  sharp first-row original covers at approximately 1.50 s. A fast scroll immediately showed the
  already-warmed next row with complete source tags and facts; no blank-card or accidental-route
  regression occurred.
- The compact native search field now receives its actual focus state, so its visual focus border
  follows text focus instead of looking inactive while the IME is active.

Evidence:

- [request skeleton](../qa-screenshots/turn110-search-reader-regression/search-b-correct-0.35s.png)
- [sharp first row](../qa-screenshots/turn110-search-reader-regression/search-b-correct-1.50s.png)
- [fast-scroll next rows](../qa-screenshots/turn110-search-reader-regression/search-b-correct-scroll-0.25s.png)

Verification: `SearchCoverPrefetchTest`, `NovalPieImageLoadingTest`, and
`ReaderPresentationTest` passed (23 tests); full Release tests passed 69 suites / 378 tests /
0 failures / 0 errors / 0 skipped. `lintDebug --no-daemon` reported no errors or warnings.
Debug APK SHA-256: `100715728BAFFF6120CE145EC1F56AA8F90D5FA66347E11C232F5B896110D6E3`.

## Turn 154 - responsive grid and aligned-card regression

- Replaced the historical hand-chunked two-card search rows with actual book-id keyed
  `LazyVerticalGrid` cells. Full-width search, filter, pagination, and list-mode items retain
  their grid spans.
- Search cards reserve fixed title, author, tag, and metric slots. A tag-rich record can no
  longer leave its neighbour with a shorter background frame; the full tag set remains visible
  in the wrapping rail and detail.
- MuMu landscape (1600 x 900 px, 1066 dp wide) visibly rendered six sharp source-cover cards in
  one aligned row. Portrait returned to the normal two-card grid. The existing 512 x 768 decode
  and full-resolution cover-preview path were unchanged.
- Regression evidence:
  `../qa-artifacts/turn154-responsive-search-reader-badge/search-landscape-grid-6-columns.png`
  and `../qa-artifacts/turn154-responsive-search-reader-badge/search-portrait-2-columns.png`.

## Browser-session rule

For authenticated source-page comparison, follow
[`BROWSER_SESSION_POLICY.md`](../BROWSER_SESSION_POLICY.md): use only the user-owned, already
logged-in Edge window when it is directly attachable. Do not create a replacement session or
export, print, save, clear, or copy its cookies, tokens, passwords, profile, or storage.
