-- Replaces the V003 skeleton (owner and members only) with the full group tables.
-- Nothing could write to groups or group_members before this change, so dropping them loses no data.
DROP TABLE group_members;
DROP TABLE groups;

CREATE TABLE groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_username TEXT NOT NULL REFERENCES users (username),
    name TEXT NOT NULL,
    description TEXT,
    location TEXT,
    event_date TEXT,
    event_time TEXT,
    state TEXT NOT NULL DEFAULT 'draft' CHECK (state IN ('draft', 'pending', 'active', 'archived')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- No foreign key to users: a Draft may name people who do not have an account yet.
CREATE TABLE group_participants (
    group_id INTEGER NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    username TEXT NOT NULL CHECK (username = lower(username)),
    position INTEGER NOT NULL,
    PRIMARY KEY (group_id, username)
);

CREATE TABLE assignments (
    group_id INTEGER NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    santa TEXT NOT NULL,
    receiver TEXT NOT NULL,
    PRIMARY KEY (group_id, santa),
    UNIQUE (group_id, receiver),
    CHECK (santa <> receiver)
);

CREATE INDEX idx_groups_owner ON groups (owner_username);
CREATE INDEX idx_group_participants_username ON group_participants (username);
