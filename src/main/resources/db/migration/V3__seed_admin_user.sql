INSERT INTO app_user (id, username, password_hash, role)
VALUES (
    UUID_TO_BIN(UUID()),
    'admin',
    '$2a$10$7sY0bLZ8nN3YwHk0oV1Y6e8vD0p0M8m2xT6YkZgTzNQmZbGmKpY4W',
    'ADMIN'
);
