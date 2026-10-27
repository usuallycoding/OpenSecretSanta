-- Accounts, with credentials kept apart from the profile fields.
-- username is always stored lowercase, which makes the primary key case-insensitive.
CREATE TABLE users (
    username TEXT PRIMARY KEY CHECK (username = lower(username)),
    full_name TEXT,
    email TEXT,
    phone TEXT,
    address TEXT,
    delivery_info TEXT,
    password_hash TEXT NOT NULL,
    temp_password_enc BLOB,
    must_change_password INTEGER NOT NULL DEFAULT 0,
    is_admin INTEGER NOT NULL DEFAULT 0,
    token_version INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Failed-login counters; deliberately not tied to users so unknown usernames behave the same.
CREATE TABLE login_attempts (
    username TEXT PRIMARY KEY,
    failures INTEGER NOT NULL,
    locked_until INTEGER
);
