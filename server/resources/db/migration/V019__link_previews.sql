-- Cached metadata and resized thumbnail bytes for wishlist links, keyed by exact URL.
-- Shared across all wishlists that reference the same URL; rows no longer referenced by
-- wishlist_item_links are pruned periodically by the server.
CREATE TABLE link_previews (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK (status IN ('pending', 'ready', 'failed')),
    title TEXT,
    site_name TEXT,
    image_bytes BLOB,
    image_content_type TEXT,
    fetched_at INTEGER,
    retry_after INTEGER
);

CREATE INDEX idx_wishlist_item_links_url ON wishlist_item_links (url);
