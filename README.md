# Legally — AI-Powered Legal Advisor (Nigeria)

GDG CareerFest 2026 · **SDG 16** (Peace, Justice, and Strong Institutions)

Legally helps Nigerians understand their rights using **Google Gemini API**, **PostgreSQL on Cloud SQL** (provisioned via Firebase SQL Connect — **Option A: direct JDBC**), **Firebase Anonymous Auth**, and **Firebase Storage**.

## Stack

| Layer | Technology |
|-------|------------|
| Backend | **Java 21**, **Spring Boot 4.0.6** |
| Database | **PostgreSQL** — local Docker or **Cloud SQL** from Firebase SQL Connect ([Option A guide](docs/CLOUD_SQL_OPTION_A.md)) |
| Auth | **Firebase Anonymous Auth** (no signup/login UI) |
| Files | **Firebase Storage** (or local `uploads/` in dev) |
| AI | **Google Gemini API** |
| Frontend | React, TypeScript, Tailwind, Vite |

## Quick start

### 1. PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Firebase project

1. [Firebase Console](https://console.firebase.google.com/) → create project (same GCP project as Cloud SQL if used)
2. **Authentication** → Sign-in method → enable **Anonymous**
3. **Storage** → create default bucket
4. **Project settings** → Service accounts → generate **Admin SDK** JSON → save as `backend/firebase-service-account.json`
5. **Project settings** → Your apps → Web app → copy config into `frontend/.env`

If you used **Firebase SQL Connect**, set `DATABASE_MODE=cloud-sql` and `CLOUD_SQL_INSTANCE_CONNECTION_NAME` — see [docs/CLOUD_SQL_OPTION_A.md](docs/CLOUD_SQL_OPTION_A.md). Local dev can stay on Docker (`DATABASE_MODE=local`).

### 3. Backend

```powershell
cd backend
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

API: http://localhost:8080

### 4. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

App: http://localhost:5173 — signs in anonymously on first visit when Firebase is configured.

## Security model

- Users never see a login form; the app calls `signInAnonymously()` automatically.
- Each API request sends `Authorization: Bearer <Firebase ID token>`.
- The backend verifies tokens with the Firebase Admin SDK and **rejects non-anonymous** providers when `FIREBASE_ANONYMOUS_ONLY=true`.
- Consultation history and uploads are scoped to the Firebase UID in PostgreSQL.

## Local dev without Firebase

```powershell
$env:FIREBASE_ENABLED="false"
$env:FIREBASE_REQUIRE_AUTH="false"
```

Uses guest principal (no persisted history). Uploads go to `backend/uploads/`.

## Cloud SQL (Firebase SQL Connect)

See **[docs/CLOUD_SQL_OPTION_A.md](docs/CLOUD_SQL_OPTION_A.md)** for connecting to `legally-7f34d-instance` / `legally-7f34d-database`.

## Environment variables

See [backend/.env.example](backend/.env.example), [backend/.env.cloudsql.example](backend/.env.cloudsql.example), and [frontend/.env.example](frontend/.env.example).

## PostgreSQL tables (auto-created via JPA)

| Table | Purpose |
|-------|---------|
| `app_users` | Anonymous Firebase users |
| `consultations` | Consultation history |
| `demand_letters` | Generated letters |
| `media_uploads` | Upload metadata |

Legal corpus and contacts remain in bundled JSON (`backend/src/main/resources/`).

## Google tools (hackathon)

- **Gemini API** — legal analysis & demand letters
- **Firebase Auth** — anonymous sessions
- **Firebase Storage** — evidence uploads
- **PostgreSQL** — persistent data in your Firebase/GCP project

## Disclaimer

Legally provides general legal information only, not legal advice.
