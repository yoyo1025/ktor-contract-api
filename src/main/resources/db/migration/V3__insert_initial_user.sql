-- Initial admin user
-- The password_hash is a placeholder that must be replaced before first login.
-- Set ADMIN_PASSWORD_HASH environment variable to override via application startup.
INSERT INTO users (id, login_id, password_hash, name)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    '__PLACEHOLDER_MUST_BE_REPLACED__',
    'Administrator'
) ON CONFLICT (id) DO NOTHING;
