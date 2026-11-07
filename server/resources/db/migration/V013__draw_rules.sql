-- The owner's draw rules for a group: kind "must" (the giver gives to the receiver) or "never" (does not).
-- They go with the group, and are deleted with a removed participant by the repository that removes them.
CREATE TABLE draw_rules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id INTEGER NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    giver TEXT NOT NULL,
    receiver TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('must', 'never')),
    UNIQUE (group_id, giver, receiver),
    CHECK (giver <> receiver)
);
