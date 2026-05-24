# Legally

Legally is an AI-powered legal information platform that helps people understand their rights, see what the law says, and take practical next steps—without needing a law degree or an expensive lawyer on call.

Users can consult via text, voice, images, PDFs, or video. The system detects jurisdiction from device location and honors explicit country or state mentions in the user’s input. Answers are grounded in official web sources (government and court sites), with SerpApi search, page excerpts, and Gemini summarization. If that fails, Gemini Google Search grounding is used; if both fail, the user gets a clear “no information” message with practical suggestions. Contact cards show only phone numbers, emails, or social links visible in search results. Users can also draft agreements and formal letters tailored to their jurisdiction, preview them, and download PDFs.

## Features

- **Legal consultations** — scenario-based guidance (police interactions, tenancy, land, employment, and more)
- **Multimodal input** — text, voice recordings, documents, and video evidence
- **Global jurisdiction** — automatic location detection with input-based override
- **Official web citations** — each point tied to source URLs from filtered government and court sites
- **Web-discovered contacts** — phones, emails, or social handles only when shown in search snippets
- **Document drafting** — rent agreements, land contracts, NDAs, demand letters, and other templates

## Architecture

| Layer | Technology |
|-------|------------|
| API | Java 21, Spring Boot 4 |
| Database | PostgreSQL (local Docker or Cloud SQL) |
| Authentication | Firebase Anonymous Auth |
| File storage | Firebase Storage (local filesystem in dev) |
| AI | Google Gemini API |
| Web search | SerpApi (official-domain filtered legal search; open search for contacts) |
| Web app | React, TypeScript, Tailwind CSS, Vite |

### Consultation flow

1. Resolve jurisdiction (device location, user input, or Gemini detection from media).
2. **SerpApi** — search official legal domains, fetch page text.
3. **Gemini** — summarize only from retrieved excerpts with source URLs.
4. If no hits: **Gemini `google_search` grounding** on official sources.
5. If still no citable law: **no-information response** + AI-generated practical suggestions (no invented statutes).
6. **Contacts** — separate SerpApi queries; parse snippets for visible phone, email, or social links.

## Prerequisites

- Java 21 and Maven
- Node.js 18+
- Docker (for local PostgreSQL)
- A [Firebase](https://console.firebase.google.com/) project with Anonymous Auth and Storage enabled
- A [Gemini API](https://ai.google.dev/) key
- A [SerpApi](https://serpapi.com/) key (recommended for web research)

## Local development

### 1. Database

```bash
docker compose up -d postgres
```

### 2. Backend

Copy `backend/.env.example` to `backend/.env` and set your keys. Then:

```powershell
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173`.

Place your Firebase Admin SDK JSON at `backend/firebase-service-account.json` and add the web app config to `frontend/.env` (see the example files).

### Development without Firebase

```powershell
$env:FIREBASE_ENABLED="false"
$env:FIREBASE_REQUIRE_AUTH="false"
```

Uploads are stored under `backend/uploads/`. Consultation history is not persisted for guest mode.

## Security

- The web app signs users in anonymously in the background—no login form.
- API requests carry a Firebase ID token; the backend verifies it with the Admin SDK.
- Data is scoped to the authenticated user in PostgreSQL.

## Configuration

| Variable | Description |
|----------|-------------|
| `GEMINI_API_KEY` | Google Gemini API key |
| `SERPAPI_API_KEY` | SerpApi key for official web search and contact discovery |
| `SERPAPI_ENABLED` | Set `true` to enable SerpApi (default) |
| `DATABASE_MODE` | `local`, `cloud-sql`, or `direct` |
| `FIREBASE_*` | Firebase project and credentials |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins |
| `VITE_API_URL` | Backend URL for the web app |

See `backend/.env.example` and `frontend/.env.example` for the full list.

## Production deployment

- **Backend:** Build with `backend/Dockerfile` and deploy to Cloud Run, Render, or any Java host.
- **Frontend:** Build with `npm run build` and deploy to Vercel or static hosting. Set `VITE_API_URL` to your API origin and add the frontend URL to `CORS_ALLOWED_ORIGINS`.

## Disclaimer

Legally provides general legal information only. It is not a substitute for advice from a licensed lawyer in your jurisdiction. Always verify critical decisions with qualified counsel.
