import { useEffect, useState, type FormEvent } from 'react'
import { CalendarDays, Check, CircleAlert, Clock3, Plus, WalletCards, X } from 'lucide-react'
import { get, post, query } from '../api'
import type { Commitment, Page } from '../types'

type Props = { refreshKey: number; onChanged: () => void }

export function CommitmentsPage({ refreshKey, onChanged }: Props) {
  const [items, setItems] = useState<Commitment[]>([])
  const [status, setStatus] = useState('OPEN')
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [saving, setSaving] = useState(false)
  const [title, setTitle] = useState('')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('INR')
  const [dueDate, setDueDate] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    get<Page<Commitment>>(`/commitments${query({ status: status === 'ALL' ? undefined : status, page: 0, size: 100 })}`)
      .then((page) => { if (active) setItems(page.content) })
      .catch((problem: unknown) => { if (active) setError(problem instanceof Error ? problem.message : 'Commitments could not be loaded.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [status, refreshKey])

  async function create(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await post('/commitments', { title, amount: amount ? Number(amount) : null, currency: amount ? currency : null, dueDate, notes: notes || null })
      setTitle(''); setAmount(''); setNotes(''); setDueDate(''); setCreating(false)
      setMessage('Commitment added to your workspace.')
      onChanged()
      window.setTimeout(() => setMessage(''), 3000)
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'Commitment could not be saved.') }
    finally { setSaving(false) }
  }

  const today = new Date().toISOString().slice(0, 10)
  const openCount = items.filter((item) => item.status === 'OPEN').length
  const overdueCount = items.filter((item) => item.status === 'OPEN' && item.dueDate < today).length
  const byCurrency = items.filter((item) => item.status === 'OPEN' && item.amount != null).reduce<Record<string, number>>((all, item) => {
    const code = item.currency ?? 'INR'; all[code] = (all[code] ?? 0) + (item.amount ?? 0); return all
  }, {})

  return (
    <div className="content-page commitments-page">
      <div className="page-lead-row"><div><p className="page-lead">Keep track of dates and obligations without losing the source context.</p><div className="trust-line"><Clock3 size={14} /> Due dates are explicit records; urgency is calculated from today.</div></div><button className="button button--primary" onClick={() => setCreating((value) => !value)}><Plus size={16} /> Add commitment</button></div>
      {error && <div className="inline-error"><CircleAlert size={16} />{error}</div>}{message && <div className="success-banner"><Check size={15} />{message}</div>}

      {creating && <section className="panel create-panel"><div className="create-panel-heading"><div><span className="eyebrow">NEW RECORD</span><h3>Add a commitment</h3></div><button className="icon-button" aria-label="Close form" onClick={() => setCreating(false)}><X size={17} /></button></div><form className="commitment-form" onSubmit={create}>
        <label className="field-label field-span-2">Title<input value={title} onChange={(event) => setTitle(event.target.value)} maxLength={160} placeholder="e.g. Home insurance renewal" required /></label>
        <label className="field-label">Due date<input type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} required /></label>
        <div className="field-row"><label className="field-label">Amount <small>optional</small><input type="number" min="0" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="0.00" /></label><label className="field-label currency-field">Currency<input value={currency} onChange={(event) => setCurrency(event.target.value.toUpperCase().slice(0, 3))} maxLength={3} disabled={!amount} /></label></div>
        <label className="field-label field-span-2">Notes <small>optional</small><textarea value={notes} onChange={(event) => setNotes(event.target.value)} maxLength={2000} rows={2} placeholder="A note to help you remember the context" /></label>
        <div className="form-actions field-span-2"><button type="button" className="button button--quiet" onClick={() => setCreating(false)}>Cancel</button><button className="button button--primary" disabled={saving}>{saving ? 'Saving…' : 'Save commitment'}</button></div>
      </form></section>}

      <div className="commitment-overview">
        <div className="commitment-summary panel"><span className="summary-icon summary-icon--green"><WalletCards size={18} /></span><span className="summary-label">OPEN COMMITMENTS</span><strong>{loading ? '—' : openCount}</strong><small>Currently active</small></div>
        <div className="commitment-summary panel"><span className="summary-icon summary-icon--amber"><CalendarDays size={18} /></span><span className="summary-label">OVERDUE</span><strong className={overdueCount ? 'text-amber' : ''}>{loading ? '—' : overdueCount}</strong><small>{overdueCount ? 'Needs your attention' : 'No overdue items'}</small></div>
        <div className="commitment-summary panel commitment-totals"><span className="summary-label">OPEN AMOUNTS</span>{loading ? <strong>—</strong> : Object.keys(byCurrency).length ? Object.entries(byCurrency).slice(0, 2).map(([code, total]) => <span className="currency-total" key={code}><strong>{formatMoney(total, code)}</strong><small>{code}</small></span>) : <><strong>—</strong><small>No amounts recorded</small></>}</div>
      </div>

      <section className="panel commitments-table-panel"><div className="table-toolbar"><div className="segmented-control" role="tablist" aria-label="Commitment status">{['OPEN', 'SETTLED', 'CANCELLED', 'ALL'].map((value) => <button key={value} role="tab" aria-selected={status === value} className={status === value ? 'segment--selected' : ''} onClick={() => setStatus(value)}>{value === 'ALL' ? 'All' : titleCase(value)}</button>)}</div><span className="result-count">{loading ? 'Loading…' : `${items.length} records`}</span></div>
        {loading ? <Skeleton /> : items.length ? <div className="commitments-list">{items.map((item) => <CommitmentRow key={item.id} item={item} />)}</div> : <div className="table-empty"><span className="empty-file-icon"><CalendarDays size={20} /></span><strong>No commitments in this view</strong><p>Record an obligation or renewal date to see it on your timeline.</p><button className="button button--quiet" onClick={() => setCreating(true)}><Plus size={14} /> Add commitment</button></div>}
      </section>
    </div>
  )
}

function CommitmentRow({ item }: { item: Commitment }) {
  const due = new Date(`${item.dueDate}T00:00:00`)
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const diff = Math.ceil((due.getTime() - today.getTime()) / 86400000)
  return <div className="commitment-row"><span className={`commitment-date-box ${item.status === 'OPEN' && diff < 0 ? 'commitment-date-box--overdue' : ''}`}><strong>{due.toLocaleDateString(undefined, { day: '2-digit' })}</strong><small>{due.toLocaleDateString(undefined, { month: 'short' })}</small></span><div className="commitment-row-main"><strong>{item.title}</strong><small>{item.notes || (item.entityId ? 'Linked to a context record' : 'Personal commitment')}</small></div><div className="commitment-amount">{item.amount != null ? formatMoney(item.amount, item.currency ?? 'INR') : <span className="muted-copy">No amount</span>}</div><span className={`commitment-due-label ${item.status === 'OPEN' && diff < 0 ? 'commitment-due-label--overdue' : ''}`}>{item.status === 'OPEN' ? diff < 0 ? `${Math.abs(diff)} days overdue` : diff === 0 ? 'Due today' : `In ${diff} days` : titleCase(item.status)}</span><span className={`status-pill status-pill--${item.status.toLowerCase()}`}><i />{titleCase(item.status)}</span></div>
}
function Skeleton() { return <div className="loading-rows table-skeleton">{[0, 1, 2, 3].map((item) => <div className="loading-row" key={item}><span /><i /><i /></div>)}</div> }
function titleCase(text: string) { return text.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) }
function formatMoney(amount: number, currency: string) { try { return new Intl.NumberFormat(undefined, { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount) } catch { return `${currency} ${amount.toLocaleString()}` } }
