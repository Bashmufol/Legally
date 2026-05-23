import type {
  ConsultResponse,
  DemandLetterResponse,
  HistoryItem,
  MediaRef,
  Scenario,
  UploadResponse,
} from '../types'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

let tokenProvider: (() => Promise<string | null>) | null = null

export function setAuthTokenProvider(provider: () => Promise<string | null>) {
  tokenProvider = provider
}

async function authHeaders(): Promise<HeadersInit> {
  const headers: Record<string, string> = {}
  if (tokenProvider) {
    const token = await tokenProvider()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

async function parseError(res: Response): Promise<string> {
  try {
    const data = await res.json()
    return data.error || res.statusText
  } catch {
    return res.statusText
  }
}

async function apiFetch(input: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(input, init)
  } catch (err) {
    const hint =
      `Cannot reach the API at ${API_URL}. ` +
      'Ensure the backend is running on port 8080, then open the app at ' +
      'http://localhost:5173 (not 127.0.0.1) so CORS matches.'
    if (err instanceof TypeError) {
      throw new Error(hint)
    }
    throw err
  }
}

export async function consult(
  message: string,
  scenario: Scenario,
  media: MediaRef[] = [],
): Promise<ConsultResponse> {
  const res = await apiFetch(`${API_URL}/api/consult`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: JSON.stringify({ message, scenario: scenario === 'general' ? null : scenario, media }),
  })
  if (res.status === 401) {
    throw new Error(
      'Unauthorized: sign in failed or Firebase token missing. Check frontend .env Firebase keys and Anonymous auth in Firebase Console.',
    )
  }
  if (!res.ok) throw new Error(await parseError(res))
  return res.json()
}

export async function uploadFile(file: File): Promise<UploadResponse> {
  const form = new FormData()
  form.append('file', file)
  const res = await apiFetch(`${API_URL}/api/uploads`, {
    method: 'POST',
    headers: await authHeaders(),
    body: form,
  })
  if (res.status === 401) {
    throw new Error('Unauthorized: wait for secure session or check Firebase Anonymous auth.')
  }
  if (!res.ok) throw new Error(await parseError(res))
  const data: UploadResponse = await res.json()
  if (data.storageType === 'local' && data.url.startsWith('/')) {
    return { ...data, url: `${API_URL}${data.url}` }
  }
  return data
}

export async function generateDemandLetter(
  facts: string,
  scenario = 'tenancy',
  senderName?: string,
  recipientName?: string,
): Promise<DemandLetterResponse> {
  const res = await apiFetch(`${API_URL}/api/demand-letter`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: JSON.stringify({ facts, scenario, senderName, recipientName }),
  })
  if (!res.ok) throw new Error(await parseError(res))
  return res.json()
}

export async function fetchConsultationHistory(): Promise<HistoryItem[]> {
  const res = await apiFetch(`${API_URL}/api/history/consultations`, {
    headers: await authHeaders(),
  })
  if (!res.ok) throw new Error(await parseError(res))
  return res.json()
}
