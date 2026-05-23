import { useCallback, useEffect, useState } from 'react'
import {
  DEFAULT_JURISDICTION,
  detectDeviceLocation,
  type JurisdictionPayload,
} from '../lib/jurisdiction'

export function useJurisdiction() {
  const [jurisdiction, setJurisdiction] = useState<JurisdictionPayload>(DEFAULT_JURISDICTION)
  const [loading, setLoading] = useState(true)

  const refreshFromDevice = useCallback(async () => {
    setLoading(true)
    const detected = await detectDeviceLocation()
    setJurisdiction(detected ?? DEFAULT_JURISDICTION)
    setLoading(false)
  }, [])

  useEffect(() => {
    refreshFromDevice()
  }, [refreshFromDevice])

  return { jurisdiction, loading, refreshFromDevice }
}
