# SummerBuild

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
Applies all pending migration scripts (e.g., `V1__...`, `V2__...`) to the database.

**When to use:**
- After adding a new Flyway migration SQL file
- When syncing your local DB schema with the latest version
- On application startup (if auto-run is configured in Spring Boot)

**Command:**
```bash
mvn flyway:migrate
