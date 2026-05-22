import { useState } from 'react'
import { Download, X } from 'lucide-react'
import { generateDemandLetter } from '../api/client'

interface Props {
  facts: string
  open: boolean
  onClose: () => void
}

export default function DemandLetterModal({ facts, open, onClose }: Props) {
  const [letter, setLetter] = useState('')
  const [loading, setLoading] = useState(false)
  const [senderName, setSenderName] = useState('')
  const [recipientName, setRecipientName] = useState('')

  if (!open) return null

  const handleGenerate = async () => {
    setLoading(true)
    try {
      const res = await generateDemandLetter(facts, 'tenancy', senderName, recipientName)
      setLetter(res.letter)
    } catch (e) {
      setLetter(e instanceof Error ? e.message : 'Failed to generate letter')
    } finally {
      setLoading(false)
    }
  }

  const download = () => {
    const blob = new Blob([letter], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'legally-demand-letter.txt'
    a.click()
    URL.revokeObjectURL(url)
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
          <div className="grid sm:grid-cols-2 gap-3">
            <input
              placeholder="Your full name"
              value={senderName}
              onChange={(e) => setSenderName(e.target.value)}
              className="rounded-lg border px-3 py-2 text-sm"
            />
            <input
              placeholder="Landlord / recipient name"
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              className="rounded-lg border px-3 py-2 text-sm"
            />
          </div>
          {!letter && (
            <button
              type="button"
              onClick={handleGenerate}
              disabled={loading}
              className="w-full rounded-lg bg-legally-gold text-legally-navy font-semibold py-3 hover:opacity-90 disabled:opacity-50"
            >
              {loading ? 'Generating with Gemini…' : 'Generate letter'}
            </button>
          )}
          {letter && (
            <>
              <pre className="text-xs whitespace-pre-wrap bg-legally-cream rounded-lg p-4 max-h-64 overflow-y-auto">
                {letter}
              </pre>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => navigator.clipboard.writeText(letter)}
                  className="flex-1 rounded-lg border py-2 text-sm font-medium"
                >
                  Copy
                </button>
                <button
                  type="button"
                  onClick={download}
                  className="flex-1 inline-flex items-center justify-center gap-2 rounded-lg bg-legally-navy text-white py-2 text-sm font-medium"
                >
                  <Download className="w-4 h-4" />
                  Download
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
