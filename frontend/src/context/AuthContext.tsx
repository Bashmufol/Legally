import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { onAuthStateChanged, signOut, type User } from 'firebase/auth'
import { getFirebaseAuth, isFirebaseConfigured, signInAnonymousUser } from '../lib/firebase'
import { setAuthTokenProvider } from '../api/client'

interface AuthContextValue {
  user: User | null
  loading: boolean
  configured: boolean
  signInGuest: () => Promise<void>
  signOutUser: () => Promise<void>
  getIdToken: () => Promise<string | null>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const configured = isFirebaseConfigured()

  const getIdToken = useCallback(async () => {
    if (!user) return null
    return user.getIdToken()
  }, [user])

  useEffect(() => {
    setAuthTokenProvider(getIdToken)
  }, [getIdToken])

  useEffect(() => {
    if (!configured) {
      setLoading(false)
      return
    }
    const auth = getFirebaseAuth()
    if (!auth) {
      setLoading(false)
      return
    }
    const unsub = onAuthStateChanged(auth, (u) => {
      setUser(u)
      setLoading(false)
    })
    signInAnonymousUser().catch(() => {
      /* onAuthStateChanged will reflect failure */
    })
    return () => unsub()
  }, [configured])

  const signInGuest = useCallback(async () => {
    if (!configured) return
    await signInAnonymousUser()
  }, [configured])

  const signOutUser = useCallback(async () => {
    const firebaseAuth = getFirebaseAuth()
    if (!firebaseAuth) return
    await signOut(firebaseAuth)
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      configured,
      signInGuest,
      signOutUser,
      getIdToken,
    }),
    [user, loading, configured, signInGuest, signOutUser, getIdToken],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
