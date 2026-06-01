import { useEffect, useState } from 'react'
import { Loader2, Sparkles } from 'lucide-react'
import { generateLegalDocument } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useJurisdiction } from '../hooks/useJurisdiction'
import { DOCUMENT_TYPES, type DocumentTypeId } from '../data/documentTypes'
import DocumentPreviewPanel from '../components/DocumentPreviewPanel'
import { toUserMessage } from '../lib/errors'
import type { LegalDocumentResponse } from '../types'

export default function DocumentsPage() {
  const [selectedType, setSelectedType] = useState<DocumentTypeId>('RENT_AGREEMENT')
  const [facts, setFacts] = useState('')
  const [additionalDetails, setAdditionalDetails] = useState('')
  const [customName, setCustomName] = useState('')
  const [partyA, setPartyA] = useState('')
  const [partyB, setPartyB] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [preview, setPreview] = useState<LegalDocumentResponse | null>(null)

  const { user, configured, loading: authLoading, signInGuest } = useAuth()
  const { jurisdiction } = useJurisdiction()

  const docMeta = DOCUMENT_TYPES.find((d) => d.id === selectedType) ?? DOCUMENT_TYPES[0]

  useEffect(() => {
    if (configured && !user && !authLoading) {
      signInGuest().catch(() =>
        setError("We couldn't sign you in securely. Please refresh the page and try again."),
      )
    }
  }, [configured, user, authLoading, signInGuest])

  const generate = async () => {
    if (!facts.trim()) {
      setError('Please describe the document you need (parties, terms, property, dates, etc.).')
      return
    }
    if (configured && !user) {
      setError('Starting secure session… try again shortly.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const res = await generateLegalDocument(
        {
          documentType: selectedType,
          facts,
          additionalDetails: additionalDetails || undefined,
          customDocumentName: selectedType === 'OTHER' ? customName : undefined,
          partyAName: partyA || undefined,
          partyBName: partyB || undefined,
        },
        jurisdiction,
      )
      setPreview(res)
    } catch (e) {
      setError(toUserMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const waitingForAuth = configured && !user && authLoading
  const canGenerate = facts.trim().length > 0 && !loading && !waitingForAuth

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8 sm:py-10 w-full min-w-0">
      <h1 className="font-display text-2xl sm:text-3xl mb-2">Legal documents</h1>
      <p className="text-legally-navy/70 mb-8 text-sm max-w-2xl">
        Generate contracts, agreements, and letters tailored to your jurisdiction. Preview the draft,
        then download as PDF. Mention a country or state in your description to override device
        location.
      </p>

      <div className="grid lg:grid-cols-2 gap-8">
        <div className="space-y-6">
          <div>
            <h2 className="text-sm font-semibold text-legally-navy mb-3">Document type</h2>
            <div className="grid sm:grid-cols-2 gap-2 max-h-64 overflow-y-auto pr-1">
              {DOCUMENT_TYPES.map((d) => (
                <button
                  key={d.id}
                  type="button"
                  onClick={() => {
                    setSelectedType(d.id)
                    setPreview(null)
                  }}
                  disabled={loading}
                  className={`text-left rounded-lg border px-3 py-2.5 text-sm transition disabled:opacity-50 disabled:cursor-not-allowed ${
                    selectedType === d.id
                      ? 'border-legally-gold bg-legally-gold/10 ring-1 ring-legally-gold/40'
                      : 'border-legally-navy/15 bg-white hover:border-legally-gold/50'
                  }`}
                >
                  <span className="font-medium block">{d.label}</span>
                  <span className="text-xs text-legally-navy/60 line-clamp-2">{d.description}</span>
                </button>
              ))}
            </div>
          </div>

          {selectedType === 'OTHER' && (
            <input
              placeholder="Document name (e.g. Loan agreement)"
              value={customName}
              onChange={(e) => setCustomName(e.target.value)}
              disabled={loading}
              className="w-full rounded-xl border border-legally-navy/15 bg-white px-4 py-3 text-sm disabled:opacity-50"
            />
          )}

          <textarea
            value={facts}
            onChange={(e) => setFacts(e.target.value)}
            rows={5}
            disabled={loading}
            placeholder={`Describe your ${docMeta.label.toLowerCase()}: parties, property, amounts, duration, special clauses…`}
            className="w-full rounded-xl border border-legally-navy/15 bg-white px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-legally-gold/50 disabled:opacity-50"
          />

          <textarea
            value={additionalDetails}
            onChange={(e) => setAdditionalDetails(e.target.value)}
            rows={3}
            disabled={loading}
            placeholder="Optional: extra clauses, governing preferences, deadlines…"
            className="w-full rounded-xl border border-legally-navy/15 bg-white px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-legally-gold/50 disabled:opacity-50"
          />

          <div className="grid sm:grid-cols-2 gap-3">
            <input
              placeholder={docMeta.partyALabel}
              value={partyA}
              onChange={(e) => setPartyA(e.target.value)}
              disabled={loading}
              className="rounded-xl border border-legally-navy/15 bg-white px-4 py-3 text-sm disabled:opacity-50"
            />
            <input
              placeholder={docMeta.partyBLabel}
              value={partyB}
              onChange={(e) => setPartyB(e.target.value)}
              disabled={loading}
              className="rounded-xl border border-legally-navy/15 bg-white px-4 py-3 text-sm disabled:opacity-50"
            />
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <button
            type="button"
            onClick={generate}
            disabled={!canGenerate}
            className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-legally-gold text-legally-navy font-semibold py-3 hover:opacity-90 disabled:opacity-50"
          >
            {loading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                Generating…
              </>
            ) : (
              <>
                <Sparkles className="w-5 h-5" />
                Generate & preview
              </>
            )}
          </button>
        </div>

        <DocumentPreviewPanel document={preview} loading={loading} onRegenerate={preview ? generate : undefined} />
      </div>
    </div>
  )
}
