export type Scenario =
  | 'police_stop'
  | 'tenancy'
  | 'land'
  | 'employment'
  | 'consumer'
  | 'family'
  | 'debt'
  | 'business_contract'
  | 'inheritance'
  | 'general'

export interface MediaRef {
  url: string
  mimeType: string
  storageType: string
}

export interface LegalCitation {
  instrument: string
  section: string
  jurisdiction: string
}

export interface LegalPoint {
  point: string
  chunkId?: string
  citation: LegalCitation
}

export interface LawChunk {
  id: string
  jurisdiction: string
  instrument: string
  section: string
  title: string
  text: string
  tags?: string[]
}

export interface ContactCard {
  id: string
  tags: string[]
  name: string
  role: string
  phones: string[]
  social: Record<string, string>
  notes: string
}

export interface JurisdictionFields {
  countryCode?: string
  countryName?: string
  regionCode?: string
  regionName?: string
  locationSource?: string
  jurisdictionOverride?: boolean
}

export interface ConsultResponse {
  summary: string
  legalAnalysis: LegalPoint[]
  steps: string[]
  contacts: ContactCard[]
  sources: LawChunk[]
  demandLetterEligible: boolean
  confidence: string
  disclaimer: string
  jurisdictionCountry?: string
  jurisdictionRegion?: string
  locationSource?: string
  corpusLimited?: boolean
}

export interface UploadResponse {
  url: string
  mimeType: string
  storageType: string
  fileName: string
}

export interface DemandLetterResponse {
  letter: string
  disclaimer: string
}

export interface HistoryItem {
  id: string
  scenario: string
  message: string
  summary: string
  confidence: string
  createdAt: string
}
