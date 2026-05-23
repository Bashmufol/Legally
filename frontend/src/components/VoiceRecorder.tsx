import { useRef, useState } from 'react'
import { Mic, Square, X } from 'lucide-react'
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
  const mediaStream = useRef<MediaStream | null>(null)
  const chunks = useRef<Blob[]>([])
  const cancelled = useRef(false)

  const cleanupStream = () => {
    mediaStream.current?.getTracks().forEach((t) => t.stop())
    mediaStream.current = null
    mediaRecorder.current = null
  }

  const start = async () => {
    if (disabled) return
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaStream.current = stream
      const recorder = new MediaRecorder(stream)
      chunks.current = []
      cancelled.current = false

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunks.current.push(e.data)
      }
      recorder.onstop = async () => {
        cleanupStream()
        if (cancelled.current) {
          setStatus(null)
          return
        }
        const blob = new Blob(chunks.current, { type: 'audio/webm' })
        if (blob.size === 0) {
          setStatus(null)
          return
        }
        setStatus('Uploading voice note…')
        try {
          const file = new File([blob], `voice-${Date.now()}.webm`, { type: 'audio/webm' })
          const res = await uploadFile(file)
          onRecorded({
            url:
              res.storageType === 'local' && res.url.startsWith('/')
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
      setStatus('Recording… tap Stop to save or Cancel to discard')
    } catch {
      setStatus('Microphone access denied')
    }
  }

  const stop = () => {
    cancelled.current = false
    mediaRecorder.current?.stop()
    setRecording(false)
  }

  const cancel = () => {
    cancelled.current = true
    chunks.current = []
    if (mediaRecorder.current?.state === 'recording') {
      mediaRecorder.current.stop()
    } else {
      cleanupStream()
    }
    setRecording(false)
    setStatus(null)
  }

  return (
    <div className="flex flex-wrap items-center gap-3">
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
        <>
          <button
            type="button"
            onClick={stop}
            className="inline-flex items-center gap-2 rounded-lg bg-red-700 px-4 py-2 text-sm text-white hover:bg-red-800"
          >
            <Square className="w-4 h-4" />
            Stop & attach
          </button>
          <button
            type="button"
            onClick={cancel}
            className="inline-flex items-center gap-2 rounded-lg border border-legally-navy/25 bg-white px-4 py-2 text-sm text-legally-navy hover:bg-legally-navy/5"
          >
            <X className="w-4 h-4" />
            Cancel
          </button>
        </>
      )}
      {status && <span className="text-xs text-legally-navy/60">{status}</span>}
    </div>
  )
}
