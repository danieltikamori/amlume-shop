/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- Create database if it doesn't exist (safer during init)
CREATE DATABASE IF NOT EXISTS amlume_db;

-- Authorization server - specific user with minimal required privileges
-- Password MUST match the AUTH_DB_PASSWORD environment variable used by the application
-- Explicitly use caching_sha2_password authentication plugin
CREATE USER IF NOT EXISTS 'auth_server_user'@'%' IDENTIFIED WITH caching_sha2_password BY 'C0961F7D8F83574BEA49E9B412BBA60704CD720C'; -- <-- Added "WITH caching_sha2_password"

-- Grant only the necessary privileges to the auth_server_user on the literal database name 'amlume_db'.
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_registered_client TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_authorization TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_authorization_consent TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_authorization_code TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_access_token TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_refresh_token TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_client_credentials TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_device_authorization TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_device_code TO 'auth_server_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.oauth2_persistent_authorization_parameters TO 'auth_server_user'@'%';

-- If the Authorization Server also manages users directly:
-- GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.users TO 'auth_server_user'@'%';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON amlume_db.authorities TO 'auth_server_user'@'%';

-- Example: Create a read-only user (Commented out)
# CREATE USER IF NOT EXISTS 'readonly_user'@'%' IDENTIFIED WITH caching_sha2_password BY 'your_actual_readonly_password'; -- Also update here if used
# GRANT SELECT ON amlume_db.* TO 'readonly_user'@'%';

-- Add a dummy table to verify script completion (Optional)
CREATE TABLE IF NOT EXISTS amlume_db.init_script_marker (id INT PRIMARY KEY, completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
INSERT IGNORE INTO amlume_db.init_script_marker (id) VALUES (1);

-- IMPORTANT: Apply the changes
FLUSH PRIVILEGES;