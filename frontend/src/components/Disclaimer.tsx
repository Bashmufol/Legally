export default function Disclaimer({ compact = false }: { compact?: boolean }) {
  if (compact) {
    return (
      <p className="text-xs text-center text-legally-navy/70 max-w-2xl mx-auto">
        Legally provides general legal information only — not legal advice. Always consult a
        licensed Nigerian lawyer.
      </p>
    )
  }
  return (
    <div className="rounded-lg border border-amber-300/60 bg-amber-50 px-4 py-3 text-sm text-amber-950">
      <strong>Important:</strong> Legally provides general legal information only, not legal
      advice. Laws change and outcomes depend on facts. Consult a licensed Nigerian lawyer before
      taking legal action.
    </div>
  )
}
