-- Who created an account: the administrator whose action made it (adding a new username to a Pending group). It lets an
-- administrator find and delete a stray account that is in no group. The first administrator has no creator.
ALTER TABLE users ADD COLUMN created_by TEXT REFERENCES users (username) ON DELETE SET NULL;

-- With exactly one administrator, every existing non-administrator account was made by that administrator.
UPDATE users
SET created_by = (SELECT username FROM users WHERE is_admin = 1)
WHERE is_admin = 0 AND (SELECT COUNT(*) FROM users WHERE is_admin = 1) = 1;
