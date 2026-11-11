-- An item holds up to 10 links, in order: the rows sharing item_id, ordered by position. The cap is kept by the domain,
-- UNIQUE (item_id, position) means two links of one item can never share a place, and its index also serves reads of an
-- item's links in order. Deleting an item, or the group it belongs to, removes its links.
-- The one link an item used to have becomes its first link. updated_at is left alone: this is the same list, not an edit.
CREATE TABLE wishlist_item_links (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL REFERENCES wishlist_items (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    url TEXT NOT NULL,
    UNIQUE (item_id, position)
);

INSERT INTO wishlist_item_links (item_id, position, url)
SELECT id, 0, TRIM(url) FROM wishlist_items WHERE url IS NOT NULL AND TRIM(url) <> '';

ALTER TABLE wishlist_items DROP COLUMN url;
