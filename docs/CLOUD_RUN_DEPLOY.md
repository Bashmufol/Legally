# Deploy Legally backend to Google Cloud Run

This guide matches your stack: **Java 21**, **Spring Boot 4**, **Firebase SQL Connect (Cloud SQL)**, **Firebase Auth/Storage**.

---

## What you need

- Google Cloud project: `legally-7f34d` (or yours)
- [gcloud CLI](https://cloud.google.com/sdk/docs/install) installed and logged in
- Billing enabled on the project
- Files on your machine:
  - `backend/firebase-service-account.json` (Firebase Admin SDK key)
  - Cloud SQL instance already provisioned (Firebase SQL Connect)

---

## Step 1 — Install and login

```powershell
gcloud auth login
gcloud config set project legally-7f34d
```

---

## Step 2 — Enable required APIs

```powershell
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com sqladmin.googleapis.com secretmanager.googleapis.com
```

---

## Step 3 — Create Artifact Registry (one-time)

```powershell
gcloud artifacts repositories create legally-repo --repository-format=docker --location=us-east4
```

---

## Step 4 — Store secrets in Secret Manager

Never put passwords in plain `gcloud run deploy` flags. Use secrets:

### Firebase service account JSON

```powershell
gcloud secrets create firebase-admin-key --data-file=backend/firebase-service-account.json
```

### Gemini API key

```powershell
# PowerShell: put key in a temp file first, then:
"YOUR_GEMINI_API_KEY" | Out-File -Encoding utf8 gemini-key.txt
gcloud secrets create gemini-api-key --data-file=gemini-key.txt
Remove-Item gemini-key.txt
```

### Database password

```powershell
"YOUR_DB_PASSWORD" | Out-File -Encoding utf8 db-pass.txt
gcloud secrets create cloudsql-password --data-file=db-pass.txt
Remove-Item db-pass.txt
```

Grant Cloud Run’s service account access to read secrets (after first deploy, or use default compute SA):

```powershell
$PROJECT_NUMBER = gcloud projects describe legally-7f34d --format="value(projectNumber)"
gcloud secrets add-iam-policy-binding firebase-admin-key --member="serviceAccount:$PROJECT_NUMBER-compute@developer.gserviceaccount.com" --role="roles/secretmanager.secretAccessor"
gcloud secrets add-iam-policy-binding gemini-api-key --member="serviceAccount:$PROJECT_NUMBER-compute@developer.gserviceaccount.com" --role="roles/secretmanager.secretAccessor"
gcloud secrets add-iam-policy-binding cloudsql-password --member="serviceAccount:$PROJECT_NUMBER-compute@developer.gserviceaccount.com" --role="roles/secretmanager.secretAccessor"
```

---

## Step 5 — Build and push the Docker image

From the **repository root**:

```powershell
cd backend
gcloud builds submit --tag us-east4-docker.pkg.dev/legally-7f34d/legally-repo/legally-api:latest
```

---

## Step 6 — Deploy to Cloud Run

Replace `YOUR_VERCEL_URL` with `https://your-app.vercel.app` (no trailing slash).

```powershell
gcloud run deploy legally-api `
  --image us-east4-docker.pkg.dev/legally-7f34d/legally-repo/legally-api:latest `
  --region us-east4 `
  --platform managed `
  --allow-unauthenticated `
  --port 8080 `
  --memory 1Gi `
  --cpu 1 `
  --min-instances 0 `
  --max-instances 3 `
  --add-cloudsql-instances legally-7f34d:us-east4:legally-7f34d-instance `
  --set-env-vars "SPRING_PROFILES_ACTIVE=cloudsql,DATABASE_MODE=cloud-sql,CLOUD_SQL_INSTANCE_CONNECTION_NAME=legally-7f34d:us-east4:legally-7f34d-instance,CLOUD_SQL_DATABASE_NAME=legally-7f34d-database,DATABASE_USERNAME=postgres,GEMINI_MODEL=gemini-2.5-flash,FIREBASE_ENABLED=true,FIREBASE_PROJECT_ID=legally-7f34d,FIREBASE_STORAGE_BUCKET=legally-7f34d.firebasestorage.app,FIREBASE_CREDENTIALS_PATH=/secrets/firebase/firebase-admin-key,FIREBASE_REQUIRE_AUTH=true,FIREBASE_ANONYMOUS_ONLY=true,CORS_ALLOWED_ORIGINS=https://YOUR_VERCEL_URL,http://localhost:5173" `
  --set-secrets "/secrets/firebase/firebase-admin-key=firebase-admin-key:latest,GEMINI_API_KEY=gemini-api-key:latest,DATABASE_PASSWORD=cloudsql-password:latest"
```

**Note:** Bash users use `\` instead of `` ` `` for line continuation.

---

## Step 7 — Grant Cloud SQL access to Cloud Run

The Cloud Run service account must connect to Cloud SQL:

```powershell
gcloud projects add-iam-policy-binding legally-7f34d `
  --member="serviceAccount:$PROJECT_NUMBER-compute@developer.gserviceaccount.com" `
  --role="roles/cloudsql.client"
```

(Use the same `$PROJECT_NUMBER` as above.)

---

## Step 8 — Verify deployment

After deploy, gcloud prints a URL like:

`https://legally-api-xxxxx-ue.a.run.app`

Test:

```powershell
curl https://legally-api-xxxxx-ue.a.run.app/actuator/health
```

Expected: `{"status":"UP"}`

---

## Step 9 — Connect Vercel frontend

1. Vercel → Project → **Environment Variables**
2. Set `VITE_API_URL=https://legally-api-xxxxx-ue.a.run.app`
3. **Redeploy** the frontend
4. Firebase Console → **Authentication** → **Authorized domains** → add your Vercel domain

---

## Console UI alternative (no CLI)

1. [Cloud Console](https://console.cloud.google.com/) → **Cloud Run** → **Create service**
2. **Deploy one revision from an existing container image** (after Step 5 build)
3. Region: `us-east4` (same as Cloud SQL)
4. **Connections** → **Cloud SQL** → select `legally-7f34d-instance`
5. **Variables & secrets** → add env vars from Step 6
6. **Security** → allow unauthenticated invocations (public API for your web app)
7. **Container port:** `8080`

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Container failed to start | Check **Logs** in Cloud Run; often missing env or wrong secret path |
| Cloud SQL connection refused | Confirm `--add-cloudsql-instances` and `roles/cloudsql.client` |
| 403 on API from browser | Update `CORS_ALLOWED_ORIGINS` with exact Vercel URL |
| 401 from frontend | Firebase Anonymous enabled; `FIREBASE_REQUIRE_AUTH=true`; valid `VITE_FIREBASE_*` |
| Firebase init failed | Secret mount path must match `FIREBASE_CREDENTIALS_PATH` |
| Wrong port | App listens on `PORT` or `8080`; deploy with `--port 8080` |

---

## Redeploy after code changes

```powershell
cd backend
gcloud builds submit --tag us-east4-docker.pkg.dev/legally-7f34d/legally-repo/legally-api:latest
gcloud run deploy legally-api --image us-east4-docker.pkg.dev/legally-7f34d/legally-repo/legally-api:latest --region us-east4
```

---

## Cost tips (hackathon)

- `min-instances 0` scales to zero when idle
- Delete old revisions if needed
- Use smallest Cloud Run memory that works (512Mi–1Gi)
