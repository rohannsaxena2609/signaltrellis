import type { AuthResponse } from './types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')
const SESSION_KEY = 'signaltrellis.session.v1'

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message)
    this.name = 'ApiError'
  }
}

export function readSession(): AuthResponse | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as AuthResponse & { expiresAt: number }
    if (session.expiresAt <= Date.now()) {
      sessionStorage.removeItem(SESSION_KEY)
      return null
    }
    return session
  } catch {
    sessionStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function saveSession(session: AuthResponse): void {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify({ ...session, expiresAt: Date.now() + session.expiresInSeconds * 1000 }))
}

export function clearSession(): void {
  sessionStorage.removeItem(SESSION_KEY)
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
