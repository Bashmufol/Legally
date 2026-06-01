import { useEffect, useState } from 'react'
import { ChevronRight, History, Loader2, X } from 'lucide-react'
import { fetchConsultationHistory, fetchConsultationHistoryDetail } from '../api/client'
import { scenarioLabel } from '../data/scenarios'
import { formatConsultationTime } from '../lib/formatDate'
import { toUserMessage } from '../lib/errors'
import type { HistoryDetail, HistoryItem } from '../types'
import ResultCards from './ResultCards'

interface Props {
  refreshKey?: unknown
}

export default function ConsultHistorySection({ refreshKey }: Props) {
  const [items, setItems] = useState<HistoryItem[]>([])
  const [loadingList, setLoadingList] = useState(true)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<HistoryDetail | null>(null)
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoadingList(true)
    fetchConsultationHistory()
      .then(setItems)
      .catch(() => setItems([]))
      .finally(() => setLoadingList(false))
  }, [refreshKey])

  useEffect(() => {
    if (!selectedId) {
      setDetail(null)
      return
    }
    setLoadingDetail(true)
    setError(null)
    fetchConsultationHistoryDetail(selectedId)
      .then(setDetail)
      .catch((e) => {
        setDetail(null)
        setError(toUserMessage(e))
      })
      .finally(() => setLoadingDetail(false))
  }, [selectedId])

  if (loadingList) {
    return (
      <section className="mt-12 flex items-center gap-2 text-sm text-legally-navy/50">
        <Loader2 className="w-4 h-4 animate-spin" />
        Loading consultation history…
      </section>
    )
  }

  if (items.length === 0) {
    return null
  }

  return (
    <section className="mt-12">
      <h2 className="font-display text-xl mb-4 flex items-center gap-2">
        <History className="w-5 h-5 text-legally-gold" />
        Your recent consultations
        <span className="text-xs font-normal text-legally-navy/50">(up to 7)</span>
      </h2>

      <div className="grid lg:grid-cols-2 gap-6">
        <ul className="space-y-2">
          {items.map((h) => {
            const active = selectedId === h.id
            return (
              <li key={h.id}>
                <button
                  type="button"
                  onClick={() => setSelectedId(active ? null : h.id)}
                  className={`w-full text-left rounded-lg border p-4 text-sm transition ${
                    active
                      ? 'border-legally-gold bg-legally-gold/10'
                      : 'border-legally-navy/10 bg-white hover:border-legally-gold/50'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-legally-navy">{scenarioLabel(h.scenario)}</p>
                      <p className="text-legally-navy/80 mt-1 line-clamp-3">{h.question}</p>
                      <p className="text-xs text-legally-navy/45 mt-2">{formatConsultationTime(h.createdAt)}</p>
                    </div>
                    <ChevronRight
                      className={`w-4 h-4 shrink-0 text-legally-gold transition ${
                        active ? 'rotate-90' : ''
                      }`}
                    />
                  </div>
                </button>
              </li>
            )
          })}
        </ul>

        <div className="min-h-[12rem]">
          {!selectedId && (
            <p className="text-sm text-legally-navy/50 rounded-xl border border-dashed border-legally-navy/15 p-8 text-center">
              Select a consultation to view the full response.
            </p>
          )}
          {selectedId && loadingDetail && (
            <div className="flex items-center justify-center gap-2 text-sm text-legally-navy/50 py-12">
              <Loader2 className="w-5 h-5 animate-spin" />
              Loading details…
            </div>
          )}
          {selectedId && error && (
            <p className="text-sm text-red-600 rounded-lg border border-red-200 bg-red-50 p-4">{error}</p>
          )}
          {detail && !loadingDetail && (
            <div className="relative">
              <button
                type="button"
                onClick={() => setSelectedId(null)}
                className="absolute right-0 top-0 z-10 p-1 rounded-md text-legally-navy/50 hover:bg-legally-navy/5"
                aria-label="Close details"
              >
                <X className="w-4 h-4" />
              </button>
              <p className="text-xs text-legally-navy/50 mb-3 pr-8">
                {scenarioLabel(detail.scenario)} · {formatConsultationTime(detail.createdAt)}
              </p>
              <p className="text-sm font-medium text-legally-navy mb-4 border-l-2 border-legally-gold pl-3">
                {detail.question}
              </p>
              <ResultCards result={detail.response} />
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
