import { FileAudio, FileImage, FileText, FileVideo, X } from 'lucide-react'
import type { MediaRef } from '../types'

function mediaLabel(mime: string): string {
  if (mime.startsWith('audio/')) return 'Voice / audio'
  if (mime.startsWith('video/')) return 'Video'
  if (mime.startsWith('image/')) return 'Image'
  if (mime === 'application/pdf') return 'PDF'
  return 'File'
}

function MediaIcon({ mime }: { mime: string }) {
  if (mime.startsWith('audio/')) return <FileAudio className="w-4 h-4 shrink-0" />
  if (mime.startsWith('video/')) return <FileVideo className="w-4 h-4 shrink-0" />
  if (mime.startsWith('image/')) return <FileImage className="w-4 h-4 shrink-0" />
  return <FileText className="w-4 h-4 shrink-0" />
}

interface Props {
  items: MediaRef[]
  onRemove: (index: number) => void
}

export default function AttachedMediaList({ items, onRemove }: Props) {
  if (items.length === 0) return null

  return (
    <ul className="space-y-2">
      {items.map((item, index) => (
        <li
          key={`${item.url}-${index}`}
          className="flex items-center gap-2 rounded-lg bg-legally-cream border border-legally-navy/10 px-3 py-2 text-sm"
        >
          <MediaIcon mime={item.mimeType} />
          <span className="flex-1 truncate text-legally-navy/80">{mediaLabel(item.mimeType)}</span>
          <button
            type="button"
            onClick={() => onRemove(index)}
            className="text-legally-navy/40 hover:text-red-600"
            aria-label="Remove attachment"
          >
            <X className="w-4 h-4" />
          </button>
        </li>
      ))}
    </ul>
  )
}
