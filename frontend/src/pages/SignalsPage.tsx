import { useEffect, useState, type ReactNode } from 'react'
import { Activity, Check, ChevronDown, CircleAlert, Clock3, RefreshCw, ShieldCheck, X } from 'lucide-react'
import { get, post, query } from '../api'
import type { Page, Signal, SignalEvidence } from '../types'

type Props = { refreshKey: number; onChanged: () => void }
type SignalDetail = { signal: Signal; evidence: SignalEvidence[]; history: { eventType: string; detail: string; occurredAt: string }[] }

export function SignalsPage({ refreshKey, onChanged }: Props) {
  const [status, setStatus] = useState('ACTIVE')
  const [signals, setSignals] = useState<Signal[]>([])
  const [selected, setSelected] = useState<SignalDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    setSelected(null)
    get<Page<Signal>>(`/signals${query({ status: status === 'ALL' ? undefined : status, page: 0, size: 50 })}`)
      .then((page) => { if (active) setSignals(page.content) })
      .catch((problem: unknown) => { if (active) setError(problem instanceof Error ? problem.message : 'Signals could not be loaded.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [status, refreshKey])

  async function openSignal(signal: Signal) {
    setError('')
    try { setSelected(await get<SignalDetail>(`/signals/${signal.id}`)) }
    catch (problem) { setError(problem instanceof Error ? problem.message : 'Signal details could not be loaded.') }
  }

  async function changeSignal(action: 'dismiss' | 'resolve') {
    if (!selected) return
    setWorking(true)
    setError('')
    try {
      await post<Signal>(`/signals/${selected.signal.id}/${action}`)
      setMessage(action === 'dismiss' ? 'Signal dismissed.' : 'Signal resolved.')
      setSelected(null)
      onChanged()
      window.setTimeout(() => setMessage(''), 2800)
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'Your change could not be saved.') }
    finally { setWorking(false) }
  }

  async function recompute() {
    setWorking(true)
    setError('')
    try {
      await post('/signals/recompute')
      setMessage('Signals refreshed from your current commitments and events.')
      onChanged()
      window.setTimeout(() => setMessage(''), 3500)
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'Signals could not be refreshed.') }
    finally { setWorking(false) }
  }

  return (
    <div className="content-page signals-page">
      <div className="page-lead-row"><div><p className="page-lead">Important changes and dates surfaced from the records you’ve connected.</p><div className="trust-line"><ShieldCheck size={14} /> Every signal has a deterministic reason and inspectable source.</div></div><button className="button button--quiet" onClick={recompute} disabled={working}><RefreshCw size={15} className={working ? 'spin' : ''} /> Recompute</button></div>

      {error && <InlineError text={error} />}{message && <div className="success-banner"><Check size={15} />{message}</div>}
      <div className="split-layout signals-layout">
        <section className="panel list-panel">
          <div className="list-toolbar"><div className="segmented-control" role="tablist" aria-label="Signal status">
            {['ACTIVE', 'DISMISSED', 'RESOLVED', 'ALL'].map((value) => <button role="tab" aria-selected={status === value} className={status === value ? 'segment--selected' : ''} onClick={() => setStatus(value)} key={value}>{value === 'ALL' ? 'All' : titleCase(value)}</button>)}
          </div><span className="result-count">{loading ? 'Loading…' : `${signals.length} ${signals.length === 1 ? 'signal' : 'signals'}`}</span></div>
          {loading ? <SkeletonList /> : signals.length ? <div className="full-signal-list">{signals.map((signal) => <button className={`full-signal-row ${selected?.signal.id === signal.id ? 'full-signal-row--selected' : ''}`} key={signal.id} onClick={() => openSignal(signal)}><span className={`priority-mark priority-mark--${signal.priority.toLowerCase()}`} /><span className="full-signal-copy"><span className="full-signal-meta"><span className={`priority-label priority-label--${signal.priority.toLowerCase()}`}>{signal.priority} PRIORITY</span><span>{titleCase(signal.type.replaceAll('_', ' '))}</span></span><strong>{signal.title}</strong><small>{signal.explanation}</small></span><ChevronDown size={15} className="side-chevron" /></button>)}</div> : <Empty icon={<Activity size={19} />} title="No signals in this view" text={status === 'ACTIVE' ? 'When something needs your attention, you’ll see why and where it came from.' : 'Signals will appear here after their status changes.'} />}
        </section>

        <aside className="panel signal-detail-panel">
          {selected ? <>
            <div className="detail-topline"><span className={`priority-label priority-label--${selected.signal.priority.toLowerCase()}`}>{selected.signal.priority} PRIORITY</span><button className="icon-button" aria-label="Close signal details" onClick={() => setSelected(null)}><X size={17} /></button></div>
            <h2 className="detail-title">{selected.signal.title}</h2><p className="detail-explanation">{selected.signal.explanation}</p>
            <div className="detail-section"><div className="detail-section-title"><span>WHY THIS MATTERS</span><span className="deterministic-tag"><Activity size={12} /> RULE BASED</span></div><p>This signal was created from your recorded commitments and events. It updates when those records change.</p></div>
            <div className="detail-section"><div className="detail-section-title"><span>SUPPORTING EVIDENCE</span><span className="evidence-count">{selected.evidence.length}</span></div>
              {selected.evidence.length ? selected.evidence.map((evidence) => <div className="evidence-card" key={evidence.id}><span className="evidence-marker"><span /></span><div><strong>{evidence.summary}</strong>{evidence.observedValue && <p>{evidence.observedValue}</p>}<small>{titleCase(evidence.type.replaceAll('_', ' '))}</small></div></div>) : <p className="muted-copy">No linked evidence was returned for this signal.</p>}
            </div>
            <div className="signal-detail-foot"><span><Clock3 size={13} /> Detected {new Date(selected.signal.lastDetectedAt).toLocaleDateString()}</span>{selected.signal.status === 'ACTIVE' && <div className="detail-actions"><button className="button button--quiet" disabled={working} onClick={() => changeSignal('dismiss')}>Dismiss</button><button className="button button--primary" disabled={working} onClick={() => changeSignal('resolve')}><Check size={14} /> Resolve</button></div>}</div>
          </> : <div className="detail-placeholder"><span className="placeholder-icon"><Activity size={20} /></span><strong>Signal details</strong><p>Select a signal to see its explanation, source evidence, and available actions.</p><div className="placeholder-foot"><ShieldCheck size={14} /> Actions are always recorded.</div></div>}
        </aside>
      </div>
    </div>
  )
}

function InlineError({ text }: { text: string }) { return <div className="inline-error"><CircleAlert size={16} />{text}</div> }
function Empty({ icon, title, text }: { icon: ReactNode; title: string; text: string }) { return <div className="quiet-empty quiet-empty--large"><span>{icon}</span><strong>{title}</strong><p>{text}</p></div> }
function SkeletonList() { return <div className="loading-rows signals-skeleton">{[0, 1, 2, 3].map((item) => <div className="loading-row" key={item}><span /><i /><i /></div>)}</div> }
function titleCase(value: string) { return value.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) }
