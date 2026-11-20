# Proposal

## Why

Wishlist links are currently displayed as truncated raw URLs, which makes it hard for a Secret Santa (or the list owner) to tell what each link points to without opening every tab. Showing a compact thumbnail-and-title preview for each link makes wishlists immediately scannable while keeping all platforms — including WasmJS web — working reliably without CORS issues or leaking viewers' IPs to third-party stores.

## What Changes

- Add server-side link preview unfurling and caching in SQLite (`link_previews` table): fetch page metadata (title, site name/host, preview image), resize and cache thumbnail bytes on the server, and serve cached thumbnails through an authenticated server endpoint.
- Fetch previews asynchronously without blocking item saves:
  - Queue new or changed URLs on item add/edit (`POST`/`PUT`).
  - Queue missing or retry-eligible URLs on demand when wishlists are read.
  - Backfill existing `wishlist_item_links` rows on server startup so existing wishlists gain previews.
- Enforce SSRF and resource-abuse protections on outbound preview fetches (block private/loopback/link-local/multicast IPs across DNS and redirects, cap redirects, timeouts, HTML/image sizes, and allowed image types) and cache failures with a cooldown.
- Extend wishlist item responses (`WishlistItemDto`, `SharedWishlistItemDto`) backward-compatibly with optional preview metadata per link while keeping `urls` (and deprecated `url`) intact for older apps.
- Update the client wishlist UI (`ItemText`, shared by own wishlist, receiver wishlist, and admin wishlist sheets) to render each link as a compact horizontal preview row (square thumbnail + title + domain) alongside the existing copy button, falling back to title-only or the existing one-line URL when pending or unavailable, with a brief follow-up refresh when newly saved links are still pending.

## Capabilities

### New Capabilities
- `link-previews`: Server-side URL metadata unfurling, SSRF-safe fetching, resized thumbnail caching and proxying, background/on-demand/startup backfill, failure cooldowns, and cache cleanup.

### Modified Capabilities
- `wishlist`: Wishlist responses attach available link preview metadata to item links alongside `urls`, and item writes/reads trigger non-blocking preview resolution.
- `client-wishlist`: Wishlist links render as compact horizontal preview rows (thumbnail, title, domain) alongside the copy button, falling back to title-only or the one-line URL when pending or unavailable, and refreshing briefly when pending previews are present.

## Impact

- **Core contract (`core/contract`)**: Additive DTOs/fields on `WishlistItemDto`, `SharedWishlistItemDto`, and a new thumbnail route in `ApiRoutes` (backward-compatible with older clients).
- **Core domain (`core/domain`)**: Domain models/ports for link preview metadata and async preview resolution.
- **Server (`server`)**: New SQLite migration (`link_previews`), SSRF-safe HTTP fetcher and HTML/OG metadata parser, JVM thumbnail resizer, background preview worker + startup backfill, and authenticated thumbnail route.
- **Client (`app/shared`)**: Ktor-based authenticated thumbnail fetcher with in-memory cache and multiplatform `ImageBitmap` decoding (Android, iOS, WasmJS), compact preview link row composable replacing plain `LinkText` when preview data is present, and bounded refresh for pending previews.
