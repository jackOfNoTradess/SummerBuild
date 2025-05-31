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
