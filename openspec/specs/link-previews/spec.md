# link-previews Specification

## Purpose

Defines server-side link preview resolution for wishlist URLs: safely fetching external web pages and preview images, resizing and caching thumbnails on the server, serving cached thumbnails to authenticated clients, backfilling existing links, and managing failure cooldowns and cache cleanup.

## Requirements

### Requirement: Server-side metadata unfurling
The server SHALL fetch metadata for `http` and `https` wishlist URLs and extract a normalized, length-capped page title (`og:title`, then `twitter:title`, then `<title>`), a site name (`og:site_name`, falling back to the host without `www.`), and an optional preview image URL (`og:image`, then `twitter:image`, resolved against the final page URL).

#### Scenario: Open Graph metadata present
- **WHEN** the server unfurls a page containing `og:title`, `og:site_name`, and `og:image`
- **THEN** the stored preview records that title, site name, and cached thumbnail derived from `og:image`

#### Scenario: Fallback to HTML title and host
- **WHEN** the server unfurls a page with an HTML `<title>` and no Open Graph tags
- **THEN** the stored preview records the HTML title, uses the URL's host as the site name, and has no thumbnail

#### Scenario: Relative preview image URL
- **WHEN** a page declares `<meta property="og:image" content="/images/socks.jpg">`
- **THEN** the server resolves it against the final page URL before fetching the image

### Requirement: SSRF-safe and bounded outbound fetching
Before connecting to any target URL or redirect target, the server SHALL resolve the host and refuse any address that is loopback, private, link-local, multicast, or unspecified. Outbound fetches SHALL enforce strict connect and read timeouts, follow at most 5 redirects (re-validating scheme and resolved IP on each hop), cap HTML reads and raw image downloads, and accept only supported image content types (`image/jpeg`, `image/png`, `image/webp`, `image/gif`) for thumbnails.

#### Scenario: Private network address blocked
- **WHEN** a wishlist link points to `http://127.0.0.1:8080/secret` or `http://192.168.1.1/`
- **THEN** the server refuses the fetch without connecting and marks the preview as failed

#### Scenario: Redirect to private address blocked
- **WHEN** a public URL redirects to `http://169.254.169.254/latest/meta-data/`
- **THEN** the server aborts the redirect without connecting and marks the preview as failed

#### Scenario: Oversized response rejected
- **WHEN** a target page or image exceeds the configured size limit
- **THEN** the server stops reading, discards the oversized payload, and does not exhaust memory or storage

### Requirement: Server-proxied and cached thumbnails
The server SHALL resize fetched preview images to at most 256 pixels on the longest edge, cache the normalized image bytes and content type in SQLite, and serve them via authenticated `GET /link-previews/{id}/image` with cache headers. Requests from users who must change a temporary password SHALL be refused with `password_change_required`, and missing previews or thumbnails SHALL return 404.

#### Scenario: Cached thumbnail served
- **WHEN** an authenticated user requests `GET /link-previews/{id}/image` for a preview that has a cached thumbnail
- **THEN** the response is 200 with the cached image content type, thumbnail bytes, and cache headers

#### Scenario: Unauthenticated request refused
- **WHEN** an unauthenticated request reaches `GET /link-previews/{id}/image`
- **THEN** the response is 401

#### Scenario: Missing thumbnail
- **WHEN** an authenticated user requests `GET /link-previews/{id}/image` for a preview id that does not exist or has no image
- **THEN** the response is 404

### Requirement: Shared cache, failure cooldown, and deduplication
Link previews SHALL be cached by exact URL and shared across all wishlists that reference the same URL. Concurrent requests to resolve the same URL SHALL be deduplicated so only one outbound fetch runs at a time. When a fetch fails or yields no usable metadata, the server SHALL record a failed entry with a cooldown period before retrying so blocked or broken sites are not repeatedly fetched on every wishlist read.

#### Scenario: Shared URL across items
- **WHEN** two participants add the same URL to their wishlists
- **THEN** the server stores one preview cache entry and serves it to both wishlists

#### Scenario: Failed fetch enters cooldown
- **WHEN** a site blocks the server's fetch with 403 or times out
- **THEN** the preview is marked failed and subsequent reads during the cooldown window do not trigger another outbound fetch

#### Scenario: Retry after cooldown expires
- **WHEN** a wishlist containing a previously failed URL is read after its cooldown period has elapsed
- **THEN** the server queues a fresh background fetch for that URL

### Requirement: Asynchronous fetching, on-demand resolution, and startup backfill
Preview fetching SHALL always run asynchronously in the background with bounded concurrency and SHALL NOT block HTTP requests. The server SHALL queue preview fetches:
- when wishlist items are added or edited with URLs that have no fresh cache entry,
- on demand when a wishlist is read and contains URLs without a cache entry or past their failure cooldown, and
- on server startup for existing URLs in `wishlist_item_links` that do not yet have a cache entry.

#### Scenario: Existing links backfilled on startup
- **WHEN** the server starts on a database containing existing wishlist links with no preview cache entries
- **THEN** those URLs are queued in the background for preview fetching without delaying server startup

#### Scenario: On-demand queue on read
- **WHEN** a wishlist is read and one of its links has no preview cache entry
- **THEN** the read returns immediately and queues that URL for background fetching

### Requirement: Unreferenced preview cleanup
The server SHALL remove cached preview entries that are no longer referenced by any row in `wishlist_item_links` so removed links do not accumulate unbounded thumbnail data.

#### Scenario: Orphan preview pruned
- **WHEN** the last wishlist item referencing a URL is deleted or edited to remove that URL and cleanup runs
- **THEN** the unreferenced `link_previews` entry and its cached thumbnail bytes are removed
