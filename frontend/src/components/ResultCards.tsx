import { useState } from 'react'
import { BookOpen, ExternalLink, ListOrdered, Phone, ScrollText, ChevronDown } from 'lucide-react'
import type { ConsultResponse, LawChunk } from '../types'
import { phoneTelHref, socialHref } from '../lib/links'
import Disclaimer from './Disclaimer'

function sourceUrlForPoint(
  item: ConsultResponse['legalAnalysis'][0],
  sources: LawChunk[],
): string | undefined {
  if (item.citation?.sourceUrl) return item.citation.sourceUrl
  if (item.chunkId) {
    const chunk = sources.find((s) => s.id === item.chunkId)
    if (chunk?.sourceUrl) return chunk.sourceUrl
  }
  const match = sources.find(
    (s) =>
      s.instrument === item.citation?.instrument && s.section === item.citation?.section,
  )
  return match?.sourceUrl
}

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
              {result.locationSource === 'input_override'
                ? ' (from your input)'
                : result.locationSource === 'device'
                  ? ' (from device location)'
                  : ''}
            </>
          )}
        </p>
      </Card>

      {result.legalAnalysis.length > 0 && (
      <Card icon={<BookOpen className="w-5 h-5 text-legally-gold" />} title="What the law says">
        <ul className="space-y-4">
          {result.legalAnalysis.map((item, i) => {
            const sourceUrl = sourceUrlForPoint(item, result.sources)
            return (
              <li key={i} className="text-sm border-l-2 border-legally-gold pl-3">
                <p>{item.point}</p>
                <p className="mt-1 text-xs text-legally-navy/60">
                  {sourceUrl ? (
                    <a
                      href={sourceUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-legally-gold font-medium hover:underline"
                    >
                      {item.citation.instrument}, {item.citation.section} (
                      {item.citation.jurisdiction})
                      <ExternalLink className="w-3 h-3 shrink-0" aria-hidden />
                    </a>
                  ) : (
                    <>
                      {item.citation.instrument}, {item.citation.section} (
                      {item.citation.jurisdiction})
                    </>
                  )}
                </p>
              </li>
            )
          })}
        </ul>
      </Card>
      )}

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
                {c.phones && c.phones.length > 0 && (
                  <p className="mt-2 flex flex-wrap gap-x-3 gap-y-1 font-medium text-legally-navy">
                    {c.phones.map((phone) => (
                      <a
                        key={phone}
                        href={phoneTelHref(phone)}
                        className="text-legally-gold hover:underline"
                      >
                        {phone}
                      </a>
                    ))}
                  </p>
                )}
                {c.emails && c.emails.length > 0 && (
                  <p className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-sm">
                    {c.emails.map((email) => (
                      <a
                        key={email}
                        href={`mailto:${email}`}
                        className="text-legally-gold hover:underline"
                      >
                        {email}
                      </a>
                    ))}
                  </p>
                )}
                {Object.keys(c.social || {}).length > 0 && (
                  <p className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs">
                    {Object.entries(c.social).map(([platform, value]) => (
                      <a
                        key={platform}
                        href={socialHref(platform, value)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-0.5 text-legally-gold font-medium hover:underline"
                      >
                        {platform}
                        <ExternalLink className="w-3 h-3" aria-hidden />
                      </a>
                    ))}
                  </p>
                )}
                {c.sourceUrl && (
                  <p className="mt-2 text-xs">
                    <a
                      href={c.sourceUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-legally-gold hover:underline inline-flex items-center gap-0.5"
                    >
                      Source
                      <ExternalLink className="w-3 h-3" aria-hidden />
                    </a>
                  </p>
                )}
                {c.notes && (
                  <p className="mt-2 text-xs text-legally-navy/70">{c.notes}</p>
                )}
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
            Official web sources
            <ChevronDown
              className={`w-4 h-4 transition ${sourcesOpen ? 'rotate-180' : ''}`}
            />
          </button>
          {sourcesOpen && (
            <ul className="px-4 pb-4 space-y-2 text-xs text-legally-navy/70 border-t">
              {result.sources.map((s) => (
                <li key={s.id}>
                  <strong>{s.title}</strong>:{' '}
                  {s.sourceUrl ? (
                    <a
                      href={s.sourceUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-legally-gold hover:underline inline-flex items-center gap-0.5"
                    >
                      {s.instrument} {s.section}
                      <ExternalLink className="w-3 h-3" aria-hidden />
                    </a>
                  ) : (
                    <>
                      {s.instrument} {s.section}
                    </>
                  )}
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
