-- The daily digest. Wishlist changes, questions, answers and letters no longer notify one by one: they mark the person they are
-- for, and one notification a day tells every marked person that something changed. The four switches for them become one.
-- A person who had any of the four on keeps the digest on; one who had all four off has it off. Someone with no row stays on.
ALTER TABLE notification_prefs ADD COLUMN daily INTEGER NOT NULL DEFAULT 1 CHECK (daily IN (0, 1));
UPDATE notification_prefs SET daily = CASE WHEN wishlist + questions + answers + letters > 0 THEN 1 ELSE 0 END;

-- Who has news waiting for the next digest: one row per person, cleared when the digest is sent. Deleting a person removes it.
CREATE TABLE digest_marks (
    username TEXT PRIMARY KEY REFERENCES users (username) ON DELETE CASCADE,
    marked_at INTEGER NOT NULL
);
