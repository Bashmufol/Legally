export type Scenario =
  | 'police_interaction'
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
  sourceUrl?: string
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
  sourceUrl?: string
}

export interface ContactCard {
  id: string
  tags?: string[]
  name: string
  role: string
  phones?: string[]
  emails?: string[]
  social?: Record<string, string>
  sourceUrl?: string
  notes?: string
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

export type LegalDocumentTypeId =
  | 'DEMAND_LETTER'
  | 'RENT_AGREEMENT'
  | 'LAND_PURCHASE'
  | 'PRENUPTIAL'
  | 'EMPLOYMENT_CONTRACT'
  | 'GENERAL_CONTRACT'
  | 'NDA'
  | 'POWER_OF_ATTORNEY'
  | 'AFFIDAVIT'
  | 'OTHER'

export interface LegalDocumentRequest {
  documentType: LegalDocumentTypeId
  facts: string
  additionalDetails?: string
  customDocumentName?: string
  partyAName?: string
  partyBName?: string
}

export interface LegalDocumentResponse {
  documentType: string
  title: string
  content: string
  disclaimer: string
  jurisdictionCountry?: string
  jurisdictionRegion?: string
  locationSource?: string
}

export interface HistoryItem {
  id: string
  scenario: string
  question: string
  createdAt: string
}

export interface HistoryDetail {
  id: string
  scenario: string
  question: string
  createdAt: string
  response: ConsultResponse
}
