import { useCallback, useState } from 'react'
import { FileUp, X } from 'lucide-react'
import { uploadFile } from '../api/client'
import type { MediaRef } from '../types'

interface Props {
  onUploaded: (media: MediaRef) => void
  disabled?: boolean
}

export default function UploadZone({ onUploaded, disabled }: Props) {
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fileName, setFileName] = useState<string | null>(null)

  const handleFiles = useCallback(
    async (files: FileList | null) => {
      if (!files?.length || disabled) return
      setUploading(true)
      setError(null)
      try {
        const file = files[0]
        const res = await uploadFile(file)
        setFileName(res.fileName)
        onUploaded({
          url: res.url,
          mimeType: res.mimeType,
          storageType: res.storageType,
        })
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Upload failed')
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
        <span className="text-xs text-legally-navy/50">Max 50MB · Stored via Google Cloud or local</span>
        <input
          type="file"
          className="hidden"
          accept="image/*,application/pdf,audio/*,video/*"
          disabled={disabled || uploading}
          onChange={(e) => handleFiles(e.target.files)}
        />
      </label>
      {fileName && (
        <div className="flex items-center gap-2 text-sm text-green-800 bg-green-50 rounded-lg px-3 py-2">
          <span className="flex-1 truncate">Attached: {fileName}</span>
          <button
            type="button"
            onClick={() => setFileName(null)}
            className="text-legally-navy/50 hover:text-legally-navy"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
