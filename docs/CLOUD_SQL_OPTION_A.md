# Option A — Spring Boot + Firebase SQL Connect Cloud SQL

Legally uses **direct JDBC** to the PostgreSQL database on the Cloud SQL instance that Firebase SQL Connect creates. It does **not** use the SQL Connect GraphQL SDK.

```
Firebase SQL Connect  →  provisions  →  Cloud SQL (PostgreSQL)
                                              ↑
Spring Boot (JPA)  ─────── JDBC ──────────────┘
Firebase Auth / Storage  ─── separate (unchanged)
```

## Your Firebase SQL Connect resources (example)

| Resource | Example ID |
|----------|------------|
| Cloud SQL instance | `legally-7f34d-instance` |
| Database | `legally-7f34d-database` |
| Region | `us-east4` |
| Connection name | `{PROJECT_ID}:us-east4:legally-7f34d-instance` |

Find **Connection name** in [Google Cloud Console](https://console.cloud.google.com/sql) → your instance → **Overview**.

## Three database modes

| `DATABASE_MODE` | When to use |
|-----------------|-------------|
| `local` | Docker Postgres (`docker compose up -d postgres`) |
| `cloud-sql` | Backend runs on GCP (Cloud Run, GCE) with socket factory |
| `direct` | Custom `DATABASE_URL` — public IP or **Cloud SQL Auth Proxy** on laptop |

## After SQL Connect finishes provisioning

### 1. Get database password

1. Cloud Console → **SQL** → `legally-7f34d-instance`
2. **Users** → set password for `postgres` (or the user Firebase created)
3. Save the password securely

### 2a. Production / GCP hosting (`cloud-sql`)

Set environment variables:

```bash
DATABASE_MODE=cloud-sql
CLOUD_SQL_INSTANCE_CONNECTION_NAME=your-project-id:us-east4:legally-7f34d-instance
CLOUD_SQL_DATABASE_NAME=legally-7f34d-database
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
SPRING_PROFILES_ACTIVE=cloudsql
```

Start backend:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=cloudsql
```

The service account (Firebase JSON or GCP default credentials) needs **Cloud SQL Client** role.

### 2b. Local dev against Cloud SQL (`direct` + proxy)

Terminal 1 — proxy:

```powershell
.\scripts\cloud-sql-proxy.ps1 -ConnectionName "your-project-id:us-east4:legally-7f34d-instance"
```

Terminal 2 — backend:

```powershell
$env:DATABASE_MODE="direct"
$env:DATABASE_URL="jdbc:postgresql://127.0.0.1:5432/legally-7f34d-database"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="your_password"
$env:FIREBASE_ENABLED="true"
# ... other Firebase / Gemini vars
mvn spring-boot:run
```

### 2c. Keep using local Docker (`local`)

No Cloud SQL needed for dev:

```powershell
docker compose up -d postgres
$env:DATABASE_MODE="local"
mvn spring-boot:run
```

## Verify connection

1. Backend log: `PostgreSQL: Cloud SQL socket mode` or `Database mode: direct JDBC`
2. http://localhost:8080/actuator/health → `UP`
3. Submit a consultation → row appears in Cloud SQL (check via Cloud Console → SQL → Databases → Query)

## Render deployment

In `render.yaml` / dashboard, set:

- `DATABASE_MODE=cloud-sql`
- `CLOUD_SQL_INSTANCE_CONNECTION_NAME`
- `CLOUD_SQL_DATABASE_NAME=legally-7f34d-database`
- `SPRING_PROFILES_ACTIVE=cloudsql`
- Mount Firebase service account JSON

Render must run in GCP or use a host that supports Cloud SQL sockets; otherwise use **direct** mode with a publicly reachable IP and authorized networks.

## What you do NOT need

- SQL Connect GraphQL schema in your React app
- `firebase deploy --only dataconnect`
- Changing Legally to use SQL Connect SDK

SQL Connect only **created** the database; Legally talks to it with normal PostgreSQL + JPA.
