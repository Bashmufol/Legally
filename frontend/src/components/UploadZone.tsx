import { useCallback, useState } from 'react'
import { FileUp } from 'lucide-react'
import { uploadFile } from '../api/client'
import { toUserMessage } from '../lib/errors'
import type { MediaRef } from '../types'

interface Props {
  onUploaded: (media: MediaRef) => void
  disabled?: boolean
  hint?: string
}

export default function UploadZone({ onUploaded, disabled, hint }: Props) {
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const handleFiles = useCallback(
    async (files: FileList | null) => {
      if (!files?.length || disabled) return
      setUploading(true)
      setError(null)
      try {
        const file = files[0]
        const res = await uploadFile(file)
        onUploaded({
          url: res.url,
          mimeType: res.mimeType,
          storageType: res.storageType,
        })
      } catch (e) {
        setError(toUserMessage(e))
      } finally {
        setUploading(false)
      }
    },
    [disabled, onUploaded],
  )

  return (
    <div className="space-y-2">
      <label
        className={`flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-legally-navy/20 bg-white px-4 py-8 cursor-pointer transition hover:border-legally-gold/60 ${
          disabled ? 'opacity-50 pointer-events-none' : ''
        }`}
      >
        <FileUp className="w-8 h-8 text-legally-gold" />
        <span className="text-sm font-medium text-legally-navy/80">
          {uploading ? 'Uploading…' : 'Upload image, PDF, audio, or video'}
        </span>
        <span className="text-xs text-legally-navy/50 text-center px-2">
          {hint ?? 'Max 50MB · image, PDF, audio, or video'}
        </span>
        <input
          type="file"
          className="hidden"
          accept="image/*,application/pdf,audio/*,video/*"
          disabled={disabled || uploading}
          onChange={(e) => handleFiles(e.target.files)}
        />
      </label>
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
