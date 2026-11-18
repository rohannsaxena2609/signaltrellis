import type { AuthResponse } from './types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')
const SESSION_KEY = 'signaltrellis.session.v1'

type StoredSession = AuthResponse & { expiresAt: number }
let inMemorySession: StoredSession | null = null

function currentMemorySession(): StoredSession | null {
  if (!inMemorySession || !Number.isFinite(inMemorySession.expiresAt) || inMemorySession.expiresAt <= Date.now()) {
    inMemorySession = null
    return null
  }
  return inMemorySession
}

function removeStoredSession(): void {
  try {
    sessionStorage.removeItem(SESSION_KEY)
  } catch {
    // Storage may be unavailable in restricted browser contexts.
  }
}

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message)
    this.name = 'ApiError'
  }
}

export function readSession(): AuthResponse | null {
  let raw: string | null
  try {
    raw = sessionStorage.getItem(SESSION_KEY)
  } catch {
    return currentMemorySession()
  }

  if (!raw) return currentMemorySession()

  try {
    const session = JSON.parse(raw) as StoredSession
    if (!session.accessToken || !Number.isFinite(session.expiresAt) || session.expiresAt <= Date.now()) {
      removeStoredSession()
      inMemorySession = null
      return null
    }
    inMemorySession = session
    return session
  } catch {
    removeStoredSession()
    inMemorySession = null
    return null
  }
}

export function saveSession(session: AuthResponse): void {
  const storedSession: StoredSession = {
    ...session,
    expiresAt: Date.now() + session.expiresInSeconds * 1000,
  }
  inMemorySession = storedSession
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(storedSession))
  } catch {
    // Keep the session for this tab; it will be lost on reload.
  }
}

export function clearSession(): void {
  inMemorySession = null
  removeStoredSession()
}

type RequestOptions = Omit<RequestInit, 'body'> & { body?: BodyInit | object | null }

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const session = readSession()
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (session?.accessToken) headers.set('Authorization', `Bearer ${session.accessToken}`)

  let body: BodyInit | null | undefined
  if (options.body instanceof FormData || options.body instanceof Blob || typeof options.body === 'string') {
    body = options.body
  } else if (options.body != null) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.body)
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers, body })
  if (response.status === 401) {
    clearSession()
    window.dispatchEvent(new CustomEvent('signaltrellis:unauthorized'))
  }
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const payload = await response.json() as { message?: string; detail?: string }
      message = payload.message ?? payload.detail ?? message
    } catch { /* Keep the status-based message. */ }
    throw new ApiError(message, response.status)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const post = <T>(path: string, body?: unknown, headers?: HeadersInit) =>
  api<T>(path, { method: 'POST', body: body as RequestOptions['body'], headers })

export const get = <T>(path: string) => api<T>(path)

export const patch = <T>(path: string, body: unknown) => api<T>(path, { method: 'PATCH', body: body as RequestOptions['body'] })

export const del = <T>(path: string) => api<T>(path, { method: 'DELETE' })

export async function downloadBlob(path: string): Promise<Blob> {
  const session = readSession()
  const response = await fetch(`${API_BASE}${path}`, {
    headers: session?.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : {},
  })
  if (response.status === 401) {
    clearSession()
    window.dispatchEvent(new CustomEvent('signaltrellis:unauthorized'))
  }
  if (!response.ok) throw new ApiError(`File could not be opened (${response.status})`, response.status)
  return response.blob()
}

export function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) if (value !== undefined && value !== '') search.set(key, String(value))
  const encoded = search.toString()
  return encoded ? `?${encoded}` : ''
}
