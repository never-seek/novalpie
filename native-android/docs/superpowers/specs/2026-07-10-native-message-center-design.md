# Native Message Center Design

Date: 2026-07-10

## Goal

Replace the remaining `/messages` WebView handoff with a complete native Android message experience that preserves every currently observed website capability while using an app-owned, premium mobile layout. This feature is available to any authenticated user and does not grant or expose administrator capabilities.

## Live website evidence

The actively served `novalpie.cc` assets were re-inspected on 2026-07-10:

- main runtime: `/_nuxt/CxFG0gqQ.js`
- message page: `/_nuxt/Bjv5odFQ.js`
- direct-message modal: `/_nuxt/DSvo202g.js`

Observed endpoints and payloads:

- `GET /api/messages` with `page`, `page_size`, optional `message_type`, `is_read`, `priority`, and `keyword`.
- `GET /api/messages/{id}`.
- `POST /api/messages/{id}/read` with `{ "id": id }`.
- `POST /api/messages/read` with `{ "ids": [...] }` or `{ "all": true }`.
- `DELETE /api/messages/{id}` with `{ "id": id, "permanent": false }`.
- `DELETE /api/messages` with `{ "ids": [...] }`.
- `POST /api/messages/{id}/star` with `{ "starred": 1|0 }`.
- `GET /api/messages/stats`.
- `GET /api/messages/settings`.
- `PUT /api/messages/settings`.
- `GET /api/messages/conversations` with `target_user_id`, `page`, and `page_size`.
- `POST /api/messages` for direct messages with `user_id`, `execute_user_id`, `message_type: 8`, `message_title`, and `message_content`.

Observed message settings:

- `enable_notifications`
- `enable_email`
- `enable_browser_push`
- `notification_types`
- `quiet_hours_start`
- `quiet_hours_end`
- `auto_read_after_days`

## Product experience

### Message inbox

The native inbox uses the existing NovalPie light blue/pink product palette but is not a visual copy of the website. It is optimized for narrow Android screens:

- compact title row with total/unread summary and refresh;
- horizontally scrollable stat pills;
- floating search field;
- filter rails for message type, read state, and priority;
- high-contrast cards with unread accent, sender, type, date, excerpt, priority, and star state;
- tap opens detail; long press or checkbox enters selection mode;
- infinite pagination with a visible retry row;
- selection toolbar supports mark-read and delete;
- top actions provide mark-all-read and settings.

The interface must avoid browser/debug language, nested website navigation, and desktop-width tables.

### Message detail

Detail is a native full-screen route on phones rather than a cramped modal. It displays:

- title/type;
- sender/avatar metadata when available;
- creation and read timestamps;
- structured content blocks for special notification data;
- plain or HTML-derived message body;
- mark-read, star/unstar, action-link, and delete actions.

Delete always requires a destructive confirmation. The list and stats update only after the server confirms success.

### Direct-message conversation

Opening a type-8 message resolves the other participant from `execute_user_id` and `user_id`. It opens a native conversation route with:

- chronological message bubbles;
- current-user/right and other-user/left alignment;
- relative timestamps;
- pull/manual refresh;
- composer and send button;
- optimistic sending bubble with sending/failed state and retry.

Conversation requests use `target_user_id`, page `1`, and page size `100`, matching the website. Sending uses message type `8` and the website-standard title format.

### Message settings

Settings use a native bottom sheet/dialog containing:

- master notifications switch;
- email switch;
- browser-push switch;
- message-type selection;
- quiet-hours start/end fields;
- automatic-read day count.

The form loads current server values, validates time/day fields locally, and closes only after a successful save.

## Architecture

### Models

Add focused models rather than overloading `SiteMessage`:

- `MessageQuery`
- `MessagePagination`
- `MessagePage`
- expanded `SiteMessage` metadata (`readAt`, `userId`, `executeUserId`, avatar fields, `extraData`)
- `MessageSettings`
- `DirectMessage`
- `MessageActionResult`

### API

`NovalPieApi` exposes typed methods for list, detail, all mutations, settings, conversation, and send. Existing HTTP helpers remain responsible for auth/cookies/proxy behavior. JSON normalization accepts current snake_case plus known camelCase fallbacks.

### State and routing

Add routes:

- `AppRoute.Messages`
- `AppRoute.MessageDetail(messageId)`
- `AppRoute.MessageConversation(targetUserId, targetName)`

Add independent state holders:

- `MessageCenterState`
- `MessageDetailState`
- `MessageConversationState`

Each request family has its own serial guard so stale search/filter/page/detail responses cannot overwrite newer state. The Tools preview remains lightweight and opens the native inbox/detail routes.

### Compose boundaries

Create `MessageCenterScreen.kt` for the inbox, detail, conversation, settings, and their small components. `NovalPieApp.kt` only wires routes and callbacks. Pure copy/filter/formatting/selection helpers live in `MessagePresentation.kt` and are unit tested.

## Authentication and permissions

- The feature requires an authenticated token/session.
- Missing auth shows the existing native login prompt and website login handoff.
- Message APIs are user-scoped by the server.
- No `/admin` route or administrator card is added here.
- Existing `role == "admin"` gating for administrator tools remains unchanged.

## Failure and safety behavior

- List/detail/conversation failures show inline retry, never a permanent spinner.
- Mutation buttons are disabled while the matching operation is active.
- Mark-read/star updates occur after server success.
- Delete and bulk delete require explicit confirmation.
- Runtime QA against the real account is read-only: list, filters, pagination, detail rendering, settings load, and conversation load only.
- Mutation request bodies are verified with MockWebServer; no real message is deleted or marked read during automated QA.

## Test strategy

### API tests

MockWebServer verifies exact path, method, query parameters, and JSON body for every endpoint. JSON fixtures verify list pagination, detail metadata, settings, and conversation normalization.

### Presentation/state tests

Pure tests cover filter labels, type labels, participant resolution, selection behavior, pagination merging, stale request rejection, settings validation, and local mutation reducers.

### Navigation tests

Route-stack tests cover Tools -> Messages -> Detail/Conversation -> Back and ensure another selected message cannot reopen stale content.

### Runtime QA

MuMu QA uses UI-tree-derived coordinates and captures inbox, filters, detail, conversation, and settings screenshots. Logcat is checked for crash, timeout, DNS, TLS, and OOM signatures. Mutating controls are not activated on the real account.

## Acceptance criteria

- The Tools message-center button and recent-message cards open native routes.
- All website filters and statistics are present.
- Pagination works without duplicate or stale rows.
- Detail, read, star, action, delete, bulk read/delete, and all-read behaviors are implemented and test-covered.
- Type-8 messages open a working native conversation and can send through the observed API.
- All observed message settings load and save.
- Anonymous users are prompted to log in.
- Administrator-only UI remains hidden from non-admin users and unchanged by this feature.
- Full unit tests, debug/release builds, and read-only MuMu QA pass.
