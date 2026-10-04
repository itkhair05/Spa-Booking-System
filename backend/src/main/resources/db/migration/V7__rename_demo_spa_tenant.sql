-- Rename the single demo tenant to the production TIKEY SPA identity.
-- Renames in place: tenant id and all related rows are preserved.
UPDATE tenants
SET name = 'TIKEY SPA', slug = 'tikey-spa'
WHERE slug = 'demo-spa';
