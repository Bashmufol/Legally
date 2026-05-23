# Local dev: "Failed to fetch" / upload errors

## Your backend is fine if IntelliJ shows `Started LegallyApplication`

The browser errors are almost always **frontend ↔ backend** connectivity, not Cloud SQL.

## Checklist

### 1. Open the app at `http://localhost:5173`

Do **not** use `http://127.0.0.1:5173` unless `CORS_ALLOWED_ORIGINS` includes it.

Backend `.env` should include:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

Restart the backend after changing CORS.

### 2. Frontend must reach the API

`frontend/.env`:

```env
VITE_API_URL=http://localhost:8080
```

Restart Vite after changing (`npm run dev`).

Test in browser: http://localhost:8080/actuator/health → `{"status":"UP"}`

### 3. Firebase Anonymous auth (when `FIREBASE_REQUIRE_AUTH=true`)

- Fill all `VITE_FIREBASE_*` in `frontend/.env`
- Firebase Console → Authentication → **Anonymous** enabled
- **Authorized domains** must include `localhost`

Wait a few seconds on the Consult page for anonymous sign-in before submitting.

For quick local testing without tokens:

```env
FIREBASE_REQUIRE_AUTH=false
```

(restart backend)

### 4. IntelliJ must load `backend/.env`

Working directory: project root `Legally` (not `backend/` only).

`application.properties` imports `optional:file:backend/.env[.properties]`.

Service account path from repo root:

```env
FIREBASE_CREDENTIALS_PATH=backend/firebase-service-account.json
```

### 5. Cloud SQL from your laptop

Requires **Application Default Credentials**:

```powershell
gcloud auth application-default login
gcloud config set project legally-7f34d
```

First connection can take ~10 seconds (normal in logs).

## Gemini: DNS / "hostname resolution" errors

If you see:

`I/O error on POST request for "https://generativelanguage.googleapis.com/..."`

your **PC cannot reach Google’s Gemini API** (DNS, firewall, VPN, or no internet). The backend and database are fine.

### Fix network access

1. In PowerShell:

   ```powershell
   nslookup generativelanguage.googleapis.com
   ping generativelanguage.googleapis.com
   ```

   If these fail, fix Wi‑Fi/DNS (try Google DNS `8.8.8.8`) or disable VPN/proxy.

2. Open https://generativelanguage.googleapis.com in a browser (may show 404 — that still proves DNS works).

3. In `backend/.env` use a stable model:

   ```env
   GEMINI_MODEL=gemini-2.0-flash
   ```

4. Restart the backend.

### Degraded mode (code fallback)

If Gemini is still unreachable, the app now returns **corpus-based guidance** instead of a hard error so you can keep testing uploads and auth.

---

## Verify in DevTools

1. F12 → **Network**
2. Submit consult or upload
3. Check request URL is `http://localhost:8080/api/...`
4. If **(failed)** or CORS error → fix steps 1–2
5. If **401** → fix step 3
6. If **500** → check backend log (Gemini key, Storage permissions)
