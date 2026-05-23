import { useState } from 'react'
import { BookOpen, ListOrdered, Phone, ScrollText, ChevronDown } from 'lucide-react'
import type { ConsultResponse } from '../types'
import Disclaimer from './Disclaimer'

export default function ResultCards({ result }: { result: ConsultResponse }) {
  const [sourcesOpen, setSourcesOpen] = useState(false)

  return (
    <div className="space-y-4">
      <Card icon={<ScrollText className="w-5 h-5 text-legally-gold" />} title="Summary">
        <p className="text-sm leading-relaxed">{result.summary}</p>
        <p className="mt-2 text-xs text-legally-navy/50">
          Confidence: <span className="capitalize font-medium">{result.confidence}</span>
          {result.jurisdictionCountry && (
            <>
              {' '}
              · Jurisdiction: {result.jurisdictionCountry}
              {result.jurisdictionRegion ? `, ${result.jurisdictionRegion}` : ''}
              {result.locationSource === 'input_override' ? ' (from your input)' : result.locationSource === 'device' ? ' (from device location)' : ''}
            </>
          )}
        </p>
        {result.corpusLimited && (
          <p className="mt-1 text-xs text-amber-800">
            Limited local law corpus for this jurisdiction — verify with a licensed lawyer.
          </p>
        )}
      </Card>

      <Card icon={<BookOpen className="w-5 h-5 text-legally-gold" />} title="What the law says">
        <ul className="space-y-4">
          {result.legalAnalysis.map((item, i) => (
            <li key={i} className="text-sm border-l-2 border-legally-gold pl-3">
              <p>{item.point}</p>
              <p className="mt-1 text-xs text-legally-navy/60">
                {item.citation.instrument} — {item.citation.section} ({item.citation.jurisdiction})
              </p>
            </li>
          ))}
        </ul>
      </Card>

      <Card icon={<ListOrdered className="w-5 h-5 text-legally-gold" />} title="Legal steps">
        <ol className="list-decimal list-inside space-y-2 text-sm">
          {result.steps.map((step, i) => (
            <li key={i}>{step}</li>
          ))}
        </ol>
      </Card>

      {result.contacts.length > 0 && (
        <Card icon={<Phone className="w-5 h-5 text-legally-gold" />} title="Who to contact">
          <div className="grid gap-3 sm:grid-cols-2">
            {result.contacts.map((c) => (
              <div
                key={c.id}
                className="rounded-lg bg-legally-cream border border-legally-navy/10 p-3 text-sm"
              >
                <p className="font-semibold">{c.name}</p>
                <p className="text-xs text-legally-navy/60">{c.role}</p>
                {c.phones?.length > 0 && (
                  <p className="mt-2 font-medium text-legally-navy">
                    {c.phones.join(' · ')}
                  </p>
                )}
                {Object.keys(c.social || {}).length > 0 && (
                  <p className="mt-1 text-xs text-legally-navy/50">
                    {Object.entries(c.social)
                      .map(([k, v]) => `${k}: ${v}`)
                      .join(' · ')}
                  </p>
                )}
                <p className="mt-2 text-xs text-legally-navy/70">{c.notes}</p>
              </div>
            ))}
          </div>
        </Card>
      )}

      {result.sources.length > 0 && (
        <div className="rounded-xl bg-white border border-legally-navy/10 overflow-hidden">
          <button
            type="button"
            onClick={() => setSourcesOpen(!sourcesOpen)}
            className="w-full flex items-center justify-between px-4 py-3 text-sm font-semibold"
          >
            Sources from Legally corpus
            <ChevronDown
              className={`w-4 h-4 transition ${sourcesOpen ? 'rotate-180' : ''}`}
            />
          </button>
          {sourcesOpen && (
            <ul className="px-4 pb-4 space-y-2 text-xs text-legally-navy/70 border-t">
              {result.sources.map((s) => (
                <li key={s.id}>
                  <strong>{s.title}</strong> — {s.instrument} {s.section}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      <Disclaimer />
    </div>
  )
}

function Card({
  icon,
  title,
  children,
}: {
  icon: React.ReactNode
  title: string
  children: React.ReactNode
}) {
  return (
    <div className="rounded-xl bg-white border border-legally-navy/10 shadow-sm overflow-hidden">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-legally-navy/5 bg-legally-navy/[0.02]">
        {icon}
        <h3 className="font-semibold text-sm">{title}</h3>
      </div>
      <div className="px-4 py-4">{children}</div>
    </div>
  )
}
