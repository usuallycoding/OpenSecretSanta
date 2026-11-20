# Tasks

## 1. Contract & Domain Models

- [x] 1.1 Add `LinkPreviewDto` and optional `linkPreviews: Map<String, LinkPreviewDto> = emptyMap()` to `WishlistItemDto` and `SharedWishlistItemDto` in `core/contract/src/WishlistDtos.kt`, plus `LINK_PREVIEW_IMAGE` route helper in `core/contract/src/ApiRoutes.kt`; verify serialization/deserialization (including backward compatibility when `linkPreviews` is omitted) with unit tests in `core/contract/test/WishlistDtoSerializationTest.kt`.
- [x] 1.2 Add domain link preview models and repository/service ports in `core/domain/src/wishlist/` (e.g., `LinkPreview`, `LinkPreviewStatus`, `LinkPreviewStore`, `LinkPreviewQueue`) and verify domain compilation and tests pass.

## 2. Database Migration & Preview Cache Repository

- [x] 2.1 Add `server/resources/db/migration/V019__link_previews.sql` creating the `link_previews` table (`id`, `url` unique, `status`, `title`, `site_name`, `image_bytes`, `image_content_type`, `fetched_at`, `retry_after`) and verify migration tests run cleanly on fresh and upgraded databases with existing `wishlist_item_links` rows.
- [x] 2.2 Implement `SqliteLinkPreviewRepository` in `server/src/data/` supporting batch lookup by URLs, upsert of `pending`/`ready`/`failed` entries, thumbnail read by preview id, querying uncached `wishlist_item_links` URLs for startup backfill, and pruning unreferenced `link_previews` rows; verify with repository unit tests.

## 3. SSRF-Safe Fetcher, HTML/OG Parser & Thumbnail Resizer

- [x] 3.1 Implement URL/IP validator and bounded redirect-checking HTTP fetcher (rejecting loopback, private, link-local, multicast, unspecified, and CGNAT addresses on initial URL and every redirect hop up to 5; enforcing connect/read timeouts and HTML/image size caps); verify with unit tests covering private IPs, redirect-to-private-IP blocking, redirect limit, and oversized responses.
- [x] 3.2 Implement HTML metadata extraction (`og:title` -> `twitter:title` -> `<title>`, `og:site_name` -> host without `www.`, `og:image` -> `twitter:image` resolved against final URL, entity decoding, whitespace normalization, length capping) and verify with unit tests across OG, Twitter, plain HTML, relative image URLs, and malformed HTML.
- [x] 3.3 Implement thumbnail decoding and resizing (accepting `image/jpeg`, `image/png`, `image/webp`, `image/gif`; resizing to max 256px longest edge preserving aspect ratio; normalizing to JPEG/PNG; degrading cleanly to title-only if decoding fails) and verify with unit tests.

## 4. Background Worker, Routes & Wishlist Integration

- [x] 4.1 Implement `LinkPreviewService` with bounded concurrency, in-flight URL deduplication, 6-hour failure cooldown, startup backfill of existing `wishlist_item_links`, and orphan cleanup; wire it in `server/src/di/ServerModules.kt` and `server/src/Application.kt`, and verify deduplication, cooldown, backfill, and cleanup with unit tests.
- [x] 4.2 Expose authenticated `GET /link-previews/{id}/image` (enforcing JWT auth and password-changed check, returning 200 with cached content type and cache headers or 404 when missing) and verify with Ktor route tests.
- [x] 4.3 Attach `linkPreviews` in `WishlistRoutes`, `GiftRoutes`, and `AdminOversightRoutes` mappers and queue missing/retry-eligible URLs non-blockingly on item add, edit, and wishlist reads; verify with route tests covering `ready`, `pending`, omitted `failed`, backward-compatible `urls`/`url`, and non-blocking saves.

## 5. Client Multiplatform Thumbnail Loader & Preview UI

- [x] 5.1 Add `decodeImageBitmap(bytes: ByteArray): ImageBitmap?` across Android, iOS, and WasmJS in `app/shared/` and implement `LinkPreviewImageCache` backed by the authenticated Ktor `HttpClient` with a bounded in-memory LRU cache; verify with unit tests using Ktor mock client.
- [x] 5.2 Update wishlist client models, repositories, and ViewModels (`WishlistViewModel`, `ReceiverWishlistViewModel`, `AdminParticipantWishlistViewModel`) to carry `linkPreviews` and run a bounded follow-up refresh when any visible link preview is `pending`; verify with ViewModel tests.
- [x] 5.3 Update `ItemText` in `app/shared/src/ui/wishlist/WishlistScreen.kt` (shared by own wishlist, Santa's receiver view, and admin wishlist sheets) to render `ready` previews as a compact horizontal row (square thumbnail or link icon placeholder, title, site name/host, and copy icon) opening the link directly in the browser, falling back to `LinkText` when pending or unavailable, with screen reader semantics preserved; verify UI logic and tests pass.

## 6. End-to-End Verification

- [x] 6.1 Run the full test suites across `core/contract`, `core/domain`, `server`, and `app/shared` (including Android/WasmJS/JVM targets as configured) and verify all tests pass with no regressions.
