/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- Seed default roles (authorities)
INSERT INTO authorities (authority)
VALUES ('ROLE_USER')
ON CONFLICT (authority) DO NOTHING;
INSERT INTO authorities (authority)
VALUES ('ROLE_ADMIN')
ON CONFLICT (authority) DO NOTHING;
INSERT INTO authorities (authority)
VALUES ('ROLE_SUPER_ADMIN')
ON CONFLICT (authority) DO NOTHING;
INSERT INTO authorities (authority)
VALUES ('ROLE_ROOT')
ON CONFLICT (authority) DO NOTHING;

-- Seed default permissions (Revised for clarity and reduced redundancy)
-- Suffix _OWN is removed; ownership checks are typically done in code with @PreAuthorize or PermissionEvaluator.
-- Suffix _ANY implies action on any record, usually for higher roles.
INSERT INTO permissions (name, description)
VALUES ('PROFILE_READ_OWN', 'Allows reading own user profile')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('PROFILE_EDIT_OWN', 'Allows editing own user profile')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('PASSWORD_CHANGE_OWN', 'Allows changing own user password')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('PASSKEY_MANAGE_OWN', 'Allows managing own passkeys')
ON CONFLICT (name) DO NOTHING;
-- General user management permissions for admins
INSERT INTO permissions (name, description)
VALUES ('USER_VIEW_ANY', 'Allows viewing any user profile')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('USER_EDIT_ANY', 'Allows editing any user profile')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('USER_DELETE_ANY', 'Allows deleting any user account')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('USER_PASSWORD_RESET_ANY', 'Allows resetting any user password')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('USER_ACCOUNT_LOCK_ANY', 'Allows locking any user account')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('USER_ACCOUNT_UNLOCK_ANY', 'Allows unlocking any user account')
ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description)
VALUES ('USER_ROLE_ASSIGN_ANY', 'Allows assigning roles to any user')
ON CONFLICT (name) DO NOTHING;
-- OAuth Client Management
INSERT INTO permissions (name, description)
VALUES ('OAUTH_CLIENT_MANAGE', 'Allows managing OAuth2 clients')
ON CONFLICT (name) DO NOTHING;
-- Add other specific application permissions as needed (e.g., for amlume-shop if authserver manages its permissions)
-- INSERT INTO permissions (name, description) VALUES ('SHOP_PRODUCT_CREATE', 'Allows creating products in the shop') ON CONFLICT (name) DO NOTHING;

-- Assign permissions to roles (example)
-- Assuming you know the IDs or can look them up. For simplicity, let's assume IDs or use subqueries if complex.
-- Example: Give ROLE_USER the USER_READ_PROFILE permission
-- INSERT INTO role_permissions (role_id, permission_id)
-- SELECT r.id, p.id
-- FROM authorities r, permissions p
-- WHERE r.authority = 'ROLE_USER' AND p.name = 'USER_READ_PROFILE'
-- ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Example: Give ROLE_ADMIN all three example permissions
-- INSERT INTO role_permissions (role_id, permission_id)
-- SELECT r.id, p.id
-- FROM authorities r, permissions p
-- WHERE r.authority = 'ROLE_ADMIN' AND p.name IN ('USER_READ_PROFILE', 'USER_EDIT_PROFILE', 'ADMIN_MANAGE_USERS')
-- ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign permissions to roles
-- ROLE_USER: Basic self-service permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_USER'
  AND p.name = 'PROFILE_READ_OWN'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_USER'
  AND p.name = 'PROFILE_EDIT_OWN'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_USER'
  AND p.name = 'PASSWORD_CHANGE_OWN'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_USER'
  AND p.name = 'PASSKEY_MANAGE_OWN'
ON CONFLICT DO NOTHING;

-- ROLE_ADMIN: Manages users (but perhaps not other admins, depending on your RoleHierarchy and enforcement)
-- An admin implicitly has ROLE_USER permissions via RoleHierarchy.
-- If you want explicit DB entries, add them. For now, assuming RoleHierarchy handles implication.
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ADMIN'
  AND p.name = 'USER_VIEW_ANY'
ON CONFLICT DO NOTHING;
-- Example: Admin can edit users but maybe not assign all roles or delete higher admins.
-- For simplicity, let's give USER_EDIT_ANY but rely on code logic to prevent editing higher roles.
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ADMIN'
  AND p.name = 'USER_EDIT_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ADMIN'
  AND p.name = 'USER_PASSWORD_RESET_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ADMIN'
  AND p.name = 'USER_ACCOUNT_LOCK_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ADMIN'
  AND p.name = 'USER_ACCOUNT_UNLOCK_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ADMIN'
  AND p.name = 'OAUTH_CLIENT_MANAGE'
ON CONFLICT DO NOTHING;


-- ROLE_SUPER_ADMIN: More power, can manage admins.
-- Implicitly has ROLE_ADMIN permissions.
-- Add permissions specific to SUPER_ADMIN, e.g., managing ROLE_ADMIN users.
-- For now, let's assume USER_EDIT_ANY and USER_DELETE_ANY are sufficient if RoleHierarchy is used.
-- If ROLE_SUPER_ADMIN needs more than ROLE_ADMIN, add specific permissions here.
-- Example:
-- INSERT INTO permissions (name, description) VALUES ('ADMIN_ROLE_MANAGE', 'Allows managing admin roles') ON CONFLICT (name) DO NOTHING;
-- INSERT INTO role_permissions (role_id, permission_id) SELECT a.id, p.id FROM authorities a, permissions p WHERE a.authority = 'ROLE_SUPER_ADMIN' AND p.name = 'ADMIN_ROLE_MANAGE' ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_SUPER_ADMIN'
  AND p.name = 'USER_DELETE_ANY'
ON CONFLICT DO NOTHING;
-- Example: Super admin can delete users

-- ROLE_ROOT: Full control
-- Implicitly has ROLE_SUPER_ADMIN permissions.
-- Often, ROLE_ROOT might not need explicit permission mappings if it bypasses checks or has all permissions by default in code.
-- However, for explicitness or if other systems read this:
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ROOT'
  AND p.name = 'USER_VIEW_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ROOT'
  AND p.name = 'USER_EDIT_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ROOT'
  AND p.name = 'USER_DELETE_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ROOT'
  AND p.name = 'USER_PASSWORD_RESET_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ROOT'
  AND p.name = 'USER_ACCOUNT_LOCK_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ROOT'
  AND p.name = 'USER_ACCOUNT_UNLOCK_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ROOT'
  AND p.name = 'USER_ROLE_ASSIGN_ANY'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT a.id, p.id
FROM authorities a,
     permissions p
WHERE a.authority = 'ROLE_ROOT'
  AND p.name = 'OAUTH_CLIENT_MANAGE'
ON CONFLICT DO NOTHING;
/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- Add any other permission to ROLE_ROOT if it's not covered by the above.
