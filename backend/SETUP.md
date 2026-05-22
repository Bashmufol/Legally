# Backend setup

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 14+ (or `docker compose up -d postgres` from repo root)
- Firebase project with Anonymous Auth + Storage

## Database modes (Option A)

| Mode | Env | Use case |
|------|-----|----------|
| `local` | `DATABASE_MODE=local` | Docker Postgres (default) |
| `cloud-sql` | `DATABASE_MODE=cloud-sql` + `CLOUD_SQL_INSTANCE_CONNECTION_NAME` | GCP / Cloud SQL socket |
| `direct` | `DATABASE_MODE=direct` + `DATABASE_URL` | Cloud SQL Auth Proxy or public IP |

Full guide: [../docs/CLOUD_SQL_OPTION_A.md](../docs/CLOUD_SQL_OPTION_A.md)

## Run locally (Docker Postgres)

```powershell
# Start database
docker compose up -d postgres

cd backend
$env:DATABASE_MODE="local"
$env:GEMINI_API_KEY="your_key"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/legally"
$env:DATABASE_USERNAME="legally"
$env:DATABASE_PASSWORD="legally"
$env:FIREBASE_ENABLED="true"
$env:FIREBASE_CREDENTIALS_PATH="./firebase-service-account.json"
$env:FIREBASE_PROJECT_ID="your-project-id"
$env:FIREBASE_STORAGE_BUCKET="your-project.appspot.com"
$env:FIREBASE_REQUIRE_AUTH="true"

mvn spring-boot:run
```

## Docker (no local Maven)

```bash
docker compose up -d postgres
docker build -t legally-api ./backend
docker run -p 8080:8080 \
  -e GEMINI_API_KEY=xxx \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/legally \
  -e DATABASE_USERNAME=legally \
  -e DATABASE_PASSWORD=legally \
  -e FIREBASE_ENABLED=true \
  -e FIREBASE_REQUIRE_AUTH=true \
  legally-api
```

## API endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/consult` | Bearer | Legal consultation |
| POST | `/api/uploads` | Bearer | File upload |
| GET | `/api/uploads/files/{name}` | Public | Local file serve |
| GET | `/api/history/consultations` | Bearer | User history |
| POST | `/api/demand-letter` | Bearer | Demand letter |
| GET | `/api/contacts` | Bearer | Contact cards |
| GET | `/actuator/health` | Public | Health |

## Firebase + PostgreSQL on GCP

1. Link Cloud SQL PostgreSQL to your Firebase/GCP project
2. Set `DATABASE_URL` to the Cloud SQL socket/JDBC URL from the console
3. Deploy backend with service account that has Firebase Admin + Cloud SQL Client roles
