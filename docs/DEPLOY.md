# Deployment guide

## 1. Database (PostgreSQL)

Use one of:

- **Local / Render Postgres**: set `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- **Firebase + Cloud SQL**: create PostgreSQL in the same GCP project as Firebase; use the JDBC URL from Cloud SQL console

```bash
docker compose up -d postgres   # local only
```

## 2. Firebase

1. Enable **Anonymous** authentication
2. Enable **Storage**
3. Download Admin SDK JSON for the backend
4. Add Web app config to Vercel env vars (`VITE_FIREBASE_*`)

## 3. Backend

Environment variables:

| Variable | Example |
|----------|---------|
| `GEMINI_API_KEY` | AI Studio key |
| `DATABASE_URL` | `jdbc:postgresql://...` |
| `DATABASE_USERNAME` | `legally` |
| `DATABASE_PASSWORD` | secret |
| `FIREBASE_ENABLED` | `true` |
| `FIREBASE_CREDENTIALS_PATH` | mount JSON secret |
| `FIREBASE_PROJECT_ID` | your-project-id |
| `FIREBASE_STORAGE_BUCKET` | your-project.appspot.com |
| `FIREBASE_REQUIRE_AUTH` | `true` |
| `FIREBASE_ANONYMOUS_ONLY` | `true` |
| `CORS_ALLOWED_ORIGINS` | `https://your-app.vercel.app` |

## 4. Frontend (Vercel)

- Root: `frontend`
- `VITE_API_URL` → backend URL
- All `VITE_FIREBASE_*` from Firebase web config

## 5. Production checklist

- [ ] Anonymous auth enabled (no email/password providers required)
- [ ] `FIREBASE_REQUIRE_AUTH=true` on backend
- [ ] PostgreSQL reachable from backend host
- [ ] CORS allows frontend origin only
