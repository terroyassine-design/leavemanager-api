-- Demo accounts get a read-only profile (see User.ensureEditable)
ALTER TABLE users ADD COLUMN demo BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET demo = TRUE WHERE email LIKE '%@demo.com';
