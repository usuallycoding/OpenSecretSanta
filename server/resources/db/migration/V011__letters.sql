-- One-way letters from a receiver to their Santa. A letter hangs off the receiver's assignment, so its Santa is always the
-- one that assignment names, and letters disappear with the group. number counts from 1 within one receiver's letters.
CREATE TABLE letters (
    group_id INTEGER NOT NULL,
    receiver TEXT NOT NULL,
    number INTEGER NOT NULL CHECK (number >= 1),
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    sent_at INTEGER NOT NULL,
    PRIMARY KEY (group_id, receiver, number),
    FOREIGN KEY (group_id, receiver) REFERENCES assignments (group_id, receiver) ON DELETE CASCADE
);

-- The fifth notification switch, letters from your receiver: on for everyone until they turn it off.
ALTER TABLE notification_prefs ADD COLUMN letters INTEGER NOT NULL DEFAULT 1;
