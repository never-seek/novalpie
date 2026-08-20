# Reader Reference Guide

## Purpose

This document records the reader-product references selected for NovalPie 2.0. They are
interaction and presentation references, not code or feature-import targets. NovalPie remains a
native Android client for the live NovalPie website and its observed APIs.

## Primary references

| Reference | Reuse in NovalPie | Do not import |
| --- | --- | --- |
| `alanliu185/Flutter_Novel_Reader` | Bookcase grid/list switching, immersive reader chrome, compact catalog/settings surfaces, readable typography controls, local progress and cache presentation. | Local-book import, arbitrary book sources, source switching, advertising, membership/payments, cloud backup. |
| `IReaderorg/IReader` | State separation between library, reader preferences, reading progress, display theme, typography and offline availability; responsive Android/desktop information hierarchy. | Extensions, JavaScript plugins, Legado import, multi-source search, source repositories, downloaded-site content. |

## Secondary visual references

- `Pacalini/PicaComic`: dense yet readable cover grids, stable card keys, image-first browsing.
- `Lingyan000/fluxdo`: forum-card hierarchy, compact action rows and semantic tag pills.
- `gedoor/legado`: immersive tap-to-toggle reader shell and catalog placement only. Its text editing,
  rule creation, crawling and download systems are outside NovalPie scope.

## Product rules

1. Source parity wins over generic reader features. A native surface must expose data and actions
   available on the live NovalPie page or observed API before adding a convenience control.
2. Book cards retain the original cover URL, author, source/status markers, all content tags,
   favourite count, site reads and word count. A visually simplified card must not discard these
   facts.
3. The reader stays immersive: text is the primary canvas; chrome appears on intentional tap,
   catalog and settings are compact overlays, and Android Back returns through the native route
   stack without reopening stale pages.
4. Cover and chapter illustrations open the original asset in a zoomable preview. Thumbnails are
   not a substitute for original-resolution viewing.
5. A normal centre tap reveals the reader's source-style top/bottom chrome. The radial tool panel
   is optional, never the default gesture, so Catalog and Settings do not require a double tap.
   A fast repeat at the same body point is coalesced to keep chrome open. Once chrome is visible,
   any readable-body tap resolves against chrome before a configured left/right page zone: it
   dismisses chrome unless it is the guarded repeat. This keeps continuous mode from treating a
   right-side body tap as a no-op and leaving the dock stuck on screen. A drag never toggles chrome.
6. A missing Android speech engine is actionable: the reader first shows preparation feedback,
   then exposes a `系统设置` action that opens the platform TTS settings surface. If a device does
   not expose that route, the action safely falls back to the app's TTS settings category.
7. Local reading progress, display preferences and UI cache policy may persist on device. NovalPie
   API content is never turned into a generic crawler, source plugin or cross-site download tool.
8. Administrator routes remain gated by `role == "admin"`. No privileged page is inferred from a
   locally saved session or an arbitrary route.
9. Real source mutations (comments, uploads, administrator actions, payments and account changes)
   remain behind the explicit confirmation layer.

## Next reader UX priorities

1. Verify the search IME submission and route-stack behavior on MuMu after every reader/navigation
   change.
2. Audit source reader controls one page at a time: typography, spacing, theme, catalog, progress
   and image preview.
3. Keep the Collection grid/list layouts responsive at phone and tablet widths while preserving
   every source fact visible on the corresponding mobile card.
4. Prefer motion that confirms navigation or toolbar visibility; do not add ornamental animations
   that interfere with reading or scrolling.
