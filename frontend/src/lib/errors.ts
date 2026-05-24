export class UserFacingError extends Error {
  readonly userMessage: string

  constructor(userMessage: string) {
    super(userMessage)
    this.name = 'UserFacingError'
    this.userMessage = userMessage
  }
}

const GENERIC = 'Something went wrong. Please try again in a moment.'
const NETWORK =
  "We couldn't reach Legally right now. Check your internet connection and try again."
const SESSION = 'Your session could not be verified. Please refresh the page and try again.'
const AI_UNAVAILABLE =
  'Our legal assistant is temporarily unavailable. Please try again in a few minutes.'
const SERVER = 'Something went wrong on our servers. Please try again shortly.'

export function toUserMessage(error: unknown): string {
  if (error instanceof UserFacingError) {
    return error.userMessage
  }
  if (error instanceof Error) {
    return humanizeMessage(error.message)
  }
  if (typeof error === 'string') {
    return humanizeMessage(error)
  }
  return GENERIC
}

export function messageForHttpStatus(status: number): string {
  switch (status) {
    case 400:
      return 'Please check your input and try again.'
    case 401:
      return SESSION
    case 403:
      return "You don't have permission to do that. Try refreshing the page."
    case 404:
      return "We couldn't find what you were looking for."
    case 408:
      return 'The request took too long. Please try again.'
    case 413:
      return 'That file is too large. Please use a file under 50 MB.'
    case 429:
      return "You're doing that too often. Please wait a moment and try again."
    case 502:
    case 503:
    case 504:
      return 'Legally is briefly unavailable. Please try again in a minute.'
    case 500:
    default:
      return status >= 500 ? SERVER : GENERIC
  }
}

export function humanizeMessage(raw: string | null | undefined): string {
  if (!raw || !raw.trim()) {
    return GENERIC
  }

  let text = raw.trim()

  if (text.startsWith('{') && text.endsWith('}')) {
    try {
      const parsed = JSON.parse(text) as Record<string, unknown>
      const inner =
        (typeof parsed.error === 'string' && parsed.error) ||
        (typeof parsed.message === 'string' && parsed.message) ||
        (typeof parsed.detail === 'string' && parsed.detail)
      if (inner) {
        text = inner
      }
    } catch {
      return GENERIC
    }
  }

  const lower = text.toLowerCase()

  if (
    lower.includes('failed to fetch') ||
    lower.includes('networkerror') ||
    lower.includes('network request failed') ||
    lower.includes('cannot reach the api') ||
    lower.includes('load failed') ||
    lower.includes('err_connection') ||
    lower.includes('cors')
  ) {
    return NETWORK
  }

  if (
    lower.includes('unauthorized') ||
    lower.includes('invalid firebase') ||
    lower.includes('authorization required') ||
    lower.includes('firebase token') ||
    lower.includes('sign in failed')
  ) {
    return SESSION
  }

  if (
    lower.includes('gemini') ||
    lower.includes('generativelanguage') ||
    lower.includes('api key') ||
    lower.includes('api_key')
  ) {
    return AI_UNAVAILABLE
  }

  if (lower.includes('max upload') || lower.includes('file too large') || lower.includes('size exceeded')) {
    return 'That file is too large. Please choose a smaller file (under 50 MB).'
  }
  if (lower.includes('multipart') || lower.includes('not a multipart')) {
    return "We couldn't upload that file. Please try a different file format."
  }

  if (lower.includes('must not be blank') || lower.includes('must not be empty')) {
    return 'Please fill in all required fields.'
  }
  if (lower.includes('validation failed')) {
    return 'Please check your input and try again.'
  }

  if (looksTechnical(text)) {
    return GENERIC
  }

  if (text.length > 280) {
    return GENERIC
  }

  return text
}

function looksTechnical(text: string): boolean {
  const lower = text.toLowerCase()
  const patterns = [
    'exception',
    'stacktrace',
    'stack trace',
    'java.',
    'org.springframework',
    'nullpointer',
    'illegalargument',
    'localhost:',
    '127.0.0.1',
    'vite_',
    'http://',
    'https://',
    '.run.app',
    'port 8080',
    'sql',
    'jdbc',
    'hibernate',
    'firebase console',
    '.env',
    'gemini_model',
    'statuscode',
    'internal server error',
    'unexpected token',
    'syntaxerror',
    'typeerror:',
    'at com.',
    'at org.',
    'cause:',
    '{',
    '}',
  ]
  return patterns.some((p) => lower.includes(p))
}

export async function parseApiErrorResponse(res: Response): Promise<string> {
  const statusMessage = messageForHttpStatus(res.status)
  const contentType = res.headers.get('content-type') ?? ''

  if (!contentType.includes('application/json')) {
    try {
      const text = (await res.text()).trim()
      if (!text) {
        return statusMessage
      }
      return humanizeMessage(text)
    } catch {
      return statusMessage
    }
  }

  try {
    const data = (await res.json()) as Record<string, unknown>
    const raw =
      (typeof data.error === 'string' && data.error) ||
      (typeof data.message === 'string' && data.message) ||
      (typeof data.detail === 'string' && data.detail)
    if (raw) {
      return humanizeMessage(raw)
    }
  } catch {
    /* ignore */
  }

  return statusMessage
}
