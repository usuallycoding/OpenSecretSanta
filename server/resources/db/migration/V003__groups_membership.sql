-- Minimal group skeleton: only who owns a group and who belongs to it, so admin access can be scoped.
-- Name, date, state and the rest of a group's fields are added by the groups change.
CREATE TABLE groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_username TEXT NOT NULL REFERENCES users (username)
);

CREATE TABLE group_members (
    group_id INTEGER NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    username TEXT NOT NULL REFERENCES users (username),
    PRIMARY KEY (group_id, username)
);

CREATE INDEX idx_groups_owner ON groups (owner_username);
CREATE INDEX idx_group_members_username ON group_members (username);
