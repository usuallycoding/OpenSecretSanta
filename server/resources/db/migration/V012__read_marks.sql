-- What a Santa has already seen of their receiver's wishlist, of the answers to their questions and of their letters:
-- the newest time seen, per kind. A mark hangs off the Santa's assignment, so it is only ever the Santa's own and it
-- disappears with the group.
CREATE TABLE read_marks (
    group_id INTEGER NOT NULL,
    santa TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('wishlist', 'answers', 'letters')),
    seen_up_to INTEGER NOT NULL,
    PRIMARY KEY (group_id, santa, kind),
    FOREIGN KEY (group_id, santa) REFERENCES assignments (group_id, santa) ON DELETE CASCADE
);
