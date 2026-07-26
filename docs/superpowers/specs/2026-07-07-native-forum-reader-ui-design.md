# NovalPie Native Forum Reader UI Design

## Goal

Rework NovalPie 2.0 from a functional Compose test shell into a native Android
novel-forum client with an integrated reader. The product should feel closer to
a forum app such as FluxDO, with a content-library structure inspired by comic
clients such as PicaComic, and a quiet reading surface inspired by Legado's
reader structure only.

## Non-goals

- Do not add Legado-style source editing, crawler rules, scraping, or complex
  download workflows.
- Do not make WebView the main product surface again.
- Do not expose API/debug implementation details as primary user-facing UI.
- Do not invent features that the NovalPie website does not already own unless
  they are local presentation helpers such as recent reading or reader theme.

## Product Structure

The bottom navigation becomes:

- Forum: default entry. Native community feed structure for latest discussion,
  book reviews, pinned or active topics, and forum fallback for unported
  community routes.
- Library: bookshelf, favorites, groups, recent reading, continue reading.
- Discover: search and ranking/filter surfaces.
- Profile: account, login sync, reader preferences, proxy diagnostics, and
  WebView fallback entry points.

## Visual Direction

- Material 3 foundation, but with forum-client density rather than dashboard
  cards.
- Warm neutral background, restrained rose/coral accent, dark-mode compatible.
- Use compact surfaces, 8dp-or-less corner radius for repeated cards, subtle
  dividers, and stable cover slots.
- Prefer rows, rails, grouped lists, and chapter sheets over oversized hero
  cards.
- Chinese UI copy must be clean UTF-8 output; no mojibake strings.
- Primary screens should show user-owned content and community activity first,
  not explanatory text.

## Forum Screen

The Forum tab is the default screen and carries the native Compose launch
marker `NOVALPIE_NATIVE_COMPOSE_HOME`.

It contains:

- compact top identity row: app name, current account state, sync action;
- latest discussion feed: topic title, author/avatar placeholder, reply count,
  update time, tag chips, related book title when available;
- book-review or active-book modules when forum API data is not yet available;
- Web forum fallback as a secondary action, not as the main screen.

## Library Screen

The Library tab contains:

- continue reading card with book/chapter and progress action;
- recent reading horizontal rail;
- favorite group chips;
- bookshelf search/filter field;
- compact book rows or grid-list with cover, title, author, status, word count,
  update date, tags, and load-more.

The screen should look like a content library, not an API status page.

## Discover Screen

Discover keeps the existing native search functionality but presents it as a
discovery surface:

- search input at top;
- history chips;
- compact filter controls grouped behind readable segmented rows;
- result count and sort state;
- book results using the same compact book component as Library.

## Book Detail

Book detail aggregates the website-owned book attributes:

- cover, title, author, status, word count, update time, tags;
- favorite state;
- same-book continue-reading action;
- description;
- chapter catalog preview with filter and current-progress marker;
- comments/discussion entry as native section or Web fallback until migrated.

## Reader

Reader is immersive and quiet:

- content appears before catalog controls;
- no `source:` or API/debug labels in the reading body;
- chapter title, position, previous/next controls, font size, and theme controls
  are available in reader chrome;
- catalog uses a sheet/drawer-like structure in later iterations;
- themes: system, sepia, dark.

## Verification Gates

- Unit tests prove navigation labels and reader presentation rules.
- Structural verifier checks that the first-stage UI now exposes Forum,
  Library, Discover, and Profile native labels while preserving fallback,
  proxy, search, detail, catalog, and reader contracts.
- MuMu verifier must still prove default launch enters Compose by finding
  `NOVALPIE_NATIVE_COMPOSE_HOME`.
- Screenshots must no longer resemble the old API test panel: no giant
  explanatory hero on default launch, no mojibake, no reader `source:` line.
