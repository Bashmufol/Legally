# Legally — Complete Configuration Guide

Step-by-step setup for local development and production deployment.

---

## What you are configuring

| Component | Purpose |
|-----------|---------|
| **Gemini API** | AI legal analysis and demand letters |
| **PostgreSQL** | Stores users, consultation history, uploads metadata |
| **Firebase Anonymous Auth** | Secure API access without login forms |
| **Firebase Storage** | Stores uploaded images, PDFs, audio, video |
| **Firebase Admin SDK** | Backend verifies tokens and writes to Storage |

---

## Part A — Prerequisites

Install these before you start:

1. **Java 21** — [Adoptium](https://adoptium.net/) or Oracle JDK 21  
   Verify: `java -version` (should show 21.x)

2. **Maven 3.9+** — [Apache Maven](https://maven.apache.org/download.cgi)  
   Verify: `mvn -version`  
   Alternative: use Docker to build/run the backend (see Part H).

3. **Node.js 20+** — [nodejs.org](https://nodejs.org/)  
   Verify: `node -v` and `npm -v`

4. **Docker Desktop** (recommended) — for local PostgreSQL  
   Verify: `docker compose version`

5. **Git** — to clone/push your repo

---

## Part B — Google Gemini API key

1. Open [Google AI Studio](https://aistudio.google.com/apikey).
2. Sign in with your Google account.
3. Click **Create API key**.
4. Choose an existing Google Cloud project or create a new one.
5. Copy the key (starts with `AIza...`).
6. Keep it secret — never commit it to GitHub.

You will use this as `GEMINI_API_KEY` on the backend.

---

## Part C — PostgreSQL (local, with Docker)

### C1. Start the database

Open a terminal in your project root (`Legally` folder — where `docker-compose.yml` lives):

```bash
docker compose up -d postgres
```

Wait until the container is healthy:

```bash
docker compose ps
```

You should see `legally-postgres` with status **running**.

### C2. Confirm connection details

| Setting | Value |
|---------|-------|
| Host | `localhost` |
| Port | `5432` |
| Database | `legally` |
| Username | `legally` |
| Password | `legally` |
| JDBC URL | `jdbc:postgresql://localhost:5432/legally` |

### C3. What happens on first backend run

Spring Boot + JPA will **automatically create tables**:

- `app_users`
- `consultations`
- `demand_letters`
- `media_uploads`

No manual SQL scripts are required for local dev.

### C4. Troubleshooting PostgreSQL

- **Port 5432 already in use** — stop other Postgres instances or change the port in `docker-compose.yml`.
- **Connection refused** — run `docker compose up -d postgres` again.
- **Wrong password** — use exactly `legally` / `legally` as in `docker-compose.yml`.

---

## Part D — Firebase project setup

### D1. Create a Firebase project

1. Go to [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or use an existing one).
3. Enter a name, e.g. `legally-hackathon`.
4. Disable Google Analytics if you do not need it (optional).
5. Click **Create project**.

Note your **Project ID** (e.g. `legally-hackathon`). You need it in several places.

### D2. Enable Anonymous Authentication

1. In Firebase Console, open your project.
2. Left menu → **Build** → **Authentication**.
3. Click **Get started** if prompted.
4. Open the **Sign-in method** tab.
5. Find **Anonymous** in the list.
6. Click it → toggle **Enable** → **Save**.

Do **not** enable Email/Password unless you want other sign-in types. The backend can reject non-anonymous users when `FIREBASE_ANONYMOUS_ONLY=true`.

### D3. Enable Firebase Storage

1. Left menu → **Build** → **Storage**.
2. Click **Get started**.
3. Choose **Start in test mode** for development (you can tighten rules later).
4. Pick a bucket location close to you (e.g. `europe-west1` or default).
5. Finish setup.

Note your **Storage bucket** name. It is often:

`your-project-id.appspot.com`

or

`your-project-id.firebasestorage.app`

Use the exact value shown in the Storage console.

### D4. Storage security rules (recommended for development)

In Storage → **Rules**, you can use test rules temporarily:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

Publish the rules. For production, restrict paths to `uploads/{userId}/**`.

### D5. Download Admin SDK credentials (backend)

1. Click the **gear icon** → **Project settings**.
2. Open the **Service accounts** tab.
3. Click **Generate new private key** → **Generate key**.
4. A JSON file downloads. Rename it to:

   `backend/firebase-service-account.json`

5. **Never commit this file to Git.** It is already in `.gitignore`.

### D6. Register a Web app (frontend)

1. Still in **Project settings** → **General**.
2. Under **Your apps**, click the **Web** icon (`</>`).
3. App nickname: `Legally Web`.
4. Do not enable Firebase Hosting unless you want it.
5. Click **Register app**.
6. Copy the `firebaseConfig` object values:

```javascript
const firebaseConfig = {
  apiKey: "...",
  authDomain: "...",
  projectId: "...",
  storageBucket: "...",
  messagingSenderId: "...",
  appId: "..."
};
```

You will map these to `VITE_FIREBASE_*` in the frontend `.env`.

### D7. Authorized domains (for local dev)

1. **Authentication** → **Settings** → **Authorized domains**.
2. Ensure `localhost` is listed (usually added by default).
3. When you deploy to Vercel, add your Vercel domain here too.

---

## Part E — Backend environment variables

### E1. Create backend config file

In the `backend` folder, copy the example:

**Windows PowerShell:**

```powershell
cd backend
Copy-Item .env.example .env
```

**Mac/Linux:**

```bash
cd backend
cp .env.example .env
```

For Spring Boot, set variables in the shell (see E2) or use your IDE run configuration. The `.env` file is a reference — Spring does not load `.env` automatically unless you use a plugin.

### E2. Set variables in PowerShell (Windows)

From `backend` folder:

```powershell
$env:GEMINI_API_KEY="AIzaSy...your_key"
$env:GEMINI_MODEL="gemini-2.5-flash"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/legally"
$env:DATABASE_USERNAME="legally"
$env:DATABASE_PASSWORD="legally"
$env:FIREBASE_ENABLED="true"
$env:FIREBASE_CREDENTIALS_PATH="./firebase-service-account.json"
$env:FIREBASE_PROJECT_ID="your-project-id"
$env:FIREBASE_STORAGE_BUCKET="your-project-id.appspot.com"
$env:FIREBASE_REQUIRE_AUTH="true"
$env:FIREBASE_ANONYMOUS_ONLY="true"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173"
$env:SERVER_PORT="8080"
```

Replace every `your-project-id` and bucket value with yours.

### E3. Set variables in Bash (Mac/Linux)

```bash
export GEMINI_API_KEY="AIzaSy...your_key"
export GEMINI_MODEL="gemini-2.5-flash"
export DATABASE_URL="jdbc:postgresql://localhost:5432/legally"
export DATABASE_USERNAME="legally"
export DATABASE_PASSWORD="legally"
export FIREBASE_ENABLED="true"
export FIREBASE_CREDENTIALS_PATH="./firebase-service-account.json"
export FIREBASE_PROJECT_ID="your-project-id"
export FIREBASE_STORAGE_BUCKET="your-project-id.appspot.com"
export FIREBASE_REQUIRE_AUTH="true"
export FIREBASE_ANONYMOUS_ONLY="true"
export CORS_ALLOWED_ORIGINS="http://localhost:5173"
export SERVER_PORT="8080"
```

### E4. Variable reference

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | Yes (for AI) | From Google AI Studio |
| `DATABASE_URL` | Yes | JDBC URL to PostgreSQL |
| `DATABASE_USERNAME` | Yes | DB user |
| `DATABASE_PASSWORD` | Yes | DB password |
| `FIREBASE_ENABLED` | Yes (full stack) | `true` to use Firebase Storage + token verify |
| `FIREBASE_CREDENTIALS_PATH` | Yes if enabled | Path to service account JSON |
| `FIREBASE_PROJECT_ID` | Yes if enabled | Firebase project ID |
| `FIREBASE_STORAGE_BUCKET` | Yes if enabled | Storage bucket name |
| `FIREBASE_REQUIRE_AUTH` | Recommended | `true` = API requires Bearer token |
| `FIREBASE_ANONYMOUS_ONLY` | Recommended | `true` = reject Google/email sign-in tokens |
| `CORS_ALLOWED_ORIGINS` | Yes | Frontend URL, comma-separated |

### E5. Local dev without Firebase (optional)

If Firebase is not ready yet:

```powershell
$env:FIREBASE_ENABLED="false"
$env:FIREBASE_REQUIRE_AUTH="false"
```

- API works as **guest** (no saved history in DB for guest UID).
- Uploads go to `backend/uploads/` folder.

### E6. Start the backend

```powershell
cd backend
mvn spring-boot:run
```

Success signs:

- Log: `Firebase Admin SDK initialized`
- Log: Hibernate creates tables (first run)
- http://localhost:8080/actuator/health returns `{"status":"UP"}`

---

## Part F — Frontend environment variables

### F1. Create frontend `.env`

```powershell
cd frontend
Copy-Item .env.example .env
```

### F2. Fill in `.env`

```env
VITE_API_URL=http://localhost:8080

VITE_FIREBASE_API_KEY=AIzaSy...
VITE_FIREBASE_AUTH_DOMAIN=your-project-id.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=your-project-id
VITE_FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=123456789012
VITE_FIREBASE_APP_ID=1:123456789012:web:abcdef
```

Map each value from Firebase Console → Project settings → Your apps → Web app config.

### F3. Install and run

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

### F4. Verify anonymous auth

1. Open browser **DevTools** → **Network**.
2. Go to **Consult** page.
3. You should see a request to Firebase Auth (anonymous sign-in).
4. When you submit a consultation, the request to `http://localhost:8080/api/consult` should include:

   `Authorization: Bearer eyJ...`

5. Backend saves history under your anonymous Firebase UID in PostgreSQL.

---

## Part G — End-to-end local test checklist

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | `docker compose up -d postgres` | Postgres running |
| 2 | Start backend with all env vars | Health UP |
| 3 | `npm run dev` in frontend | App loads |
| 4 | Open `/consult`, submit text | JSON response with summary, law, steps |
| 5 | Refresh page, check history | Recent consultation listed |
| 6 | Upload an image | Upload succeeds, analysis includes media |
| 7 | Tenancy scenario → demand letter | Letter generates |

---

## Part H — Production deployment (overview)

### H1. PostgreSQL via Firebase SQL Connect (Option A — implemented in Legally)

If you already ran **Set up SQL Connect** in Firebase:

1. Wait until Cloud SQL instance status is **Ready** (up to ~20 minutes).
2. Copy **Connection name** from Cloud Console → SQL → `legally-7f34d-instance`  
   Format: `your-project-id:us-east4:legally-7f34d-instance`
3. Set backend env (see [CLOUD_SQL_OPTION_A.md](./CLOUD_SQL_OPTION_A.md)):

   ```bash
   DATABASE_MODE=cloud-sql
   CLOUD_SQL_INSTANCE_CONNECTION_NAME=your-project-id:us-east4:legally-7f34d-instance
   CLOUD_SQL_DATABASE_NAME=legally-7f34d-database
   SPRING_PROFILES_ACTIVE=cloudsql
   ```

4. Legally uses **JPA + JDBC** — you do **not** need SQL Connect GraphQL in the frontend.

For local testing against that same Cloud SQL database, use **Cloud SQL Auth Proxy** + `DATABASE_MODE=direct` (documented in CLOUD_SQL_OPTION_A.md).

### H1b. Manual Cloud SQL (without SQL Connect wizard)

1. In [Google Cloud Console](https://console.cloud.google.com/), select the **same project** as Firebase.
2. **SQL** → **Create instance** → **PostgreSQL**.
3. Set password, region, and machine type (smallest tier is fine for hackathon).
4. Create database and user; use `DATABASE_MODE=cloud-sql` or `direct` as above.

### H2. Backend on Render

1. Push code to GitHub.
2. Render → **New Web Service** → connect repo.
3. Use `backend/Dockerfile` (see `render.yaml`).
4. Add environment variables from Part E (use production DB URL).
5. Upload Firebase JSON as a **Secret File** mounted at `/etc/secrets/firebase.json`.
6. Set `FIREBASE_CREDENTIALS_PATH=/etc/secrets/firebase.json`.
7. Set `CORS_ALLOWED_ORIGINS=https://your-app.vercel.app`.
8. Set `FIREBASE_REQUIRE_AUTH=true`.

### H3. Frontend on Vercel

1. Import repo on [Vercel](https://vercel.com).
2. Set **Root Directory** to `frontend`.
3. Add all `VITE_*` variables from Part F.
4. Set `VITE_API_URL` to your Render backend URL.
5. Deploy.
6. Add Vercel domain to Firebase **Authorized domains**.

---

## Part I — Common errors and fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `401 Unauthorized` on API | Missing/invalid Firebase token | Enable Anonymous auth; check frontend `.env`; open Consult page first |
| `403 Only anonymous sign-in` | Signed in with Google/email | Sign out; use **New session** or clear site data |
| `Connection refused` to DB | Postgres not running | `docker compose up -d postgres` |
| `Firebase Admin SDK disabled` | `FIREBASE_ENABLED=false` or bad JSON path | Set `true` and correct path to service account file |
| CORS error in browser | Backend CORS mismatch | Set `CORS_ALLOWED_ORIGINS` to exact frontend URL (no trailing slash) |
| Gemini errors | Invalid/missing API key | Check `GEMINI_API_KEY` in backend env |
| History empty | Guest mode or wrong user | Use Firebase enabled + `FIREBASE_REQUIRE_AUTH=true` + anonymous sign-in |

---

## Part J — Security checklist before hackathon submit

- [ ] `firebase-service-account.json` is **not** in Git
- [ ] `FIREBASE_REQUIRE_AUTH=true` on production backend
- [ ] `FIREBASE_ANONYMOUS_ONLY=true`
- [ ] Firebase Storage rules require `request.auth != null`
- [ ] CORS only allows your Vercel domain
- [ ] PostgreSQL uses a strong password in production
- [ ] Gemini API key only on server (never in frontend)

---

## Quick reference — file locations

```
Legally/
├── docker-compose.yml          # Local Postgres
├── backend/
│   ├── .env.example
│   ├── firebase-service-account.json   # YOU add this (gitignored)
│   └── src/main/resources/application.yml
└── frontend/
    └── .env                    # YOU create from .env.example
```

For more deployment detail, see [DEPLOY.md](./DEPLOY.md).
