-- A participant's wishlist for one group: the rows sharing (group_id, username), ordered by position.
-- UNIQUE (group_id, username, position) means two items of one list can never share a place, and its
-- index also serves reads of a list in order. Deleting a group removes its lists.
-- No foreign key to group_participants: someone removed from a Pending group keeps their (hidden) rows,
-- and every access checks that the caller still participates.
CREATE TABLE wishlist_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id INTEGER NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    username TEXT NOT NULL REFERENCES users (username),
    position INTEGER NOT NULL,
    title TEXT NOT NULL,
    note TEXT,
    url TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    UNIQUE (group_id, username, position)
);
