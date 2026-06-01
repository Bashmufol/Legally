export type LocationSource = 'device' | 'input_override' | 'default_fallback'

export interface JurisdictionPayload {
  countryCode: string
  countryName: string
  regionCode: string
  regionName: string
  locationSource: LocationSource
}

export interface ResolvedJurisdiction extends JurisdictionPayload {
  loading: boolean
  error: string | null
  fromDevice: boolean
}

const NOMINATIM_URL = 'https://nominatim.openstreetmap.org/reverse'
const BIG_DATA_CLOUD_URL = 'https://api.bigdatacloud.net/data/reverse-geocode-client'

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function reverseGeocode(lat: number, lon: number): Promise<JurisdictionPayload | null> {
  const params = new URLSearchParams({
    format: 'json',
    lat: String(lat),
    lon: String(lon),
    addressdetails: '1',
  })
  const res = await fetch(`${NOMINATIM_URL}?${params}`, {
    headers: { Accept: 'application/json', 'User-Agent': 'LegallyApp/1.0 (legal-info-demo)' },
  })
  if (!res.ok) return null
  const data = (await res.json()) as {
    address?: {
      country_code?: string
      country?: string
      state?: string
      region?: string
      county?: string
    }
  }
  const addr = data.address
  if (!addr?.country_code) return null
  const regionName = addr.state || addr.region || addr.county || 'General'
  const base: JurisdictionPayload = {
    countryCode: addr.country_code.toUpperCase(),
    countryName: addr.country || addr.country_code.toUpperCase(),
    regionCode: regionName.toUpperCase().replace(/\s+/g, '_').slice(0, 32),
    regionName,
    locationSource: 'device',
  }
  return normalizeNigeria(base)
}

async function reverseGeocodeBigDataCloud(lat: number, lon: number): Promise<JurisdictionPayload | null> {
  const params = new URLSearchParams({
    latitude: String(lat),
    longitude: String(lon),
    localityLanguage: 'en',
  })
  const res = await fetch(`${BIG_DATA_CLOUD_URL}?${params}`)
  if (!res.ok) return null
  const data = (await res.json()) as {
    countryCode?: string
    countryName?: string
    principalSubdivision?: string
  }
  if (!data.countryCode) return null
  const regionName = data.principalSubdivision || 'General'
  const base: JurisdictionPayload = {
    countryCode: data.countryCode.toUpperCase(),
    countryName: data.countryName || data.countryCode.toUpperCase(),
    regionCode: regionName.toUpperCase().replace(/\s+/g, '_').slice(0, 32),
    regionName,
    locationSource: 'device',
  }
  return normalizeNigeria(base)
}

async function reverseGeocodeWithRetry(lat: number, lon: number): Promise<JurisdictionPayload | null> {
  for (let attempt = 0; attempt < 2; attempt++) {
    const fromNominatim = await reverseGeocode(lat, lon)
    if (fromNominatim) return fromNominatim
    if (attempt === 0) await sleep(600)
  }
  return reverseGeocodeBigDataCloud(lat, lon)
}

/** Initial / unresolved state — never sent to the API as a real jurisdiction. */
export const UNRESOLVED_JURISDICTION: JurisdictionPayload = {
  countryCode: '',
  countryName: '',
  regionCode: '',
  regionName: '',
  locationSource: 'default_fallback',
}

/** @deprecated Use UNRESOLVED_JURISDICTION */
export const DEFAULT_JURISDICTION = UNRESOLVED_JURISDICTION

export function isDeviceJurisdiction(j: JurisdictionPayload): boolean {
  return (
    j.locationSource === 'device' &&
    j.countryCode.length > 0 &&
    j.countryCode !== 'INT'
  )
}

/** Only device GPS results are sent; text/media overrides are resolved on the server. */
export function jurisdictionForApi(j: JurisdictionPayload): JurisdictionPayload | undefined {
  return isDeviceJurisdiction(j) ? j : undefined
}

export function detectDeviceLocation(): Promise<JurisdictionPayload | null> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve(null)
      return
    }
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          const j = await reverseGeocodeWithRetry(pos.coords.latitude, pos.coords.longitude)
          resolve(j)
        } catch {
          resolve(null)
        }
      },
      () => resolve(null),
      { enableHighAccuracy: false, timeout: 20000, maximumAge: 300000 },
    )
  })
}

/** Nominatim state names → stable codes for Nigeria. */
const NG_STATE_ALIASES: Record<string, { regionCode: string; regionName: string }> = {
  kwara: { regionCode: 'KWARA', regionName: 'Kwara State' },
  ilorin: { regionCode: 'KWARA', regionName: 'Kwara State' },
  lagos: { regionCode: 'LAGOS', regionName: 'Lagos State' },
  abuja: { regionCode: 'FCT', regionName: 'Federal Capital Territory' },
  'federal capital territory': { regionCode: 'FCT', regionName: 'Federal Capital Territory' },
  fct: { regionCode: 'FCT', regionName: 'Federal Capital Territory' },
  kano: { regionCode: 'KANO', regionName: 'Kano State' },
  rivers: { regionCode: 'RIVERS', regionName: 'Rivers State' },
}

function normalizeNigeria(payload: JurisdictionPayload): JurisdictionPayload {
  if (payload.countryCode !== 'NG') return payload
  const haystack = `${payload.regionCode} ${payload.regionName}`.toLowerCase()
  for (const [alias, mapped] of Object.entries(NG_STATE_ALIASES)) {
    if (haystack.includes(alias)) {
      return { ...payload, regionCode: mapped.regionCode, regionName: mapped.regionName }
    }
  }
  return payload
}
