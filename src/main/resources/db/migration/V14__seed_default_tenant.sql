-- Seed a default tenant for dev/test environments where no tenant bootstrap exists.
INSERT INTO tenants (id, name, code, active, created_at, updated_at)
VALUES ('tenant-001', 'Default Tenant', 'TENANT001', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
