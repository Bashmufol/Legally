# Legally

Legally is an AI-powered legal information platform that helps people understand their rights, see what the law says, and take practical next steps—without needing a law degree or an expensive lawyer on call.

Users can consult via text, voice, images, PDFs, or video. The system detects jurisdiction from device location and honors explicit country or state mentions in text or uploads. Legal answers are produced by a **multi-LLM fallback chain** (Gemini first, with optional Groq, OpenRouter, Mistral, Cloudflare, and Hugging Face). **Gemini** can ground research with **Google Search** on official government and court sources. If no provider returns citable material, users see a clear “no information” message with practical suggestions—not invented law. **Contact cards** use the same provider chain: NGOs, government bodies, and related organizations, with phone, email, or social handles only when published on the cited official page. Users can also draft agreements and formal letters for their jurisdiction, preview them, and download PDFs.

## Features

- **Legal consultations** — scenario-based guidance (police interactions, tenancy, land, employment, and more)
- **Multimodal input** — text, voice recordings, documents, and video evidence
- **Global jurisdiction** — automatic location detection with input-based override
- **Official web citations** — each point tied to source URLs where available
- **Multi-LLM fallback** — configurable order via `LLM_PROVIDER_ORDER`; automatic failover on quota or errors
- **Live AI contacts** — Gemini + Google Search first; other providers as fallbacks when configured
- **Document drafting** — rent agreements, land contracts, NDAs, demand letters, and other templates
- **Session privacy** — uploads and history expire after 72 hours of inactivity, or instantly on “New session”

## Architecture

| Layer | Technology |
|-------|------------|
| API | Java 21, Spring Boot 4 |
| Database | PostgreSQL (local Docker or **Cloud SQL**) |
| Authentication | **Firebase** Anonymous Auth |
| File storage | **Firebase Storage** (local filesystem when Firebase is disabled) |
| AI | **Gemini API** + optional OpenAI-compatible providers (Groq, OpenRouter, Mistral, Cloudflare Workers AI, Hugging Face) |
| Web research | **Gemini Google Search** grounding (no separate search API) |
| Web app | React, TypeScript, Tailwind CSS, Vite |
| Production API | **Cloud Run** (recommended) |

### Consultation flow

1. **Resolve jurisdiction** — device geolocation, explicit place in the user’s message, or Gemini detection from media when applicable.
2. **Legal analysis** — try each provider in `LLM_PROVIDER_ORDER` until one returns substantive, cited content. Gemini uses the `google_search` tool for live official sources.
3. **No information** — if every provider fails or returns nothing citable, return a short message and practical next steps (no fabricated statutes).
4. **Contacts** — same provider chain; Gemini searches the web first; other providers use structured JSON from model knowledge when configured.
5. **Persist** — consultation saved per Firebase user and session (when auth and database are enabled).

## Google technologies

| Product | Role in Legally |
|---------|-----------------|
| **Gemini API** | Legal research, contacts, document drafting, jurisdiction hints, no-info fallbacks |
| **Firebase** | Anonymous sign-in, ID token verification, file storage for uploads |
| **Cloud SQL** | Production PostgreSQL (consultations, sessions, upload metadata) |
| **Cloud Run** | Host the Spring Boot API with secure Cloud SQL connectivity |

## Prerequisites

- Java 21 and Maven
- Node.js 18+
- Docker (for local PostgreSQL)
- A [Firebase](https://console.firebase.google.com/) project with Anonymous Auth and Storage enabled (optional for local dev)
- A [Gemini API](https://ai.google.dev/) key (**required** for multimodal consults and Google Search grounding)
- Optional fallback providers: [Groq](https://console.groq.com/), [OpenRouter](https://openrouter.ai/), [Mistral](https://console.mistral.ai/), [Cloudflare Workers AI](https://developers.cloudflare.com/workers-ai/), [Hugging Face](https://huggingface.co/settings/tokens)

## Local development

### 1. Database

```bash
docker compose up -d postgres
```

Set in `backend/.env`:

```env
DATABASE_MODE=local
DATABASE_URL=jdbc:postgresql://localhost:5432/legally
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
```

### 2. Backend

Copy environment variables into `backend/.env` (see Configuration below). Then:

```powershell
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

On startup, logs show `LLM_PROVIDER_ORDER` and which providers are in the active fallback chain.

### 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173`.

Place your Firebase Admin SDK JSON at `backend/firebase-service-account.json` and add the web app config to `frontend/.env` when using Firebase.

### Development without Firebase

```powershell
$env:FIREBASE_ENABLED="false"
$env:FIREBASE_REQUIRE_AUTH="false"
```

Uploads are stored under `backend/uploads/`. Consultation history is not persisted for guest mode.

## Session and data retention

- Each browser tab stores a **session ID** (`X-Legally-Session-Id`) sent with uploads and consultations.
- **New session** (header button) deletes that session’s uploads (Firebase or local disk), consultation history, and demand letters, then starts a fresh anonymous Firebase user and session ID.
- A scheduled job runs hourly and purges sessions with no activity for **72 hours** (configurable via `SESSION_TTL_HOURS`).

## Security

- The web app signs users in anonymously in the background—no login form.
- API requests carry a Firebase ID token when Firebase is enabled; the backend verifies it with the Admin SDK.
- Data is scoped to the authenticated user in PostgreSQL.

## Production deployment

- **Backend:** Build with `backend/Dockerfile` and deploy to **Cloud Run**. Use `SPRING_PROFILES_ACTIVE=cloudsql` (or `legally.database.mode=cloud-sql`) with `CLOUD_SQL_INSTANCE_CONNECTION_NAME` and service account credentials.
- **Database:** **Cloud SQL for PostgreSQL** (often provisioned via Firebase SQL Connect in the Firebase console).
- **Frontend:** `npm run build` and deploy to Vercel, Firebase Hosting, or static hosting. Set `VITE_API_URL` to your API origin and add the frontend URL to `CORS_ALLOWED_ORIGINS`.

## Disclaimer

Legally provides general legal information only. It is not a substitute for advice from a licensed lawyer in your jurisdiction. Always verify contact details, citations, and critical steps with qualified counsel.
