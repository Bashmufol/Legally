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
  return {
    countryCode: addr.country_code.toUpperCase(),
    countryName: addr.country || addr.country_code.toUpperCase(),
    regionCode: regionName.toUpperCase().replace(/\s+/g, '_').slice(0, 32),
    regionName,
    locationSource: 'device',
  }
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
          const j = await reverseGeocode(pos.coords.latitude, pos.coords.longitude)
          resolve(j)
        } catch {
          resolve(null)
        }
      },
      () => resolve(null),
      { timeout: 12000, maximumAge: 300000 },
    )
  })
}

export const DEFAULT_JURISDICTION: JurisdictionPayload = {
  countryCode: 'INT',
  countryName: 'International',
  regionCode: 'GENERAL',
  regionName: 'General',
  locationSource: 'default_fallback',
}
