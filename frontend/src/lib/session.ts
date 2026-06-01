const SESSION_STORAGE_KEY = 'legally_session_id'

export const SESSION_HEADER = 'X-Legally-Session-Id'

export function getSessionId(): string {
  let id = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(SESSION_STORAGE_KEY, id)
  }
  return id
}

export function renewSessionId(): string {
  const id = crypto.randomUUID()
  localStorage.setItem(SESSION_STORAGE_KEY, id)
  return id
}
