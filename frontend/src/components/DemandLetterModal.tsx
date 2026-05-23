import { useState } from 'react'
import { Loader2, X } from 'lucide-react'
import { generateLegalDocument } from '../api/client'
import { downloadDocumentPdf } from '../lib/pdf'
import { toUserMessage } from '../lib/errors'
import type { JurisdictionFields, LegalDocumentResponse } from '../types'

interface Props {
  facts: string
  open: boolean
  onClose: () => void
  jurisdiction?: JurisdictionFields
}

export default function DemandLetterModal({ facts, open, onClose, jurisdiction }: Props) {
  const [preview, setPreview] = useState<LegalDocumentResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [partyA, setPartyA] = useState('')
  const [partyB, setPartyB] = useState('')

  if (!open) return null

  const handleGenerate = async () => {
    setLoading(true)
    try {
      const res = await generateLegalDocument(
        {
          documentType: 'DEMAND_LETTER',
          facts,
          partyAName: partyA || undefined,
          partyBName: partyB || undefined,
        },
        jurisdiction
          ? {
              countryCode: jurisdiction.countryCode ?? 'INT',
              countryName: jurisdiction.countryName ?? 'International',
              regionCode: jurisdiction.regionCode ?? 'GENERAL',
              regionName: jurisdiction.regionName ?? 'General',
              locationSource:
                (jurisdiction.locationSource as 'device' | 'input_override' | 'default_fallback') ??
                'device',
            }
          : undefined,
      )
      setPreview(res)
    } catch (e) {
      setPreview({
        documentType: 'DEMAND_LETTER',
        title: 'Demand letter',
        content: toUserMessage(e),
        disclaimer: '',
      })
    } finally {
      setLoading(false)
    }
  }

  const downloadPdf = () => {
    if (!preview) return
    downloadDocumentPdf(
      preview.title,
      preview.content + '\n\n---\n' + preview.disclaimer,
      'legally-demand-letter',
    )
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col shadow-2xl">
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h2 className="font-display text-lg font-semibold">Demand letter</h2>
          <button type="button" onClick={onClose} className="text-legally-navy/50 hover:text-legally-navy">
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-6 space-y-4 overflow-y-auto flex-1">
          {!preview && (
            <>
              <div className="grid sm:grid-cols-2 gap-3">
                <input
                  placeholder="Your full name"
                  value={partyA}
                  onChange={(e) => setPartyA(e.target.value)}
                  className="rounded-lg border px-3 py-2 text-sm"
                />
                <input
                  placeholder="Recipient name"
                  value={partyB}
                  onChange={(e) => setPartyB(e.target.value)}
                  className="rounded-lg border px-3 py-2 text-sm"
                />
              </div>
              <button
                type="button"
                onClick={handleGenerate}
                disabled={loading}
                className="w-full rounded-lg bg-legally-gold text-legally-navy font-semibold py-3 hover:opacity-90 disabled:opacity-50 inline-flex items-center justify-center gap-2"
              >
                {loading && <Loader2 className="w-5 h-5 animate-spin" />}
                {loading ? 'Generating…' : 'Generate & preview'}
              </button>
            </>
          )}
          {preview && (
            <>
              <pre className="text-xs whitespace-pre-wrap bg-legally-cream rounded-lg p-4 max-h-64 overflow-y-auto">
                {preview.content}
              </pre>
              {preview.disclaimer && (
                <p className="text-xs text-legally-navy/50">{preview.disclaimer}</p>
              )}
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => navigator.clipboard.writeText(preview.content)}
                  className="flex-1 rounded-lg border py-2 text-sm font-medium"
                >
                  Copy
                </button>
                <button
                  type="button"
                  onClick={downloadPdf}
                  className="flex-1 rounded-lg bg-legally-navy text-white py-2 text-sm font-medium"
                >
                  Download PDF
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
