import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { History, Loader2, Send } from 'lucide-react'
import { consult, fetchConsultationHistory } from '../api/client'
import { useAuth } from '../context/AuthContext'
import type { HistoryItem } from '../types'
import UploadZone from '../components/UploadZone'
import VoiceRecorder from '../components/VoiceRecorder'
import ResultCards from '../components/ResultCards'
import DemandLetterModal from '../components/DemandLetterModal'
import type { ConsultResponse, MediaRef, Scenario } from '../types'

const SCENARIOS: { id: Scenario; label: string }[] = [
  { id: 'police_stop', label: 'Police stop' },
  { id: 'tenancy', label: 'Rent / tenancy' },
  { id: 'land', label: 'Land' },
  { id: 'general', label: 'General' },
]

export default function ConsultPage() {
  const [params] = useSearchParams()
  const initial = (params.get('scenario') as Scenario) || 'general'

  const [scenario, setScenario] = useState<Scenario>(initial)
  const [message, setMessage] = useState('')
  const [media, setMedia] = useState<MediaRef[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<ConsultResponse | null>(null)
  const [letterOpen, setLetterOpen] = useState(false)
  const [history, setHistory] = useState<HistoryItem[]>([])
  const { user, configured, loading: authLoading, signInGuest } = useAuth()

  useEffect(() => {
    if (configured && !user && !authLoading) {
      signInGuest().catch(() => setError('Could not start secure session. Check Firebase config.'))
    }
  }, [configured, user, authLoading, signInGuest])

  useEffect(() => {
    if (!user) return
    fetchConsultationHistory()
      .then(setHistory)
      .catch(() => setHistory([]))
  }, [user, result])

  const addMedia = (m: MediaRef) => setMedia((prev) => [...prev, m])

  const submit = async () => {
    if (!message.trim()) {
      setError('Please describe your situation.')
      return
    }
    if (configured && !user) {
      setError('Starting secure session… try again in a moment.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const res = await consult(message, scenario, media)
      setResult(res)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Consultation failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <h1 className="font-display text-3xl mb-2">Legal consultation</h1>
      <p className="text-legally-navy/70 mb-8 text-sm">
        Describe your issue, upload documents or recordings, and receive grounded legal information.
      </p>

      <div className="flex flex-wrap gap-2 mb-6">
        {SCENARIOS.map((s) => (
          <button
            key={s.id}
            type="button"
            onClick={() => setScenario(s.id)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition ${
              scenario === s.id
                ? 'bg-legally-navy text-white'
                : 'bg-white border border-legally-navy/20 text-legally-navy/80 hover:border-legally-gold'
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>

      <div className="grid lg:grid-cols-2 gap-8">
        <div className="space-y-4">
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            rows={6}
            placeholder="e.g. Police stopped me and demanded my phone password…"
            className="w-full rounded-xl border border-legally-navy/15 bg-white px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-legally-gold/50"
          />
          <UploadZone onUploaded={addMedia} disabled={loading} />
          <VoiceRecorder onRecorded={addMedia} disabled={loading} />
          {media.length > 0 && (
            <p className="text-xs text-legally-navy/60">{media.length} file(s) attached</p>
          )}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="button"
            onClick={submit}
            disabled={loading}
            className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-legally-gold text-legally-navy font-semibold py-3 hover:opacity-90 disabled:opacity-50"
          >
            {loading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                Analyzing with Gemini…
              </>
            ) : (
              <>
                <Send className="w-5 h-5" />
                Get legal guidance
              </>
            )}
          </button>
        </div>

        <div>
          {result ? (
            <>
              {result.demandLetterEligible && (
                <button
                  type="button"
                  onClick={() => setLetterOpen(true)}
                  className="mb-4 w-full rounded-lg border-2 border-legally-gold text-legally-navy font-semibold py-2 text-sm hover:bg-legally-gold/10"
                >
                  Generate demand letter
                </button>
              )}
              <ResultCards result={result} />
            </>
          ) : (
            <div className="rounded-xl border border-dashed border-legally-navy/20 bg-white/50 p-12 text-center text-legally-navy/50 text-sm">
              Your analysis will appear here — summary, law, steps, and contacts.
            </div>
          )}
        </div>
      </div>

      {user && history.length > 0 && (
        <section className="mt-12">
          <h2 className="font-display text-xl mb-4 flex items-center gap-2">
            <History className="w-5 h-5 text-legally-gold" />
            Your recent consultations
          </h2>
          <ul className="space-y-3">
            {history.slice(0, 5).map((h) => (
              <li
                key={h.id}
                className="rounded-lg bg-white border border-legally-navy/10 p-4 text-sm"
              >
                <p className="font-medium capitalize">{h.scenario.replace('_', ' ')}</p>
                <p className="text-legally-navy/70 mt-1 line-clamp-2">{h.summary}</p>
                <p className="text-xs text-legally-navy/40 mt-2">{h.createdAt}</p>
              </li>
            ))}
          </ul>
        </section>
      )}

      <DemandLetterModal
        facts={message}
        open={letterOpen}
        onClose={() => setLetterOpen(false)}
      />
    </div>
  )
}
