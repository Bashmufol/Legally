import type {
  ConsultResponse,
  DemandLetterResponse,
  HistoryItem,
  JurisdictionFields,
  LegalDocumentRequest,
  LegalDocumentResponse,
  MediaRef,
  Scenario,
  UploadResponse,
} from '../types'
import type { JurisdictionPayload } from '../lib/jurisdiction'
import { parseApiErrorResponse, UserFacingError } from '../lib/errors'

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

async function apiFetch(input: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(input, init)
  } catch {
    throw new UserFacingError(
      "We couldn't reach Legally right now. Check your internet connection and try again.",
    )
  }
}

async function ensureOk(res: Response): Promise<void> {
  if (!res.ok) {
    throw new UserFacingError(await parseApiErrorResponse(res))
  }
}

export async function consult(
  message: string,
  scenario: Scenario,
  media: MediaRef[] = [],
  jurisdiction?: JurisdictionPayload,
): Promise<ConsultResponse> {
  const body: Record<string, unknown> = {
    message,
    scenario: scenario === 'general' ? null : scenario,
    media,
  }
  if (jurisdiction) {
    body.countryCode = jurisdiction.countryCode
    body.countryName = jurisdiction.countryName
    body.regionCode = jurisdiction.regionCode
    body.regionName = jurisdiction.regionName
    body.locationSource = jurisdiction.locationSource
  }
  const res = await apiFetch(`${API_URL}/api/consult`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: JSON.stringify(body),
  })
  await ensureOk(res)
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
  await ensureOk(res)
  const data: UploadResponse = await res.json()
  if (data.storageType === 'local' && data.url.startsWith('/')) {
    return { ...data, url: `${API_URL}${data.url}` }
  }
  return data
}

export async function generateLegalDocument(
  request: LegalDocumentRequest,
  jurisdiction?: JurisdictionPayload,
): Promise<LegalDocumentResponse> {
  const body: Record<string, unknown> = { ...request }
  if (jurisdiction) {
    body.countryCode = jurisdiction.countryCode
    body.countryName = jurisdiction.countryName
    body.regionCode = jurisdiction.regionCode
    body.regionName = jurisdiction.regionName
    body.locationSource = jurisdiction.locationSource
  }
  const res = await apiFetch(`${API_URL}/api/documents/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: JSON.stringify(body),
  })
  await ensureOk(res)
  return res.json()
}

export async function generateDemandLetter(
  facts: string,
  scenario = 'tenancy',
  senderName?: string,
  recipientName?: string,
  jurisdiction?: JurisdictionFields,
): Promise<DemandLetterResponse> {
  const res = await apiFetch(`${API_URL}/api/demand-letter`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: JSON.stringify({ facts, scenario, senderName, recipientName, ...jurisdiction }),
  })
  await ensureOk(res)
  return res.json()
}

export async function fetchConsultationHistory(): Promise<HistoryItem[]> {
  const res = await apiFetch(`${API_URL}/api/history/consultations`, {
    headers: await authHeaders(),
  })
  await ensureOk(res)
  return res.json()
}
