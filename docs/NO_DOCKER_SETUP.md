# Run Legally without Docker

You only need: **Java 21**, **Maven/IntelliJ**, **Postman**, and **PostgreSQL** (one of the options below).

---

## Option 1 — Use your Firebase Cloud SQL (no Docker)

You already provisioned Cloud SQL via Firebase SQL Connect. Use the **Auth Proxy** (a small `.exe`, not Docker).

### Step 1: Install Google Cloud CLI (if needed)

https://cloud.google.com/sdk/docs/install

```powershell
gcloud auth login
gcloud auth application-default login
gcloud config set project legally-7f34d
```

### Step 2: Download Cloud SQL Auth Proxy

https://cloud.google.com/sql/docs/postgres/sql-proxy#install

Save as e.g. `C:\tools\cloud-sql-proxy.exe`

### Step 3: Start the proxy (keep this window open)

```powershell
C:\tools\cloud-sql-proxy.exe legally-7f34d:us-east4:legally-7f34d-instance --port 5432
```

### Step 4: IntelliJ run configuration

| Setting | Value |
|---------|--------|
| **Active profiles** | `direct` |
| **Working directory** | `backend` |

**Environment variables:**

```text
GEMINI_API_KEY=your_key
DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/legally-7f34d-database
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_cloud_sql_password
FIREBASE_ENABLED=true
FIREBASE_CREDENTIALS_PATH=./firebase-service-account.json
FIREBASE_PROJECT_ID=legally-7f34d
FIREBASE_STORAGE_BUCKET=legally-7f34d.firebasestorage.app
FIREBASE_REQUIRE_AUTH=false
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Run `LegallyApplication`. Test: `GET http://localhost:8080/actuator/health`

---

## Option 2 — Install PostgreSQL on Windows (simplest long-term)

### Step 1: Install

https://www.postgresql.org/download/windows/

During setup, note the password you set for user `postgres`.

### Step 2: Create database

Open **pgAdmin** or `psql`:

```sql
CREATE DATABASE legally;
-- optional dedicated user:
CREATE USER legally WITH PASSWORD 'legally';
GRANT ALL PRIVILEGES ON DATABASE legally TO legally;
```

### Step 3: IntelliJ

| Setting | Value |
|---------|--------|
| **Active profiles** | `local` |

**Environment variables:**

```text
GEMINI_API_KEY=your_key
DATABASE_URL=jdbc:postgresql://localhost:5432/legally
DATABASE_USERNAME=legally
DATABASE_PASSWORD=legally
FIREBASE_ENABLED=true
FIREBASE_CREDENTIALS_PATH=./firebase-service-account.json
FIREBASE_PROJECT_ID=legally-7f34d
FIREBASE_STORAGE_BUCKET=legally-7f34d.firebasestorage.app
FIREBASE_REQUIRE_AUTH=false
```

If you only have user `postgres`:

```text
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=the_password_you_chose_at_install
```

---

## Postman (same for both options)

1. `GET http://localhost:8080/actuator/health`
2. `POST http://localhost:8080/api/consult` with JSON body (no auth if `FIREBASE_REQUIRE_AUTH=false`)

---

## Profiles summary

| Profile | When to use |
|---------|-------------|
| `local` | Postgres on `localhost:5432` (Windows install) |
| `direct` | Cloud SQL Auth Proxy on `127.0.0.1:5432` |
| `cloudsql` | Cloud Run / GCP only (socket factory) |

**Avoid** profile `cloudsql` in IntelliJ unless you are on GCP.
