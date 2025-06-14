# SummerBuild

[![Java CI with Maven](https://github.com/[YOUR_USERNAME]/SummerBuild/actions/workflows/ci.yml/badge.svg)](https://github.com/[YOUR_USERNAME]/SummerBuild/actions/workflows/ci.yml)

## api docs
http://localhost:8080/swagger-ui/index.html#/

## Flyway CLI Commands
```bash
mvn flyway:migrate    # Run migrations
mvn flyway:info       # Show migration status
mvn flyway:clean      # Drop all objects
```

## 🛠 Flyway Migration Usage Guide
### 🚀 `mvn flyway:migrate`

**Purpose:**  
Applies all pending migration scripts to the database.

**When to use:**
- After adding a new migration SQL file
- When syncing your local DB schema with the latest version
- On application startup (if auto-run is configured in Spring Boot)

**Command:**
```bash
mvn flyway:migrate

```

### ⚠️ `mvn flyway:clean` — Use with Caution

**Purpose:**  
This command **drops all database objects** (tables, views, constraints, etc.) in the configured schema. It resets the entire schema.

---

**🕵️‍♂️ When to Use**

Only use `flyway:clean` in **development or testing environments**:

- To **wipe** the database and start from scratch
- When schema changes or migrations are broken and unrecoverable
- Before re-running a full migration set for a clean slate


## 🐘 Supabase Local Development Migration Guide
### 📝 Creating Migrations
1. **Create a new migration:**
```bash
supabase migration new <migration_name>
```


2. **Edit the generated SQL file in `supabase/migrations/`:**
- Do this only to amend bucket policies. ALL postgres migrations use flyway
```sql
-- Example: 20240606000001_create_users_table.sql
CREATE TABLE users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 🔄 Running Migrations

1. **Apply migrations to local database:**
```bash
supabase db push --local
```

2. **Reseting Supabase**
```bash
supabase reset
```
