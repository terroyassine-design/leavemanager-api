-- Incremented on each password change: tokens carrying an older value are rejected
ALTER TABLE users ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0;
