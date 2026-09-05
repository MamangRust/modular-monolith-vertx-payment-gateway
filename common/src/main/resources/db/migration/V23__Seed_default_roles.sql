-- Default roles required by registration and admin-protected gateway routes.
-- Keep this idempotent so existing environments can migrate safely.
INSERT INTO roles (role_name)
VALUES ('ROLE_ADMIN'), ('ROLE_USER')
ON CONFLICT (role_name) DO NOTHING;
