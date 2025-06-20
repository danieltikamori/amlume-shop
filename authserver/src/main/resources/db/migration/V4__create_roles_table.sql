/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- Create roles table with LTREE path support and audit columns
CREATE TABLE roles
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100)                                       NOT NULL UNIQUE,
    description      VARCHAR(255),                                                -- Made nullable, adjust if description is mandatory in your Role entity
    path             LTREE                                              NOT NULL,
    parent_id        BIGINT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Ensure NOT NULL if AbstractAuditableEntity enforces it
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Ensure NOT NULL
    created_by       BIGINT,                                                      -- ADDED: For AbstractAuditableEntity
    last_modified_by BIGINT,                                                      -- ADDED: For AbstractAuditableEntity
    FOREIGN KEY (parent_id) REFERENCES roles (id) ON DELETE SET NULL              -- Or ON DELETE CASCADE
);

-- Create index on path for faster hierarchical queries
CREATE INDEX roles_path_idx ON roles USING GIST (path);

-- Create index for parent-child relationship queries
CREATE INDEX roles_parent_id_idx ON roles (parent_id);

-- Create index on name for lookups (if not already covered by UNIQUE constraint effectively)
CREATE INDEX roles_name_idx ON roles (name);

-- Insert root roles (Data seeding is often better in a separate V4.1 script)
-- Ensure LTREE paths and parent_id are set correctly for hierarchy if seeding here.
-- For simplicity, assuming parent_id will be updated later or these are all top-level for now.
-- If these are truly hierarchical, the INSERT statements need to reflect that.
INSERT INTO roles (name, description, path, parent_id, created_by, last_modified_by)
VALUES ('ROLE_ROOT', 'Root administrator with all privileges', 'ROLE_ROOT', NULL, NULL, NULL)
ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description, path, parent_id, created_by, last_modified_by)
VALUES ('ROLE_SUPER_ADMIN', 'Super System administrator', 'ROLE_SUPER_ADMIN', NULL, NULL, NULL)
ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description, path, parent_id, created_by, last_modified_by)
VALUES ('ROLE_ADMIN', 'System administrator', 'ROLE_ADMIN', NULL, NULL, NULL)
ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description, path, parent_id, created_by, last_modified_by)
VALUES ('ROLE_MANAGER', 'System manager', 'ROLE_MANAGER', NULL, NULL, NULL)
ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description, path, parent_id, created_by, last_modified_by)
VALUES ('ROLE_USER', 'Standard user', 'ROLE_USER', NULL, NULL, NULL)
ON CONFLICT (name) DO NOTHING;

-- Note: If your AbstractAuditableEntity uses String for createdBy/lastModifiedBy (e.g., username),
-- change BIGINT to VARCHAR(255) for those columns.
-- The seed data for created_by/last_modified_by is NULL here; your application's
-- AuditorAware implementation would populate these for new entities created via JPA.
-- For Flyway-seeded data, these might remain NULL or be set to a system user ID.
