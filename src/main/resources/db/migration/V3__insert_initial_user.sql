-- Initial admin user (password: "admin123")
-- bcrypt hash generated with cost factor 10
INSERT INTO users (id, login_id, password_hash, name)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$dXJ3SW6G7P50lGmMQiS4MOmFih0h4gAfUHeDv2BLBID5GWQNH0gqS',
    'Administrator'
);
