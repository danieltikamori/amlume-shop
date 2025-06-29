# Hybrid Database Approach for Auth Server

This document explains the hybrid database approach used in the Amlume Auth Server.

## Overview

The auth server uses three different databases, each optimized for specific purposes:

1. **PostgreSQL** - For user data and hierarchical role management
2. **MongoDB** - For session storage
3. **Redis** - For caching

## PostgreSQL with LTREE

PostgreSQL is used for storing user data and role hierarchies. The LTREE extension provides efficient hierarchical data
storage and querying capabilities.

### Role Hierarchy Example

```
role_root
├── role_admin
│   ├── role_manager
│   └── role_support
└── role_user
    ├── role_premium
    └── role_basic
```

### LTREE Queries

- Find all descendants: `SELECT * FROM roles WHERE path <@ 'role_admin'`
- Find all ancestors: `SELECT * FROM roles WHERE path @> 'role_admin.role_manager'`
- Find roles at specific level: `SELECT * FROM roles WHERE nlevel(path) = 2`

## MongoDB for Session Storage

MongoDB is used for session storage to avoid serialization issues with complex objects like WebAuthn credentials.

Benefits:

- Native JSON document storage
- No need for complex serialization/deserialization
- Better handling of nested objects
- Schema flexibility

## Redis for Caching

Redis is still used, but only for caching purposes:

- API response caching
- Rate limiting
- Temporary data storage

## Setup Instructions

1. Start the databases:
   ```
   docker-compose up -d
   ```

2. Verify PostgreSQL LTREE extension:
   ```
   docker exec -it amlume-postgres psql -U amlume_user -d amlume_auth_db -c "SELECT 'role_admin.role_manager'::ltree <@ 'role_admin'::ltree;"
   ```

3. Run Flyway migrations:
   ```
   ./mvnw flyway:migrate
   ```

4. Start the application:
   ```
   ./mvnw spring-boot:run
   ```

## Configuration

The database configuration is in `application.yml`:

```yaml
spring:
  # MongoDB for sessions
  data:
    mongodb:
      uri: mongodb://localhost:27017/amlume_sessions
  
  # Redis for caching
  cache:
    type: redis
  
  # PostgreSQL for user data and roles
  datasource:
    url: jdbc:postgresql://localhost:5432/amlume_auth_db
```
