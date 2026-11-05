-- A participant's delivery info (name, email, phone, address, delivery information) now belongs to a group, not to the
-- account. A row exists once the person has saved something for that group; a missing row reads as empty details.
CREATE TABLE group_delivery_info (
    group_id INTEGER NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    username TEXT NOT NULL CHECK (username = lower(username)),
    full_name TEXT,
    email TEXT,
    phone TEXT,
    address TEXT,
    delivery_info TEXT,
    PRIMARY KEY (group_id, username)
);

-- Every profile saved on an account is copied into each group the person is in, in any state, so nobody loses their details.
INSERT INTO group_delivery_info (group_id, username, full_name, email, phone, address, delivery_info)
SELECT p.group_id, u.username, u.full_name, u.email, u.phone, u.address, u.delivery_info
FROM group_participants p
JOIN users u ON u.username = p.username
WHERE u.full_name IS NOT NULL OR u.email IS NOT NULL OR u.phone IS NOT NULL OR u.address IS NOT NULL OR u.delivery_info IS NOT NULL;

ALTER TABLE users DROP COLUMN full_name;
ALTER TABLE users DROP COLUMN email;
ALTER TABLE users DROP COLUMN phone;
ALTER TABLE users DROP COLUMN address;
ALTER TABLE users DROP COLUMN delivery_info;
