# Design

## Context

See `proposal.md` for motivation. Key constraints from the current codebase:
- **Client platforms**: Compose Multiplatform (`android`, `iosArm64`, `iosSimulatorArm64`, `wasmJs`) in `app/shared`, currently with no remote image-loading library. WasmJS runs in the browser, where CORS blocks direct fetches to arbitrary third-party websites and images.
- **Server & storage**: Ktor JVM server (`server/`) backed by a single SQLite database on a `/data` Docker volume, using JDK `java.net.http.HttpClient` for outbound calls (FCM, Resend).
- **Wishlist model**: Links are stored in `wishlist_item_links (id, item_id, position, url)` (up to 10 links per item, up to 200 items per wishlist). All three wishlist UIs (own list, Santa's receiver view, admin oversight sheet) share `ItemText` in `app/shared/src/ui/wishlist/WishlistScreen.kt`.

## Goals / Non-Goals

**Goals:**
- Unfurl link titles, site names, and resized thumbnails safely on the server and serve thumbnails from the server so Android, iOS, and WasmJS behave identically with zero CORS or viewer-IP leakage issues.
- Keep item add/edit/read endpoints fast and non-blocking via background fetching, on-demand queuing, and startup backfill of existing links.
- Maintain strict backward compatibility for existing clients reading `urls` and deprecated `url`.
- Keep the client lightweight without introducing a heavy multiplatform image library.

**Non-Goals:**
- Rich product scraping (prices, availability, ratings) or headless browser rendering for JS-only SPAs.
- Full-size image caching or tall social-media cards with multi-line descriptions.
- Real-time push/WebSocket delivery of preview updates (bounded short client refresh is sufficient).

## Decisions

### 1. SQLite cache table (`link_previews`) with resized BLOB thumbnails
Store preview metadata and resized thumbnail bytes in a new SQLite table (`V019__link_previews.sql`):
- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `url TEXT NOT NULL UNIQUE`
- `status TEXT NOT NULL` (`pending`, `ready`, `failed`)
- `title TEXT`
- `site_name TEXT`
- `image_bytes BLOB`
- `image_content_type TEXT`
- `fetched_at TEXT`
- `retry_after TEXT`

**Why**: The server already persists all state in SQLite on `/data`. Storing small resized thumbnails (capped at 256px max dimension, JPEG/PNG, typically 5–30 KB) as BLOBs avoids adding a second storage path, keeps Docker volume backups single-file, and lets orphan cleanup be a simple SQL `DELETE` where `url NOT IN (SELECT url FROM wishlist_item_links)`.
**Alternative considered**: Storing image files under `/data/previews/` on disk. Rejected because it splits state between SQLite and the filesystem and complicates cleanup/backups for negligible benefit at Secret Santa group scale.

### 2. SSRF-safe outbound fetcher & lightweight HTML/OG parsing
Use JDK `java.net.http.HttpClient` configured with `Redirect.NEVER` and a manual redirect loop (max 5 hops):
- On every hop (initial URL, redirects, and the extracted image URL), parse the URI (`http`/`https` only), resolve all `InetAddress.getAllByName(host)`, and reject any address that is loopback, site-local/private, link-local, multicast, or any-local (plus IPv4-mapped IPv6 equivalents and CGNAT `100.64.0.0/10`).
- Connect timeout 5s, request timeout 10s, custom polite `User-Agent`.
- Stream HTML response up to 512 KB (UTF-8 / declared charset) and parse `<meta>` (`property`/`name` for `og:title`, `twitter:title`, `og:site_name`, `og:image`, `twitter:image`) and `<title>` without needing a full DOM engine (or with a small focused HTML head parser that handles attribute ordering, quotes, and HTML entity decoding).
- Stream image response up to 5 MB, verify `Content-Type` (`image/jpeg`, `image/png`, `image/webp`, `image/gif`), decode with `ImageIO` (handling WebP gracefully if unsupported by JDK by falling back to title-only rather than failing the whole preview), resize preserving aspect ratio to max 256px, and encode to JPEG (or PNG if alpha is present).

**Why**: Manual redirect validation prevents redirect-based SSRF; streaming byte caps prevent memory exhaustion; keeping title-only when an image fails to decode maximizes useful previews.
**Alternative considered**: Adding `jsoup` for HTML parsing. Adding a small dependency like `jsoup` is also viable if edge-case HTML parsing warrants it, or a focused head-tag parser keeps `server/module.yaml` zero-dependency; either satisfies the spec.

### 3. Background worker with deduplication, on-demand queuing, and startup backfill
Introduce a `LinkPreviewService` in `server` with a bounded coroutine dispatcher/semaphore (e.g., max 4 concurrent outbound fetches) and an in-memory `inFlight` set keyed by URL:
- **On add/edit**: After persisting the item, enqueue any item URLs that have no `ready` entry (and are not within a `failed` cooldown). Insert/upsert a `pending` row so the immediate response can mark them `pending`.
- **On read**: Look up `link_previews` for the returned items' URLs in one batch query; enqueue any URL that is missing or whose `failed` `retry_after` has passed (marking it `pending` if missing).
- **On startup**: Query distinct URLs from `wishlist_item_links` that have no `link_previews` row and enqueue them in the background.
- **Failure cooldown**: Failed fetches set `status = 'failed'` and `retry_after = now + 6 hours`.
- **Orphan cleanup**: Run a lightweight cleanup on startup and periodically (e.g., daily or after item deletion/edit batches) deleting `link_previews` rows whose `url` is no longer in `wishlist_item_links`.

### 4. Backward-compatible API contract & authenticated thumbnail route
- Add `LinkPreviewDto` in `core/contract/src/WishlistDtos.kt`:
  ```kotlin
  @Serializable
  data class LinkPreviewDto(
      val status: String, // "pending" or "ready"
      val title: String? = null,
      val siteName: String? = null,
      val imagePath: String? = null, // e.g. "/link-previews/42/image"
  )
  ```
- Add `val linkPreviews: Map<String, LinkPreviewDto> = emptyMap()` to both `WishlistItemDto` and `SharedWishlistItemDto`.
- Add `GET /link-previews/{id}/image` in `ApiRoutes` (`LINK_PREVIEW_IMAGE`), protected by the same JWT + password-changed auth as wishlist routes, returning `200` with `image_content_type`, `image_bytes`, and `Cache-Control: private, max-age=86400, immutable` (or `ETag`).

**Why**: Keeping `urls` as the ordered list and adding `linkPreviews: Map<String, LinkPreviewDto> = emptyMap()` keeps older clients completely unaffected, avoids duplicating URL order, and omits failed previews cleanly.
**Alternative considered**: Replacing `urls` with `links: List<WishlistLinkDto>`. Rejected as a breaking change for older apps and redundant with `urls`.

### 5. Lightweight multiplatform client thumbnail loading & bounded pending refresh
- Add a small `LinkPreviewImageCache` in `app/shared` backed by the existing authenticated Ktor `HttpClient` and an in-memory LRU cache (`Map<String, ImageBitmap>` capped at e.g. 64 entries).
- Decode server-normalized JPEG/PNG bytes via a tiny `expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?` (`BitmapFactory` on Android, Skia `Image.makeFromEncoded` via Compose/Skiko on iOS and WasmJS — or Compose's built-in `ByteArray.decodeToImageBitmap()`).
- In `ItemText`, replace `urls.forEach { LinkText(it) }` with `WishlistLinkRow(url, preview, imageCache)`:
  - When `preview?.status == "ready"` and `!preview.title.isNullOrBlank()`: render a compact horizontal row (40–48dp rounded square thumbnail or link icon placeholder, 1-line title + 1-line siteName/host, and the existing copy icon on the right). Tapping the preview opens `url` directly in the browser; tapping the copy icon copies `url`.
  - Otherwise (`pending`, `null`, or blank title): render the existing `LinkText(url)` one-line fallback.
- When any loaded wishlist (`WishlistViewModel`, `ReceiverWishlistViewModel`, `AdminParticipantWishlistViewModel`) contains a `pending` preview, schedule a bounded follow-up refresh (e.g., up to 3 checks spaced 2s, 4s, 6s apart, cancelled when no `pending` previews remain or the screen leaves composition).

**Why**: Avoids adding Coil/Kamel across Android/iOS/WasmJS for a single 48dp server-normalized thumbnail use case, and works seamlessly with the authenticated Ktor client on WasmJS without cookie/header workarounds.

## Risks / Trade-offs

- **Bot-blocking sites (Amazon, Cloudflare, login-walled stores)** → Mitigated by graceful fallback to the existing one-line `LinkText` and a 6-hour failure cooldown so blocked URLs aren't repeatedly hammered.
- **DNS rebinding between validation and HTTP connect** → Mitigated by resolving DNS and validating all IPs before each hop; for full rebinding resistance on JVM, we can pin or verify the resolved IP / use a validating socket/connector if needed, and keep timeouts/size caps strict.
- **WebP images on JVM `ImageIO`** → Standard JDK `ImageIO` does not decode WebP out of the box. Mitigated by falling back cleanly to a title-only preview when an image cannot be decoded, rather than failing the entire preview (WebP support can be added later if needed).
- **SQLite database growth from cached thumbnails** → Mitigated by resizing to <= 256px (~5–30 KB each), sharing entries by URL across users, and pruning unreferenced `link_previews` rows.
- **Drag-to-reorder height changes while previews load** → `ItemList` measures item heights via `onGloballyPositioned`, so once a fallback row transitions to a compact preview row, its updated height is naturally re-measured before the next drag starts.

## Migration Plan

1. Deploy the new server version: `V019__link_previews.sql` creates `link_previews` without touching `wishlist_items` or `wishlist_item_links`.
2. On startup, the server enqueues all existing `wishlist_item_links` URLs into the background preview worker (bounded concurrency, non-blocking).
3. Older clients ignore `linkPreviews` and continue rendering `urls` normally; updated clients immediately display cached previews as they become `ready`.
4. **Rollback**: Reverting the client or server leaves `wishlist_items` and `wishlist_item_links` completely intact; the `link_previews` table is purely additive cache data.
