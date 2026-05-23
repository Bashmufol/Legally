import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { LogIn, LogOut, Scale } from 'lucide-react'
import Disclaimer from './Disclaimer'
import { useAuth } from '../context/AuthContext'
import { setAuthTokenProvider } from '../api/client'

export default function Layout({ children }: { children: React.ReactNode }) {
  const { pathname } = useLocation()
  const { user, loading, configured, signInGuest, signOutUser, getIdToken } = useAuth()

  useEffect(() => {
    setAuthTokenProvider(getIdToken)
  }, [getIdToken])

  const nav = [
    { to: '/', label: 'Home' },
    { to: '/consult', label: 'Consult' },
    { to: '/about', label: 'About' },
  ]

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-legally-navy text-white shadow-lg">
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 font-display text-xl">
            <Scale className="w-7 h-7 text-legally-gold" />
            <span>
              Legally<span className="text-legally-gold">.</span>
            </span>
          </Link>
          <nav className="flex items-center gap-4 text-sm font-medium">
            {configured && (
              <button
                type="button"
                onClick={user ? signOutUser : signInGuest}
                disabled={loading}
                className="inline-flex items-center gap-1.5 rounded-lg border border-white/25 px-3 py-1.5 text-xs hover:bg-white/10"
              >
                {user ? <LogOut className="w-3.5 h-3.5" /> : <LogIn className="w-3.5 h-3.5" />}
                {loading ? '…' : user ? 'New session' : 'Connect'}
              </button>
            )}
            {nav.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className={
                  pathname === item.to
                    ? 'text-legally-gold'
                    : 'text-white/80 hover:text-white'
                }
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
      </header>

      <main className="flex-1">{children}</main>

      <footer className="border-t border-legally-navy/10 bg-white/60">
        <div className="max-w-6xl mx-auto px-4 py-6">
          <Disclaimer compact />
          <p className="text-xs text-legally-navy/50 mt-3 text-center">
            Legally — accessible legal information worldwide
          </p>
        </div>
      </footer>
    </div>
  )
}
