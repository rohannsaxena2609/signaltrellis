import { useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  ArrowDownRight, ArrowRight, ArrowUpRight, CalendarDays, CheckCircle2, ChevronRight,
  CircleAlert, FileCheck2, FileText, GitBranch, ShieldCheck, Sparkles, WalletCards,
} from 'lucide-react'
import { get, query } from '../api'
import type { ActionProposal, Commitment, DocumentRecord, Entity, Page, Signal } from '../types'
import type { ViewKey } from '../ui/AppShell'

type Props = { refreshKey: number; navigate: (view: ViewKey) => void }

export function DashboardPage({ refreshKey, navigate }: Props) {
  const [signals, setSignals] = useState<Signal[]>([])
  const [commitments, setCommitments] = useState<Commitment[]>([])
  const [documents, setDocuments] = useState<DocumentRecord[]>([])
  const [entities, setEntities] = useState<Entity[]>([])
  const [signalCount, setSignalCount] = useState(0)
  const [approvalCount, setApprovalCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    Promise.allSettled([
      get<Page<Signal>>(`/signals${query({ status: 'ACTIVE', page: 0, size: 4 })}`),
      get<Page<Commitment>>(`/commitments${query({ status: 'OPEN', page: 0, size: 100 })}`),
      get<Page<DocumentRecord>>(`/documents${query({ page: 0, size: 5 })}`),
      get<Page<ActionProposal>>(`/actions/pending${query({ page: 0, size: 5 })}`),
      get<Page<Entity>>(`/context/entities${query({ page: 0, size: 8 })}`),
    ]).then((results) => {
      if (!active) return
      const [signalResult, commitmentResult, documentResult, approvalResult, entityResult] = results
      if (signalResult.status === 'fulfilled') { setSignals(signalResult.value.content); setSignalCount(signalResult.value.totalElements) }
      if (commitmentResult.status === 'fulfilled') setCommitments(commitmentResult.value.content)
      if (documentResult.status === 'fulfilled') setDocuments(documentResult.value.content)
      if (approvalResult.status === 'fulfilled') setApprovalCount(approvalResult.value.totalElements)
      if (entityResult.status === 'fulfilled') setEntities(entityResult.value.content)
      if (results.every((result) => result.status === 'rejected')) setError('We could not load your workspace. Check your connection and try again.')
      setLoading(false)
    })
    return () => { active = false }
  }, [refreshKey])

  const today = new Date()
  const inSevenDays = useMemo(() => new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7), [today])
  const upcoming = commitments.filter((item) => {
    const due = new Date(`${item.dueDate}T00:00:00`)
    return due >= new Date(today.getFullYear(), today.getMonth(), today.getDate()) && due <= inSevenDays
  }).slice(0, 4)
  const overdueCount = commitments.filter((item) => new Date(`${item.dueDate}T23:59:59`) < today).length
  const dateLabel = new Intl.DateTimeFormat(undefined, { weekday: 'long', month: 'long', day: 'numeric' }).format(today)
  const greeting = today.getHours() < 12 ? 'Good morning' : today.getHours() < 17 ? 'Good afternoon' : 'Good evening'

  return (
    <div className="dashboard-page">
      <div className="dashboard-intro"><div><span className="date-kicker"><CalendarDays size={14} /> {dateLabel}</span><h2>{greeting}. <span>Here’s what matters.</span></h2><p>A clear view of the signals, dates, and records connected to your life.</p></div><button className="briefing-link" onClick={() => navigate('assistant')}><Sparkles size={15} /> Ask your assistant <ArrowUpRight size={14} /></button></div>

      <section className="briefing-hero" aria-label="Workspace summary">
        <div className="hero-main"><div className="hero-overline"><span className="hero-pulse" /> YOUR INTELLIGENCE BRIEFING</div><h3>{loading ? 'Gathering your context…' : signalCount ? `${signalCount} signal${signalCount === 1 ? '' : 's'} need your attention` : 'You’re up to date.'}</h3><p>{loading ? 'Connecting the records in your workspace.' : signals[0]?.explanation ?? 'No active signals right now. Your commitments and records are still here when you need them.'}</p>
          <button className="hero-action" onClick={() => navigate(signalCount ? 'signals' : 'commitments')}>{signalCount ? 'Review active signals' : 'View commitments'} <ArrowRight size={15} /></button>
        </div>
        <div className="hero-art" aria-hidden="true"><div className="orbit orbit--one" /><div className="orbit orbit--two" /><div className="orbit orbit--three" /><div className="orbit-center"><span className="orbit-core" /></div><span className="orbit-node orbit-node--a" /><span className="orbit-node orbit-node--b" /><span className="orbit-node orbit-node--c" /><span className="orbit-node orbit-node--d" /></div>
        <div className="hero-foot"><span><ShieldCheck size={13} /> Built from your records</span><span>{signals.length ? 'Evidence attached' : 'Workspace connected'}</span></div>
      </section>

      {error && <div className="inline-error"><CircleAlert size={16} />{error}<button onClick={() => window.location.reload()}>Reload</button></div>}

      <section className="metric-row" aria-label="Workspace at a glance">
        <Metric icon={<ActivityIcon />} label="Active signals" value={loading ? '—' : String(signalCount)} detail={signals.some((item) => item.priority === 'HIGH') ? 'High priority needs review' : 'Rule-based & explainable'} tone={signals.some((item) => item.priority === 'HIGH') ? 'amber' : 'green'} onClick={() => navigate('signals')} />
        <Metric icon={<CalendarDays size={18} />} label="Due this week" value={loading ? '—' : String(upcoming.length)} detail={overdueCount ? `${overdueCount} overdue` : 'Nothing overdue'} tone={overdueCount ? 'amber' : 'neutral'} onClick={() => navigate('commitments')} />
        <Metric icon={<FileText size={18} />} label="Documents" value={loading ? '—' : String(documents.length)} detail="Source records connected" tone="blue" onClick={() => navigate('documents')} />
        <Metric icon={<ShieldCheck size={18} />} label="Awaiting approval" value={loading ? '—' : String(approvalCount)} detail="You stay in control" tone="neutral" onClick={() => navigate('approvals')} />
      </section>

      <div className="dashboard-grid">
        <section className="panel signal-panel">
          <div className="panel-heading"><div><span className="eyebrow">WHAT NEEDS ATTENTION</span><h3>Active signals</h3></div><button className="text-link" onClick={() => navigate('signals')}>View all <ArrowRight size={14} /></button></div>
          {loading ? <LoadingRows /> : signals.length ? <div className="signal-list">{signals.slice(0, 3).map((signal) => <SignalRow key={signal.id} signal={signal} onClick={() => navigate('signals')} />)}</div> : <QuietEmpty icon={<CheckCircle2 size={20} />} title="Nothing urgent right now" text="When a commitment or event needs attention, it will appear here with its evidence." />}
          <div className="panel-bottom-note"><span className="green-check"><CheckCircle2 size={14} /></span> Signals come from deterministic rules and link back to their evidence.</div>
        </section>

        <section className="panel upcoming-panel">
          <div className="panel-heading"><div><span className="eyebrow">NEXT 7 DAYS</span><h3>Coming up</h3></div><button className="round-link" aria-label="View commitments" onClick={() => navigate('commitments')}><ArrowUpRight size={16} /></button></div>
          {loading ? <LoadingRows rows={3} /> : upcoming.length ? <div className="upcoming-list">{upcoming.map((item) => <UpcomingRow key={item.id} item={item} />)}</div> : <QuietEmpty icon={<CalendarDays size={20} />} title="Your week is clear" text="Open commitments will appear here as their due dates approach." />}
          <button className="panel-footer-link" onClick={() => navigate('commitments')}>See all commitments <ChevronRight size={15} /></button>
        </section>
      </div>

      <div className="dashboard-grid dashboard-grid--lower">
        <section className="panel records-panel">
          <div className="panel-heading"><div><span className="eyebrow">YOUR SOURCES</span><h3>Recent documents</h3></div><button className="text-link" onClick={() => navigate('documents')}>Browse library <ArrowRight size={14} /></button></div>
          {loading ? <LoadingRows rows={2} /> : documents.length ? <div className="document-mini-list">{documents.slice(0, 4).map((doc) => <button className="document-mini" key={doc.id} onClick={() => navigate('documents')}><span className="file-mini-icon"><FileCheck2 size={16} /></span><span className="document-mini-copy"><strong>{doc.filename}</strong><small>{doc.category} · {formatRelative(doc.createdAt)}</small></span><StatusPill status={doc.processingStatus} /></button>)}</div> : <QuietEmpty icon={<FileText size={20} />} title="Your library is ready" text="Add a document to connect it with your commitments and context." />}
        </section>

        <section className="context-card">
          <div className="context-card-top"><div><span className="eyebrow">CONNECTED CONTEXT</span><h3>Your records,<br />in relationship.</h3></div><span className="context-count">{loading ? '—' : entities.length.toString().padStart(2, '0')}<small>records</small></span></div>
          <div className="mini-network" aria-hidden="true"><span className="net-line net-line--1" /><span className="net-line net-line--2" /><span className="net-line net-line--3" /><span className="net-node net-node--center"><GitBranch size={15} /></span><span className="net-node net-node--one"><FileText size={12} /></span><span className="net-node net-node--two"><WalletCards size={12} /></span><span className="net-node net-node--three"><CalendarDays size={12} /></span><span className="net-label net-label--a">DOCUMENT</span><span className="net-label net-label--b">COMMITMENT</span><span className="net-label net-label--c">EVENT</span></div>
          <button className="context-card-link" onClick={() => navigate('context')}>Explore context graph <ArrowRight size={14} /></button>
        </section>
      </div>
    </div>
  )
}

function Metric({ icon, label, value, detail, tone, onClick }: { icon: ReactNode; label: string; value: string; detail: string; tone: string; onClick: () => void }) {
  return <button className="metric-tile" onClick={onClick}><span className={`metric-icon metric-icon--${tone}`}>{icon}</span><span className="metric-label">{label}</span><strong className="metric-value">{value}</strong><span className={`metric-detail ${tone === 'amber' ? 'metric-detail--amber' : ''}`}>{detail}</span></button>
}

function SignalRow({ signal, onClick }: { signal: Signal; onClick: () => void }) {
  return <button className="signal-row" onClick={onClick}><span className={`priority-mark priority-mark--${signal.priority.toLowerCase()}`} /><span className="signal-row-copy"><strong>{signal.title}</strong><small>{signal.explanation}</small></span><span className={`priority-label priority-label--${signal.priority.toLowerCase()}`}>{signal.priority}</span><ChevronRight size={15} className="row-chevron" /></button>
}

function UpcomingRow({ item }: { item: Commitment }) {
  const due = new Date(`${item.dueDate}T00:00:00`)
  const diff = Math.ceil((due.getTime() - new Date().setHours(0, 0, 0, 0)) / 86400000)
  return <div className="upcoming-row"><span className={`due-date-chip ${diff < 0 ? 'due-date-chip--overdue' : ''}`}><strong>{due.toLocaleDateString(undefined, { day: '2-digit' })}</strong><small>{due.toLocaleDateString(undefined, { month: 'short' })}</small></span><span className="upcoming-copy"><strong>{item.title}</strong><small>{item.amount != null ? formatMoney(item.amount, item.currency ?? 'INR') : 'Open commitment'}</small></span><span className={`due-label ${diff < 0 ? 'due-label--overdue' : ''}`}>{diff < 0 ? `${Math.abs(diff)}d overdue` : diff === 0 ? 'Today' : `${diff}d`}</span></div>
}

function StatusPill({ status }: { status: string }) {
  return <span className={`status-pill status-pill--${status.toLowerCase()}`}>{status.replace('_', ' ')}</span>
}

function QuietEmpty({ icon, title, text }: { icon: ReactNode; title: string; text: string }) {
  return <div className="quiet-empty"><span>{icon}</span><strong>{title}</strong><p>{text}</p></div>
}

function LoadingRows({ rows = 3 }: { rows?: number }) {
  return <div className="loading-rows">{Array.from({ length: rows }, (_, index) => <div className="loading-row" key={index}><span /><i /><i /></div>)}</div>
}

function formatMoney(amount: number, currency: string) {
  try { return new Intl.NumberFormat(undefined, { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount) } catch { return `${currency} ${amount.toLocaleString()}` }
}

function formatRelative(value: string) {
  const elapsedDays = Math.floor((Date.now() - new Date(value).getTime()) / 86400000)
  if (elapsedDays <= 0) return 'Today'
  if (elapsedDays === 1) return 'Yesterday'
  if (elapsedDays < 7) return `${elapsedDays} days ago`
  return new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

function ActivityIcon() { return <span className="activity-glyph"><ArrowUpRight size={11} /><ArrowDownRight size={11} /></span> }
