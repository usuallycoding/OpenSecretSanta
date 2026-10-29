-- Push notifications. A device is identified by its push token, so a token registered again for another
-- person simply changes owner (the primary key makes it an upsert). Deleting a person removes their devices
-- and their choices.
CREATE TABLE push_devices (
    token TEXT PRIMARY KEY,
    username TEXT NOT NULL REFERENCES users (username) ON DELETE CASCADE,
    platform TEXT NOT NULL CHECK (platform IN ('android', 'web')),
    created_at INTEGER NOT NULL
);
CREATE INDEX push_devices_by_username ON push_devices (username);

-- What a person wants to be told about. No row means all four are on.
CREATE TABLE notification_prefs (
    username TEXT PRIMARY KEY REFERENCES users (username) ON DELETE CASCADE,
    announcements INTEGER NOT NULL DEFAULT 1 CHECK (announcements IN (0, 1)),
    wishlist INTEGER NOT NULL DEFAULT 1 CHECK (wishlist IN (0, 1)),
    questions INTEGER NOT NULL DEFAULT 1 CHECK (questions IN (0, 1)),
    answers INTEGER NOT NULL DEFAULT 1 CHECK (answers IN (0, 1))
);
