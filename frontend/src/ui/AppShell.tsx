import { useState, type ReactNode } from 'react'
import { Activity, BellRing, CalendarDays, ChevronDown, FileText, GitBranch, LayoutDashboard, LogOut, Menu, Plus, Search, Settings2, ShieldCheck, Sparkles, UploadCloud, X } from 'lucide-react'

export type ViewKey = 'overview' | 'signals' | 'documents' | 'commitments' | 'context' | 'assistant' | 'approvals'
type Props = { view: ViewKey; email: string; notice: string; children: ReactNode; onNavigate: (view: ViewKey) => void; onSignOut: () => void; onSearch: (value: string) => void; onQuickUpload: () => void; onQuickCommitment: () => void; onRefresh: () => void }

const primary: { key: ViewKey; label: string; icon: typeof LayoutDashboard }[] = [
  { key: 'overview', label: 'Overview', icon: LayoutDashboard }, { key: 'signals', label: 'Signals', icon: Activity },
  { key: 'documents', label: 'Documents', icon: FileText }, { key: 'commitments', label: 'Commitments', icon: CalendarDays },
  { key: 'context', label: 'Context graph', icon: GitBranch }, { key: 'assistant', label: 'AI assistant', icon: Sparkles },
]
const titles: Record<ViewKey, [string, string]> = { overview: ['Overview', 'Your life, with more context.'], signals: ['Signals', 'Understand what needs attention.'], documents: ['Documents', 'Keep source records connected.'], commitments: ['Commitments', 'Dates and obligations in one place.'], context: ['Context graph', 'See how your records relate.'], assistant: ['AI assistant', 'Ask questions grounded in your workspace.'], approvals: ['Approvals', 'You decide what happens next.'] }

export function AppShell({ view, email, notice, children, onNavigate, onSignOut, onSearch, onQuickUpload, onQuickCommitment, onRefresh }: Props) {
  const [mobileOpen, setMobileOpen] = useState(false)
  function go(next: ViewKey) { onNavigate(next); setMobileOpen(false) }
  return <div className="app-layout">
    {mobileOpen && <button className="mobile-scrim" aria-label="Close navigation" onClick={() => setMobileOpen(false)} />}
    <aside className={`app-sidebar ${mobileOpen ? 'app-sidebar--open' : ''}`}><div className="sidebar-brand"><span className="brand-mark"><BrandGlyph /></span><span>SignalTrellis<small>PERSONAL INTELLIGENCE</small></span><button className="mobile-close icon-button" onClick={() => setMobileOpen(false)} aria-label="Close navigation"><X size={18} /></button></div>
      <span className="nav-section-label">WORKSPACE</span><nav className="main-nav">{primary.map(({ key, label, icon: Icon }) => <button key={key} className={`nav-item ${view === key ? 'nav-item--active' : ''}`} onClick={() => go(key)}><Icon size={17} /><span>{label}</span>{key === 'signals' && <i className="nav-indicator" />}</button>)}</nav>
      <div className="sidebar-spacer" /><button className={`approval-nav ${view === 'approvals' ? 'approval-nav--active' : ''}`} onClick={() => go('approvals')}><span className="approval-nav-icon"><ShieldCheck size={16} /></span><span><strong>Approval queue</strong><small>Your decision required</small></span><ChevronDown size={14} className="approval-chevron" /></button>
      <div className="sidebar-private"><ShieldCheck size={14} /><span>PRIVATE WORKSPACE<small>Only you can access these records.</small></span></div><div className="sidebar-profile"><span className="profile-avatar">{email.slice(0, 1).toUpperCase()}</span><span className="profile-email">{email}<small>Personal workspace</small></span><button className="icon-button signout-button" onClick={onSignOut} title="Sign out" aria-label="Sign out"><LogOut size={16} /></button></div>
    </aside>
    <main className="app-main"><header className="topbar"><button className="mobile-menu icon-button" onClick={() => setMobileOpen(true)} aria-label="Open navigation"><Menu size={20} /></button><div className="breadcrumb"><span>Workspace</span><span>/</span><strong>{titles[view][0]}</strong></div><div className="topbar-actions"><label className="global-search"><Search size={15} /><input placeholder="Search your workspace" onKeyDown={(event) => { if (event.key === 'Enter') onSearch(event.currentTarget.value) }} aria-label="Search your documents" /><kbd>↵</kbd></label><button className="icon-button topbar-icon" onClick={onRefresh} title="Refresh workspace" aria-label="Refresh workspace"><Settings2 size={17} /></button><span className="topbar-avatar">{email.slice(0, 1).toUpperCase()}</span></div></header>
      <div className="main-content"><div className="page-heading"><div><span className="eyebrow">SIGNALTRELLIS WORKSPACE</span><h1>{titles[view][0]}</h1><p>{titles[view][1]}</p></div><div className="quick-actions"><button className="button button--quiet" onClick={onQuickCommitment}><Plus size={15} /> Add commitment</button><button className="button button--primary" onClick={onQuickUpload}><UploadCloud size={15} /> Add document</button></div></div>{notice && <div className="notice-banner"><BellRing size={14} />{notice}</div>}{children}<footer className="app-footer"><span>SignalTrellis <i>·</i> Personal context, connected.</span><span>Source-linked <ShieldCheck size={12} /> · Human approved</span></footer></div>
    </main>
  </div>
}

export function BrandGlyph({ light = false }: { light?: boolean }) { return <svg className={light ? 'brand-glyph brand-glyph--light' : 'brand-glyph'} viewBox="0 0 36 36" fill="none" aria-hidden="true"><path d="M18 3 31 10.5v15L18 33 5 25.5v-15L18 3Z" stroke="currentColor" strokeWidth="1.6"/><path d="m11 22 6.6-10 7.4 4.3M11 22l13 1M17.6 12 24 23" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round"/><circle cx="11" cy="22" r="2" fill="currentColor"/><circle cx="17.6" cy="12" r="2" fill="currentColor"/><circle cx="25" cy="16.3" r="2" fill="currentColor"/><circle cx="24" cy="23" r="2" fill="currentColor"/></svg> }
