import { useEffect, useState } from 'react'
import { clearSession, get, post, readSession, saveSession, ApiError } from './api'
import { AssistantPage } from './pages/AssistantPage'
import { CommitmentsPage } from './pages/CommitmentsPage'
import { ContextPage } from './pages/ContextPage'
import { DashboardPage } from './pages/DashboardPage'
import { DocumentsPage } from './pages/DocumentsPage'
import { SignalsPage } from './pages/SignalsPage'
import { ApprovalsPage } from './pages/ApprovalsPage'
import type { AuthResponse, Profile } from './types'
import { AuthScreen } from './ui/AuthScreen'
import { AppShell, type ViewKey } from './ui/AppShell'

export default function App() {
  const [session, setSession] = useState<AuthResponse | null>(() => readSession())
  const [profile, setProfile] = useState<Profile | null>(null)
  const [view, setView] = useState<ViewKey>('overview')
  const [notice, setNotice] = useState('')
  const [documentSearch, setDocumentSearch] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    const unauthorized = () => {
      clearSession()
      setSession(null)
      setProfile(null)
    }
    window.addEventListener('signaltrellis:unauthorized', unauthorized)
    return () => window.removeEventListener('signaltrellis:unauthorized', unauthorized)
  }, [])

  useEffect(() => {
    if (!session) return
    let active = true
    get<Profile>('/auth/me').then((result) => {
      if (active) setProfile(result)
    }).catch((error: unknown) => {
      if (error instanceof ApiError && error.status === 401) return
      if (active) setNotice('Your profile could not be loaded. Some pages may be unavailable.')
    })
    const timeout = window.setTimeout(() => {
      clearSession()
      setSession(null)
      setProfile(null)
    }, Math.max(0, session.expiresInSeconds * 1000 - 500))
    return () => {
      active = false
      window.clearTimeout(timeout)
    }
  }, [session])

  async function signIn(email: string, password: string, displayName?: string) {
    const path = displayName ? '/auth/register' : '/auth/login'
    const result = await post<AuthResponse>(path, displayName ? { email, password, displayName } : { email, password })
    saveSession(result)
    setSession(result)
    setProfile({ userId: result.userId, workspaceId: result.workspaceId, email: result.email })
  }

  function signOut() {
    clearSession()
    setSession(null)
    setProfile(null)
    setView('overview')
  }

  function navigate(next: ViewKey) {
    setView(next)
    setNotice('')
  }

  if (!session) return <AuthScreen onSubmit={signIn} />

  const page = (() => {
    switch (view) {
      case 'overview': return <DashboardPage refreshKey={refreshKey} navigate={navigate} />
      case 'signals': return <SignalsPage refreshKey={refreshKey} onChanged={() => setRefreshKey((value) => value + 1)} />
      case 'documents': return <DocumentsPage refreshKey={refreshKey} query={documentSearch} onQueryChange={setDocumentSearch} onChanged={() => setRefreshKey((value) => value + 1)} />
      case 'commitments': return <CommitmentsPage refreshKey={refreshKey} onChanged={() => setRefreshKey((value) => value + 1)} />
      case 'context': return <ContextPage />
      case 'assistant': return <AssistantPage />
      case 'approvals': return <ApprovalsPage refreshKey={refreshKey} onChanged={() => setRefreshKey((value) => value + 1)} />
    }
  })()

  return (
    <AppShell
      view={view}
      email={profile?.email ?? session.email}
      notice={notice}
      onNavigate={navigate}
      onSignOut={signOut}
      onSearch={(value) => { setDocumentSearch(value); navigate('documents') }}
      onQuickUpload={() => navigate('documents')}
      onQuickCommitment={() => navigate('commitments')}
      onRefresh={() => setRefreshKey((value) => value + 1)}
    >
      {page}
    </AppShell>
  )
}
