import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { History, Loader2, Send } from 'lucide-react'
import { consult, fetchConsultationHistory } from '../api/client'
import { useAuth } from '../context/AuthContext'
import type { HistoryItem } from '../types'
import UploadZone from '../components/UploadZone'
import VoiceRecorder from '../components/VoiceRecorder'
import AttachedMediaList from '../components/AttachedMediaList'
import { useJurisdiction } from '../hooks/useJurisdiction'
import ResultCards from '../components/ResultCards'
import DemandLetterModal from '../components/DemandLetterModal'
import { getConsultHelpers, SCENARIO_LABELS } from '../data/scenarios'
import { toUserMessage } from '../lib/errors'
import type { ConsultResponse, MediaRef, Scenario } from '../types'

function hasConsultInput(message: string, media: MediaRef[]): boolean {
  return message.trim().length > 0 || media.length > 0
}

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
  const { jurisdiction } = useJurisdiction()
  const helpers = getConsultHelpers(scenario)

  useEffect(() => {
    if (configured && !user && !authLoading) {
      signInGuest().catch(() =>
        setError("We couldn't sign you in securely. Please refresh the page and try again."),
      )
    }
  }, [configured, user, authLoading, signInGuest])

  useEffect(() => {
    if (!user) return
    fetchConsultationHistory()
      .then(setHistory)
      .catch(() => setHistory([]))
  }, [user, result])

  const addMedia = (m: MediaRef) => setMedia((prev) => [...prev, m])
  const removeMedia = (index: number) => setMedia((prev) => prev.filter((_, i) => i !== index))

  const submit = async () => {
    if (!hasConsultInput(message, media)) {
      setError(helpers.emptyInputHint)
      return
    }
    if (configured && !user) {
      setError('Starting secure session… try again in a moment.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const res = await consult(message.trim(), scenario, media, jurisdiction)
      setResult(res)
    } catch (e) {
      setError(toUserMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const canSubmit = hasConsultInput(message, media) && !loading

  const demandLetterFacts =
    message.trim() || result?.summary || 'See consultation summary and attached evidence.'

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8 sm:py-10 w-full min-w-0">
      <h1 className="font-display text-2xl sm:text-3xl mb-2">Legal consultation</h1>
      <p className="text-legally-navy/70 mb-8 text-sm max-w-2xl">{helpers.intro}</p>

      <div className="flex flex-wrap gap-2 mb-2">
        {SCENARIO_LABELS.map((s) => (
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
      <p className="text-xs text-legally-navy/50 mb-6">
        Selected: <span className="font-medium text-legally-navy">{helpers.shortTitle}</span>
        {' · '}
        Use text, voice, or uploads, together or separately.
      </p>

      <div className="grid lg:grid-cols-2 gap-8">
        <div className="space-y-4">
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            rows={6}
            placeholder={helpers.textPlaceholder}
            className="w-full rounded-xl border border-legally-navy/15 bg-white px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-legally-gold/50"
          />
          <UploadZone onUploaded={addMedia} disabled={loading} hint={helpers.uploadHint} />
          <VoiceRecorder onRecorded={addMedia} disabled={loading} />

          <AttachedMediaList items={media} onRemove={removeMedia} />

          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="button"
            onClick={submit}
            disabled={!canSubmit}
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
          {!hasConsultInput(message, media) && (
            <p className="text-xs text-legally-navy/50 text-center">{helpers.emptyInputHint}</p>
          )}
        </div>

        <div>
          {result ? (
            <>
              {result.demandLetterEligible && (
                <div className="mb-4 flex flex-col sm:flex-row gap-2">
                  <button
                    type="button"
                    onClick={() => setLetterOpen(true)}
                    className="flex-1 rounded-lg border-2 border-legally-gold text-legally-navy font-semibold py-2 text-sm hover:bg-legally-gold/10"
                  >
                    Generate demand letter
                  </button>
                </div>
              )}
              <ResultCards result={result} />
            </>
          ) : (
            <div className="rounded-xl border border-dashed border-legally-navy/20 bg-white/50 p-12 text-center text-legally-navy/50 text-sm">
              {helpers.resultsPlaceholder}
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
        facts={demandLetterFacts}
        open={letterOpen}
        onClose={() => setLetterOpen(false)}
        jurisdiction={{
          countryCode: jurisdiction.countryCode,
          countryName: jurisdiction.countryName,
          regionCode: jurisdiction.regionCode,
          regionName: jurisdiction.regionName,
        }}
      />
    </div>
  )
}
