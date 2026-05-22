import { useRef, useState } from 'react'
import { Mic, Square } from 'lucide-react'
import { uploadFile } from '../api/client'
import type { MediaRef } from '../types'

interface Props {
  onRecorded: (media: MediaRef) => void
  disabled?: boolean
}

export default function VoiceRecorder({ onRecorded, disabled }: Props) {
  const [recording, setRecording] = useState(false)
  const [status, setStatus] = useState<string | null>(null)
  const mediaRecorder = useRef<MediaRecorder | null>(null)
  const chunks = useRef<Blob[]>([])

  const start = async () => {
    if (disabled) return
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const recorder = new MediaRecorder(stream)
      chunks.current = []
      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunks.current.push(e.data)
      }
      recorder.onstop = async () => {
        stream.getTracks().forEach((t) => t.stop())
        const blob = new Blob(chunks.current, { type: 'audio/webm' })
        setStatus('Uploading voice note…')
        try {
          const file = new File([blob], `voice-${Date.now()}.webm`, { type: 'audio/webm' })
          const res = await uploadFile(file)
          onRecorded({
            url: res.storageType === 'local' && res.url.startsWith('/')
              ? `${import.meta.env.VITE_API_URL || 'http://localhost:8080'}${res.url}`
              : res.url,
            mimeType: res.mimeType,
            storageType: res.storageType,
          })
          setStatus('Voice note attached')
        } catch {
          setStatus('Failed to upload recording')
        }
      }
      mediaRecorder.current = recorder
      recorder.start()
      setRecording(true)
      setStatus('Recording…')
    } catch {
      setStatus('Microphone access denied')
    }
  }

  const stop = () => {
    mediaRecorder.current?.stop()
    setRecording(false)
  }

  return (
    <div className="flex items-center gap-3">
      {!recording ? (
        <button
          type="button"
          onClick={start}
          disabled={disabled}
          className="inline-flex items-center gap-2 rounded-lg bg-legally-navy px-4 py-2 text-sm text-white hover:bg-legally-navy/90 disabled:opacity-50"
        >
          <Mic className="w-4 h-4" />
          Record voice
        </button>
      ) : (
        <button
          type="button"
          onClick={stop}
          className="inline-flex items-center gap-2 rounded-lg bg-red-700 px-4 py-2 text-sm text-white"
        >
          <Square className="w-4 h-4" />
          Stop
        </button>
      )}
      {status && <span className="text-xs text-legally-navy/60">{status}</span>}
    </div>
  )
}
