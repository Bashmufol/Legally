# CareerFest 2026 — Legally Submission

## Project

**Legally** — AI-Powered Legal Advisor for Nigeria (Federal + Kwara corpus)

## SDG alignment

**SDG 16: Peace, Justice, and Strong Institutions**

Legally improves access to justice for Nigerians who cannot afford private counsel by providing plain-English rights analysis, grounded legal citations, practical steps, and curated public contact cards.

## Google tools used

1. **Gemini API** (`gemini-2.5-flash`) — multimodal legal analysis, structured JSON responses, demand letter generation
2. **Firebase Anonymous Auth** — secure API access without signup/login UI
3. **Firebase Storage** — evidence file uploads
4. **PostgreSQL** (Cloud SQL / Firebase GCP project) — consultation history and user data

## Live demo

| Resource | URL |
|----------|-----|
| Web app | Deploy `frontend/` to Vercel — see [DEPLOY.md](./DEPLOY.md) |
| Backend API | Deploy `backend/` via Docker to Render — see [DEPLOY.md](./DEPLOY.md) |
| GitHub | _Add your public repository URL before submitting_ |
| Demo video | _Record 2–3 min screen capture; add YouTube/Drive link here_ |

**Before submit:** Replace placeholder URLs above and record the demo video.

## Demo script (2–3 minutes)

### 1. Police stop / phone search

- Scenario: **Police stop**
- Input: “Officer demanded my phone and password on the road in Ilorin”
- Show: legal analysis citing Constitution / Police Act / ACJA, steps, Police PRO & NHRC contacts

### 2. Rent hike / tenancy

- Scenario: **Rent / tenancy**
- Input: describe 50% unlawful rent increase; optionally upload lease image
- Show: analysis + **Generate demand letter** button

### 3. Land purchase

- Scenario: **Land**
- Input: “I want to buy land but fear fraud” + upload agreement photo/PDF if available
- Show: due diligence steps, Land Use Act citations, Kwara Lands Ministry contact

## How to run locally

```bash
# Backend
cd backend
export GEMINI_API_KEY=your_key
./mvnw spring-boot:run

# Frontend
cd frontend
echo "VITE_API_URL=http://localhost:8080" > .env
npm install && npm run dev
```

## GCS setup (production)

1. Create a GCS bucket (e.g. `legally-uploads`)
2. Create a service account with Storage Object Admin
3. Set environment variables:
   - `GEMINI_API_KEY`
   - `GCS_BUCKET=legally-uploads`
   - `GCS_ENABLED=true`
   - `GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json`

## Team

Solo builder — University of Ilorin / GDG on Campus CareerFest 2026

## Disclaimer

Legally provides general legal information only, not legal advice.
