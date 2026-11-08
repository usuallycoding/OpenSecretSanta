-- The announcements a group's owner has sent, kept so that the participants and the owner can read them later. number counts
-- from 1 within the group, and announcements disappear with the group.
CREATE TABLE announcements (
    group_id INTEGER NOT NULL,
    number INTEGER NOT NULL CHECK (number >= 1),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    sent_at INTEGER NOT NULL,
    PRIMARY KEY (group_id, number),
    FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE
);
