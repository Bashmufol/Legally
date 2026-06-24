# Legally Backend API

Legally is a REST API that helps people understand everyday legal questions in plain language. It is not a substitute for a licensed lawyer. Clients send a scenario by text, voice, or uploaded files, and the API returns structured guidance: a summary, legal points with source links where possible, practical steps, and contacts for relevant organisations when those details appear on official sources.

The service resolves jurisdiction from device location and from explicit place names in the user's message or uploads. Legal research runs through a configurable multi-provider AI chain (Gemini first, with optional Groq, OpenRouter, Mistral, Cloudflare, and Hugging Face). Gemini can ground answers with Google Search on government and court sites. If no provider returns usable material, the API responds with a clear no-information message instead of invented law. Contact research uses the same failover pattern.

## Features

- **Legal consultations** via `POST /api/consult` (tenancy, land, employment, police encounters, and more)
- **Multimodal input** through file uploads plus speech-to-text for audio attachments
- **Jurisdiction resolution** from request fields, message text, and optional Gemini detection
- **Multi-LLM failover** controlled by `LLM_PROVIDER_ORDER`
- **Contact cards** with phone, email, or social handles only when tied to cited official pages
- **Document drafting** for agreements, letters, and demand letters
- **Session-scoped data** with TTL-based cleanup and an explicit end-session endpoint

## Tech stack

| Area | Technology |
|------|------------|
| Runtime | Java 21, Spring Boot 4 |
| API | Spring Web, Validation, Actuator |
| Security | Spring Security, Firebase Admin SDK |
| Persistence | Spring Data JPA, PostgreSQL |
| Cloud database | Cloud SQL (PostgreSQL socket factory) |
| File storage | Firebase Storage (local disk when Firebase is disabled) |
| AI | Gemini API, Google Speech-to-Text, optional OpenAI-compatible providers |
| Build and deploy | Maven, Docker, Cloud Run |

## API overview

All routes under `/api/**` expect a Firebase ID token in `Authorization: Bearer <token>` when Firebase auth is enabled. Clients should also send `X-Legally-Session-Id` (a UUID) on uploads and consultations so history and files stay scoped to one session.

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/consult` | Run legal research and return a structured response |
| `POST` | `/api/uploads` | Upload photos, PDFs, audio, or video |
| `GET` | `/api/uploads/files/{fileName}` | Serve local uploads (dev only when Firebase is off) |
| `GET` | `/api/history/consultations` | List consultations for the current user and session |
| `GET` | `/api/history/consultations/{id}` | Fetch one consultation record |
| `POST` | `/api/session/end` | End session and delete its uploads and history |
| `POST` | `/api/documents/generate` | Generate a legal document draft |
| `POST` | `/api/demand-letter` | Generate a demand letter |
| `GET` | `/actuator/health` | Health check (public) |

Public routes also include `OPTIONS` and `/actuator/info`. Configure allowed browser origins with `CORS_ALLOWED_ORIGINS` when a separate web client hosts the UI.

## How a consult works

1. **Jurisdiction** is resolved from device fields, parsed place names, or Gemini when needed. If jurisdiction is still unknown, the API returns a dedicated response and does not run legal research.
2. **Media** is transcribed (audio) or summarised (images, PDF, video) before or during provider calls.
3. **Legal research** tries each configured provider in order until one returns substantive, citable content. Gemini uses native multimodal input and the `google_search` tool.
4. **Contacts** run on a separate provider chain when the legal result calls for them.
5. **Persistence** saves the consultation to PostgreSQL when auth and the database are enabled.

## Google Cloud and Firebase

| Product | Role |
|---------|------|
| **Gemini API** | Legal research, contacts, media digest, jurisdiction hints |
| **Google Speech-to-Text** | Audio transcription |
| **Firebase Auth** | Anonymous (or configured) user tokens verified on the API |
| **Firebase Storage** | Production file uploads |
| **Cloud SQL** | PostgreSQL for users, sessions, consultations, upload metadata |
| **Cloud Run** | Recommended host for the containerised API |

## Prerequisites

- Java 21 and Maven 3.9+
- Docker (for local PostgreSQL)
- A [Gemini API](https://ai.google.dev/) key (required for multimodal consults and search grounding)
- A [Firebase](https://console.firebase.google.com/) project with Auth and Storage (optional for local dev without Firebase)
- Optional fallback keys: [Groq](https://console.groq.com/), [OpenRouter](https://openrouter.ai/), [Mistral](https://console.mistral.ai/), [Cloudflare Workers AI](https://developers.cloudflare.com/workers-ai/), [Hugging Face](https://huggingface.co/settings/tokens)

## Local development

### 1. Start PostgreSQL

From the repository root:

```bash
docker compose up -d postgres
```

Default connection: `jdbc:postgresql://localhost:5432/legally`, user `legally`, password `legally`.

### 2. Configure environment

Create `backend/.env` with at least:

```env
DATABASE_MODE=local
DATABASE_URL=jdbc:postgresql://localhost:5432/legally
DATABASE_USERNAME=legally
DATABASE_PASSWORD=legally

GEMINI_API_KEY=your_gemini_key

# Optional: comma-separated provider order (only configured providers are used)
LLM_PROVIDER_ORDER=gemini,groq,openrouter

# CORS for your web client origin(s)
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Add Firebase settings when auth and cloud storage are enabled:

```env
FIREBASE_ENABLED=true
FIREBASE_REQUIRE_AUTH=true
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com
FIREBASE_CREDENTIALS_PATH=firebase-service-account.json
```

Place the Firebase Admin SDK JSON at `backend/firebase-service-account.json` (or the path set in `FIREBASE_CREDENTIALS_PATH`).

### 3. Run the API

```bash
cd backend
mvn spring-boot:run
```

The server listens on `http://localhost:8080` by default. Startup logs list `LLM_PROVIDER_ORDER` and which providers joined the active chain.

### Development without Firebase

```powershell
$env:FIREBASE_ENABLED="false"
$env:FIREBASE_REQUIRE_AUTH="false"
```

Uploads are stored under `backend/uploads/`. Consultation history is not persisted for guest mode.

## Session and data retention

- Clients send `X-Legally-Session-Id` with uploads and consults.
- `POST /api/session/end` deletes that session's uploads (Firebase or local disk), consultation history, and demand letters.
- A scheduled job purges inactive sessions after **72 hours** by default (`SESSION_TTL_HOURS`).

## Security

- When Firebase is enabled, the API verifies `Authorization: Bearer <Firebase ID token>` with the Admin SDK.
- Data in PostgreSQL is scoped to the authenticated Firebase user and session.
- API keys and database credentials belong in environment variables or `.env`, not in source control.

## Production deployment

1. Build the image from `backend/Dockerfile`.
2. Deploy to **Cloud Run** and set environment variables (or Secret Manager references).
3. Use `SPRING_PROFILES_ACTIVE=cloudsql` with `CLOUD_SQL_INSTANCE_CONNECTION_NAME`, `DATABASE_URL`, and service account credentials for Cloud SQL.
4. Set `CORS_ALLOWED_ORIGINS` to the origin(s) of the web client that calls this API.
5. Point the client at the Cloud Run service URL for all `/api` requests.

Example build:

```bash
cd backend
docker build -t legally-api .
```

## Project layout

```
backend/
  src/main/java/com/legally/
    controller/     REST endpoints
    service/        Application logic (consult, jurisdiction, storage, sessions)
    llm/            Provider integrations and orchestration
    entity/         JPA entities
    security/       Firebase auth and session filters
  src/main/resources/
    application.properties
  Dockerfile
docker-compose.yml   Local PostgreSQL
```

## Disclaimer

Legally provides general legal information only. It is not legal advice. Users should verify citations, contact details, and important steps with a qualified lawyer in their jurisdiction.

## License

See repository license file if present.
