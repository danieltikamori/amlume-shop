/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- create_core_tables.sql
-- Create the users table first as persistent_logins might logically relate to it
CREATE TABLE users
(
    user_id                 BIGSERIAL PRIMARY KEY,
    external_id             VARCHAR(255) UNIQUE,
    first_name              VARCHAR(127)             NOT NULL,
    last_name_encrypted     BYTEA,
    nickname                VARCHAR(127) UNIQUE,
    email                   VARCHAR(255)             NOT NULL UNIQUE,
    backup_email_encrypted  BYTEA,
    profile_picture_url     VARCHAR(2046),
    mobile_number_encrypted BYTEA,        -- Corresponds to encrypted PhoneNumber.e164Value
    password                VARCHAR(127), -- Corresponds to HashedPassword.value
    enabled                 BOOLEAN                  NOT NULL,
    failed_login_attempts   INTEGER,
    lockout_expiration_time TIMESTAMP WITH TIME ZONE,
    account_non_expired     BOOLEAN                  NOT NULL,
    credentials_non_expired BOOLEAN                  NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by              BIGINT,
    last_modified_by        BIGINT,
    last_login_at           TIMESTAMP WITH TIME ZONE,
    deleted_at              TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

-- Indexes for users table
CREATE INDEX idx_email ON users (email);
CREATE INDEX idx_backup_email ON users (backup_email);
CREATE INDEX idx_nickname ON users (nickname);
CREATE INDEX idx_mobile_number ON users (mobile_number);
CREATE INDEX idx_created_at ON users (created_at);
CREATE INDEX idx_updated_at ON users (updated_at);
CREATE INDEX idx_external_id ON users (external_id);
CREATE INDEX idx_users_deleted_at ON users (deleted_at);

-- Add an index on the username column to improve performance of delete operations
-- (e.g., when a user logs out and their remember-me tokens are cleared).
CREATE INDEX idx_persistent_logins_username ON persistent_logins (username);

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- Optional: Add foreign key constraints if 'createdBy' and 'lastModifiedBy' reference user_id
ALTER TABLE users
    ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);
ALTER TABLE users
    ADD CONSTRAINT fk_users_modified_by FOREIGN KEY (last_modified_by) REFERENCES users (user_id);


-- Note: The 'authorities' table and the 'user_authorities' join table
-- (defined by the @ManyToMany relationship in your User entity)
-- will also need to be created. You can add them here or in a subsequent
-- migration script (e.g., V2__create_authorities_tables.sql).
-- It's generally good to create related tables together if possible.
-- For example:

-- CREATE TABLE authorities (
--     id BIGSERIAL PRIMARY KEY,
--     authority VARCHAR(255) NOT NULL UNIQUE
-- );

-- CREATE TABLE user_authorities (
--     user_id BIGINT NOT NULL,
--     authority_id BIGINT NOT NULL,
--     PRIMARY KEY (user_id, authority_id),
--     FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
--     FOREIGN KEY (authority_id) REFERENCES authorities (id) ON DELETE CASCADE
-- );
