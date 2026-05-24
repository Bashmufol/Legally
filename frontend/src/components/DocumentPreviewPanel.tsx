import { Copy, Download, FileText, RotateCcw } from 'lucide-react'
import { downloadDocumentPdf } from '../lib/pdf'
import type { LegalDocumentResponse } from '../types'

interface Props {
  document: LegalDocumentResponse | null
  loading: boolean
  onRegenerate?: () => void
}

export default function DocumentPreviewPanel({ document, loading, onRegenerate }: Props) {
  if (loading) {
    return (
      <div className="rounded-xl border border-dashed border-legally-navy/20 bg-white/50 p-12 text-center text-sm text-legally-navy/50">
        Drafting your document…
      </div>
    )
  }

  if (!document) {
    return (
      <div className="rounded-xl border border-dashed border-legally-navy/20 bg-white/50 p-12 text-center text-sm text-legally-navy/50">
        <FileText className="w-10 h-10 mx-auto mb-3 text-legally-gold/60" />
        Select a document type, describe your needs, and generate a preview here.
      </div>
    )
  }

  const downloadPdf = () => {
    downloadDocumentPdf(
      document.title,
      document.content + '\n\n---\n' + document.disclaimer,
      `legally-${document.documentType.toLowerCase()}`,
    )
  }

  return (
    <div className="rounded-xl border border-legally-navy/10 bg-white shadow-sm overflow-hidden flex flex-col max-h-[calc(100vh-12rem)]">
      <div className="px-5 py-4 border-b border-legally-navy/10 bg-legally-cream/40">
        <h2 className="font-display text-lg font-semibold text-legally-navy">{document.title}</h2>
        <p className="text-xs text-legally-navy/60 mt-1">
          Jurisdiction: {document.jurisdictionCountry}
          {document.jurisdictionRegion ? `, ${document.jurisdictionRegion}` : ''}
          {document.locationSource === 'input_override' ? ' · from your description' : document.locationSource === 'device' ? ' · from device location' : ''}
        </p>
      </div>

      <div className="flex-1 overflow-y-auto p-5">
        <pre className="text-xs whitespace-pre-wrap font-sans leading-relaxed text-legally-navy/90">
          {document.content}
        </pre>
        <p className="mt-6 text-xs text-legally-navy/50 border-t pt-4">{document.disclaimer}</p>
      </div>

      <div className="px-5 py-4 border-t border-legally-navy/10 flex flex-wrap gap-2 bg-white">
        {onRegenerate && (
          <button
            type="button"
            onClick={onRegenerate}
            className="inline-flex items-center gap-2 rounded-lg border border-legally-navy/20 px-4 py-2 text-sm font-medium hover:bg-legally-cream/50"
          >
            <RotateCcw className="w-4 h-4" />
            Regenerate
          </button>
        )}
        <button
          type="button"
          onClick={() => navigator.clipboard.writeText(document.content)}
          className="inline-flex items-center gap-2 rounded-lg border border-legally-navy/20 px-4 py-2 text-sm font-medium hover:bg-legally-cream/50"
        >
          <Copy className="w-4 h-4" />
          Copy
        </button>
        <button
          type="button"
          onClick={downloadPdf}
          className="inline-flex items-center gap-2 rounded-lg bg-legally-navy text-white px-4 py-2 text-sm font-semibold hover:opacity-90 ml-auto"
        >
          <Download className="w-4 h-4" />
          Download PDF
        </button>
      </div>
    </div>
  )
}
