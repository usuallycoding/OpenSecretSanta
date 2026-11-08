-- The newest announcement time a person has seen in a group, one row per person and group, so that the app can show how many
-- announcements are new to them. Like wishlist_items there is no foreign key to group_participants: someone removed from a
-- Pending group keeps their mark, and every access checks that the caller still participates. Deleting a group deletes the marks.
CREATE TABLE announcement_reads (
    group_id INTEGER NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    username TEXT NOT NULL REFERENCES users (username),
    seen_up_to INTEGER NOT NULL,
    PRIMARY KEY (group_id, username)
);
