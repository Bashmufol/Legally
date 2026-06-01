import { useCallback, useEffect, useState } from 'react'
import {
  detectDeviceLocation,
  isDeviceJurisdiction,
  type JurisdictionPayload,
  UNRESOLVED_JURISDICTION,
} from '../lib/jurisdiction'

export function useJurisdiction() {
  const [jurisdiction, setJurisdiction] = useState<JurisdictionPayload>(UNRESOLVED_JURISDICTION)
  const [loading, setLoading] = useState(true)

  const refreshFromDevice = useCallback(async () => {
    setLoading(true)
    try {
      const detected = await detectDeviceLocation()
      setJurisdiction(detected ?? UNRESOLVED_JURISDICTION)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refreshFromDevice()
  }, [refreshFromDevice])

  const hasDeviceLocation = isDeviceJurisdiction(jurisdiction)

  return { jurisdiction, loading, hasDeviceLocation, refreshFromDevice }
}
