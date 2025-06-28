/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- V2: Seed initial hierarchical roles and permissions.

-- 1. Seed default roles into the new 'roles' table with ltree paths
-- The hierarchy is: ROOT > SUPER_ADMIN > ADMIN > USER
-- Technical roles are also children of ADMIN.

-- Top-level role
INSERT INTO roles (name, description, path, parent_id)
VALUES ('ROLE_ROOT', 'Root-level administrator with all permissions.', 'ROLE_ROOT', NULL)
ON CONFLICT (name) DO NOTHING;

-- Child of ROLE_ROOT
INSERT INTO roles (name, description, path, parent_id)
SELECT 'ROLE_SUPER_ADMIN',
       'Super administrator with extensive permissions.',
       'ROLE_ROOT.ROLE_SUPER_ADMIN',
       id
FROM roles
WHERE name = 'ROLE_ROOT'
ON CONFLICT (name) DO NOTHING;

-- Child of ROLE_SUPER_ADMIN
INSERT INTO roles (name, description, path, parent_id)
SELECT 'ROLE_ADMIN',
       'Administrator with user and client management permissions.',
       'ROLE_ROOT.ROLE_SUPER_ADMIN.ROLE_ADMIN',
       id
FROM roles
WHERE name = 'ROLE_SUPER_ADMIN'
ON CONFLICT (name) DO NOTHING;

-- Child of ROLE_ADMIN
INSERT INTO roles (name, description, path, parent_id)
SELECT 'ROLE_USER',
       'Default role for all authenticated users.',
       'ROLE_ROOT.ROLE_SUPER_ADMIN.ROLE_ADMIN.ROLE_USER',
       id
FROM roles
WHERE name = 'ROLE_ADMIN'
ON CONFLICT (name) DO NOTHING;

-- Technical Roles as children of ROLE_ADMIN
INSERT INTO roles (name, description, path, parent_id)
SELECT 'ROLE_AUTH_ADMIN',
       'Technical admin for auth server management.',
       'ROLE_ROOT.ROLE_SUPER_ADMIN.ROLE_ADMIN.ROLE_AUTH_ADMIN',
       id
FROM roles
WHERE name = 'ROLE_ADMIN'
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description, path, parent_id)
SELECT 'ROLE_AUTH_SUPPORT',
       'Technical support role for auth server.',
       'ROLE_ROOT.ROLE_SUPER_ADMIN.ROLE_ADMIN.ROLE_AUTH_SUPPORT',
       id
FROM roles
WHERE name = 'ROLE_ADMIN'
ON CONFLICT (name) DO NOTHING;


-- 2. Seed default permissions (All permissions MUST have a unique TSID for their 'id' column)
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2V3', 'PROFILE_READ_OWN', 'Allows reading own user profile')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2V4', 'PROFILE_EDIT_OWN', 'Allows editing own user profile')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2V5', 'PASSWORD_CHANGE_OWN', 'Allows changing own user password')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2V6', 'PASSKEY_MANAGE_OWN', 'Allows managing own passkeys')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2V7', 'USER_READ_ANY', 'Allows viewing any user profile')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2V8', 'USER_EDIT_ANY', 'Allows editing any user profile')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2V9', 'USER_DELETE_ANY', 'Allows deleting any user account')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2VA', 'USER_PASSWORD_RESET_ANY', 'Allows resetting any user password')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2VB', 'USER_ACCOUNT_LOCK_ANY', 'Allows locking any user account')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2VC', 'USER_ACCOUNT_UNLOCK_ANY', 'Allows unlocking any user account')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2VD', 'USER_ROLE_ASSIGN_ANY', 'Allows assigning roles to any user')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2VG', 'USER_MANAGE_ANY', 'Allows managing any user account')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2VE', 'OAUTH_CLIENT_MANAGE', 'Allows managing OAuth2 clients')
ON CONFLICT (id) DO NOTHING;
INSERT INTO permissions (id, name, description)
VALUES ('01J3T8Y4Z5N6M7P8Q9R0S1T2VF', 'AUDIT_LOG_READ', 'Allows viewing authserver audit logs')
ON CONFLICT (id) DO NOTHING;


/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */


-- 3. Assign permissions to roles
-- ROLE_USER: Basic self-service permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'ROLE_USER'
  AND p.name IN ('PROFILE_READ_OWN', 'PROFILE_EDIT_OWN', 'PASSWORD_CHANGE_OWN', 'PASSKEY_MANAGE_OWN')
ON CONFLICT DO NOTHING;

-- ROLE_ADMIN: Manages users
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'ROLE_ADMIN'
  AND p.name IN
      ('USER_READ_ANY', 'USER_EDIT_ANY', 'USER_PASSWORD_RESET_ANY', 'USER_ACCOUNT_LOCK_ANY', 'USER_ACCOUNT_UNLOCK_ANY',
       'OAUTH_CLIENT_MANAGE')
ON CONFLICT DO NOTHING;

-- ROLE_SUPER_ADMIN: More power
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND p.name IN ('USER_DELETE_ANY', 'USER_MANAGE_ANY')
ON CONFLICT DO NOTHING;

-- ROLE_ROOT: Full control (gets all defined permissions for explicitness)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'ROLE_ROOT'
  AND p.name IN (
                 'PROFILE_READ_OWN', 'PROFILE_EDIT_OWN', 'PASSWORD_CHANGE_OWN', 'PASSKEY_MANAGE_OWN',
                 'USER_READ_ANY', 'USER_EDIT_ANY', 'USER_DELETE_ANY', 'USER_PASSWORD_RESET_ANY',
                 'USER_ACCOUNT_LOCK_ANY', 'USER_ACCOUNT_UNLOCK_ANY', 'USER_ROLE_ASSIGN_ANY',
                 'USER_MANAGE_ANY', 'OAUTH_CLIENT_MANAGE', 'AUDIT_LOG_READ'
    )
ON CONFLICT DO NOTHING;

-- Assign Permissions to Technical Roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'ROLE_AUTH_SUPPORT'
  AND p.name IN ('USER_READ_ANY', 'USER_PASSWORD_RESET_ANY')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'ROLE_AUTH_ADMIN'
  AND p.name IN ('USER_READ_ANY', 'USER_EDIT_ANY', 'USER_PASSWORD_RESET_ANY', 'OAUTH_CLIENT_MANAGE')
ON CONFLICT DO NOTHING;
