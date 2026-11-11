import { useEffect, useState } from 'react'
import { Check, CircleAlert, Clock3, ShieldCheck, X } from 'lucide-react'
import { get, post, query } from '../api'
import type { ActionProposal, Page } from '../types'

type Props = { refreshKey: number; onChanged: () => void }

export function ApprovalsPage({ refreshKey, onChanged }: Props) {
  const [items, setItems] = useState<ActionProposal[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    get<Page<ActionProposal>>(`/actions/pending${query({ page: 0, size: 50 })}`)
      .then((page) => { if (active) setItems(page.content) })
      .catch((problem: unknown) => { if (active) setError(problem instanceof Error ? problem.message : 'Approvals could not be loaded.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [refreshKey])

  async function decide(id: string, decision: 'approve' | 'reject') {
    setBusyId(id); setError('')
    try {
      await post(`/actions/${id}/${decision}`, {})
      setMessage(decision === 'approve' ? 'Approved. The reminder is now scheduled.' : 'Proposal rejected.')
      onChanged()
      window.setTimeout(() => setMessage(''), 3500)
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'Your decision could not be saved.') }
    finally { setBusyId('') }
  }

  return <div className="content-page approvals-page">
    <div className="page-lead-row"><div><p className="page-lead">Nothing consequential happens until you decide.</p><div className="trust-line"><ShieldCheck size={14} /> Approval creates the in-app reminder and records your decision.</div></div><span className="approval-count"><strong>{loading ? '—' : items.length}</strong> waiting</span></div>
    {error && <div className="inline-error"><CircleAlert size={16} />{error}</div>}{message && <div className="success-banner"><Check size={15} />{message}</div>}
    <section className="approval-intro panel"><div className="approval-intro-icon"><ShieldCheck size={19} /></div><div><span className="eyebrow">YOUR CONTROL, BY DESIGN</span><h3>Review each proposed action</h3><p>AI and signal workflows may prepare a proposal. Only your approval schedules a reminder.</p></div></section>
    {loading ? <div className="panel"><div className="loading-rows table-skeleton">{[0, 1, 2].map((item) => <div className="loading-row" key={item}><span /><i /><i /></div>)}</div></div> : items.length ? <div className="approval-list">{items.map((item) => <article className="panel approval-card" key={item.id}><div className="approval-card-icon"><Clock3 size={18} /></div><div className="approval-card-main"><div className="approval-card-meta"><span className="pending-label"><i /> PENDING YOUR REVIEW</span><span>Review before {new Date(item.expiresAt).toLocaleDateString()}</span></div><h3>{item.title}</h3><p>{item.details || 'No additional details were provided.'}</p><div className="approval-dates"><span><Clock3 size={13} /> Reminder {new Date(item.scheduledFor).toLocaleString()}</span><span>Expires {new Date(item.expiresAt).toLocaleString()}</span></div></div><div className="approval-card-actions"><button className="button button--quiet" onClick={() => decide(item.id, 'reject')} disabled={busyId === item.id}><X size={14} /> Reject</button><button className="button button--primary" onClick={() => decide(item.id, 'approve')} disabled={busyId === item.id}><Check size={14} /> Approve</button></div></article>)}</div> : <div className="panel approval-empty"><span><ShieldCheck size={20} /></span><strong>No pending actions</strong><p>Proposed reminders and other actions will wait here for your decision.</p></div>}
  </div>
}
