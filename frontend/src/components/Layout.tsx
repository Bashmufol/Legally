import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { LogIn, LogOut, Menu, Scale, X } from 'lucide-react'
import Disclaimer from './Disclaimer'
import { useAuth } from '../context/AuthContext'
import { setAuthTokenProvider } from '../api/client'

export default function Layout({ children }: { children: React.ReactNode }) {
  const { pathname } = useLocation()
  const { user, loading, configured, signInGuest, signOutUser, getIdToken } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    setAuthTokenProvider(getIdToken)
  }, [getIdToken])

  useEffect(() => {
    setMenuOpen(false)
  }, [pathname])

  useEffect(() => {
    document.body.style.overflow = menuOpen ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [menuOpen])

  const nav = [
    { to: '/', label: 'Home' },
    { to: '/consult', label: 'Consult' },
    { to: '/documents', label: 'Documents' },
    { to: '/about', label: 'About' },
  ]

  const navLinkClass = (active: boolean) =>
    active ? 'text-legally-gold' : 'text-white/80 hover:text-white'

  return (
    <div className="min-h-screen flex flex-col w-full overflow-x-hidden">
      <header className="bg-legally-navy text-white shadow-lg sticky top-0 z-40">
        <div className="max-w-6xl mx-auto px-4 py-3 md:py-4 flex items-center justify-between gap-3 min-w-0">
          <Link
            to="/"
            className="flex items-center gap-2 font-display text-lg md:text-xl shrink-0 min-w-0"
          >
            <Scale className="w-6 h-6 md:w-7 md:h-7 text-legally-gold shrink-0" />
            <span className="truncate">
              Legally<span className="text-legally-gold">.</span>
            </span>
          </Link>

          <nav className="hidden md:flex items-center gap-3 lg:gap-4 text-sm font-medium shrink-0">
            {configured && (
              <button
                type="button"
                onClick={user ? signOutUser : signInGuest}
                disabled={loading}
                className="inline-flex items-center gap-1.5 rounded-lg border border-white/25 px-3 py-1.5 text-xs hover:bg-white/10 whitespace-nowrap"
              >
                {user ? <LogOut className="w-3.5 h-3.5" /> : <LogIn className="w-3.5 h-3.5" />}
                {loading ? '…' : user ? 'New session' : 'Connect'}
              </button>
            )}
            {nav.map((item) => (
              <Link key={item.to} to={item.to} className={navLinkClass(pathname === item.to)}>
                {item.label}
              </Link>
            ))}
          </nav>

          <button
            type="button"
            className="md:hidden inline-flex items-center justify-center rounded-lg border border-white/25 p-2 hover:bg-white/10 shrink-0"
            onClick={() => setMenuOpen((open) => !open)}
            aria-expanded={menuOpen}
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          >
            {menuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {menuOpen && (
          <div className="md:hidden border-t border-white/10 bg-legally-navy">
            <nav className="max-w-6xl mx-auto px-4 py-4 flex flex-col gap-1">
              {nav.map((item) => (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`rounded-lg px-3 py-2.5 text-base font-medium ${
                    pathname === item.to ? 'bg-white/10 text-legally-gold' : 'text-white/90'
                  }`}
                >
                  {item.label}
                </Link>
              ))}
              {configured && (
                <button
                  type="button"
                  onClick={user ? signOutUser : signInGuest}
                  disabled={loading}
                  className="mt-2 inline-flex items-center justify-center gap-2 rounded-lg border border-white/25 px-3 py-2.5 text-sm hover:bg-white/10"
                >
                  {user ? <LogOut className="w-4 h-4" /> : <LogIn className="w-4 h-4" />}
                  {loading ? '…' : user ? 'New session' : 'Connect'}
                </button>
              )}
            </nav>
          </div>
        )}
      </header>

      <main className="flex-1 w-full min-w-0">{children}</main>

      <footer className="border-t border-legally-navy/10 bg-white/60 w-full">
        <div className="max-w-6xl mx-auto px-4 py-6">
          <Disclaimer compact />
          <p className="text-xs text-legally-navy/50 mt-3 text-center">
            Legally · accessible legal information worldwide
          </p>
        </div>
      </footer>
    </div>
  )
}
