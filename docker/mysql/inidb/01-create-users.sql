/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- Get the database name from the environment variable (passed by Docker Compose)
SET @DATABASE_NAME = IFNULL(getenv('MYSQL_DATABASE'), 'amlume_db');

-- Authorization server - specific user with minimal required privileges
-- The password for this user should be managed through Docker environment variables, NOT hardcoded here.
CREATE USER IF NOT EXISTS 'auth_server_user'@'%' IDENTIFIED BY getenv('AUTH_SERVER_DB_PASSWORD');

-- Grant only the necessary privileges to the auth_server_user on the specified database.
-- We need to identify the specific tables the authorization server interacts with.
-- Assuming the Spring Authorization Server uses the default schema names:

GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_registered_client TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_authorization TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_authorization_consent TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_authorization_code TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_access_token TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_refresh_token TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_client_credentials TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_device_authorization TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_device_code TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.oauth2_persistent_authorization_parameters TO 'auth_server_user'@'%';

-- If the Authorization Server also manages users directly:
-- GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.users TO 'auth_server_user'@'%';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON @DATABASE_NAME.authorities TO 'auth_server_user'@'%';

-- Example: Create a read-only user (password from env var)
CREATE USER IF NOT EXISTS 'readonly_user'@'%' IDENTIFIED BY getenv('READONLY_DB_PASSWORD');
    GRANT SELECT ON @DATABASE_NAME.* TO 'readonly_user'@'%';

-- IMPORTANT: Apply the changes
FLUSH PRIVILEGES;