import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { Check, ChevronRight, CircleAlert, Eye, FileText, LoaderCircle, Plus, Search, ShieldCheck, UploadCloud, X } from 'lucide-react'
import { api, downloadBlob, get, post, query, patch } from '../api'
import type { DocumentRecord, Entity, Extraction, ExtractedFact, Page, ProcessingJob } from '../types'

type Props = { refreshKey: number; query: string; onQueryChange: (value: string) => void; onChanged: () => void }

export function DocumentsPage({ refreshKey, query: initialQuery, onQueryChange, onChanged }: Props) {
  const [documents, setDocuments] = useState<DocumentRecord[]>([])
  const [entities, setEntities] = useState<Entity[]>([])
  const [selected, setSelected] = useState<DocumentRecord | null>(null)
  const [extraction, setExtraction] = useState<Extraction | null>(null)
  const [job, setJob] = useState<ProcessingJob | null>(null)
  const [search, setSearch] = useState(initialQuery)
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const fileInput = useRef<HTMLInputElement>(null)

  useEffect(() => setSearch(initialQuery), [initialQuery])
  useEffect(() => {
    let active = true
    get<Page<Entity>>('/context/entities?page=0&size=100').then((page) => { if (active) setEntities(page.content) }).catch(() => undefined)
    return () => { active = false }
  }, [])
  useEffect(() => {
    let active = true
    setLoading(true)
    get<Page<DocumentRecord>>(`/documents${query({ query: search.trim() || undefined, category: category || undefined, status: status || undefined, page: 0, size: 50 })}`)
      .then((page) => { if (active) setDocuments(page.content) })
      .catch((problem: unknown) => { if (active) setError(problem instanceof Error ? problem.message : 'Documents could not be loaded.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [search, category, status, refreshKey])

  useEffect(() => {
    if (!selected || !job || !['QUEUED', 'PROCESSING'].includes(job.status)) return
    let active = true
    const interval = window.setInterval(() => {
      get<ProcessingJob>(`/documents/${selected.id}/process`).then((next) => {
        if (!active) return
        setJob(next)
        if (next.status === 'PROCESSED') {
          loadExtraction(selected.id)
          onChanged()
        }
        if (next.status === 'FAILED') setError(next.lastError ?? 'Processing failed. You can retry the job.')
      }).catch(() => undefined)
    }, 2500)
    return () => { active = false; window.clearInterval(interval) }
  }, [selected, job?.status])

  async function selectDocument(document: DocumentRecord) {
    setSelected(document)
    setExtraction(null)
    setJob(null)
    setError('')
    setMessage('')
    try {
      const result = await get<Extraction>(`/documents/${document.id}/extraction`)
      setExtraction(result)
      try { setJob(await get<ProcessingJob>(`/documents/${document.id}/process`)) } catch { /* Older documents may not have a job. */ }
    } catch {
      try { setJob(await get<ProcessingJob>(`/documents/${document.id}/process`)) } catch { /* Not queued yet. */ }
    }
  }

  async function loadExtraction(documentId: string) {
    try { setExtraction(await get<Extraction>(`/documents/${documentId}/extraction`)) }
    catch (problem) { setError(problem instanceof Error ? problem.message : 'Extraction could not be loaded.') }
  }

  async function upload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setUploading(true)
    setError('')
    setMessage('')
    const form = new FormData()
    form.append('file', file)
    try {
      const document = await api<DocumentRecord>('/capture/upload', { method: 'POST', body: form })
      setMessage(`${document.filename} added. Start local processing when you’re ready.`)
      onQueryChange('')
      setSearch('')
      onChanged()
      await selectDocument(document)
      window.setTimeout(() => setMessage(''), 4500)
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'Upload failed.') }
    finally { setUploading(false) }
  }

  async function processDocument() {
    if (!selected) return
    setError('')
    try {
      const next = await post<ProcessingJob>(`/documents/${selected.id}/process`)
      setJob(next)
      setMessage(next.status === 'PROCESSED' ? 'This document is already processed.' : 'Local extraction and indexing queued. No AI calls will be made.')
      onChanged()
      window.setTimeout(() => setMessage(''), 4000)
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'Document could not be queued.') }
  }

  async function openFile(document: DocumentRecord) {
    setError('')
    try {
      const blob = await downloadBlob(`/documents/${document.id}/content`)
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank', 'noopener,noreferrer')
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'File could not be opened.') }
  }

  async function reviewFact(fact: ExtractedFact, status: 'CONFIRMED' | 'CORRECTED' | 'REJECTED', correctedValue?: string) {
    if (!selected) return
    setError('')
    try {
      await patch(`/documents/${selected.id}/extraction/facts/${fact.id}`, { status, correctedValue })
      setMessage(`Candidate ${status.toLowerCase()}.`)
      await loadExtraction(selected.id)
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'Review could not be saved.') }
  }

  async function linkFact(fact: ExtractedFact, entityId: string) {
    if (!selected) return false
    setError('')
    try {
      await post(`/documents/${selected.id}/extraction/facts/${fact.id}/context-links`, { targetType: 'ENTITY', targetId: entityId })
      setMessage('Reviewed fact linked to the selected context record.')
      window.setTimeout(() => setMessage(''), 3500)
      return true
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'The fact could not be linked.'); return false }
  }

  return (
    <div className="content-page documents-page">
      <div className="page-lead-row"><div><p className="page-lead">Original records stay connected to every extracted fact and citation.</p><div className="trust-line"><ShieldCheck size={14} /> Uploads stay in your private workspace. Local processing makes no model calls.</div></div>
        <input ref={fileInput} className="visually-hidden" type="file" accept="application/pdf,image/png,image/jpeg,.pdf,.png,.jpg,.jpeg" onChange={upload} />
        <button className="button button--primary" onClick={() => fileInput.current?.click()} disabled={uploading}><UploadCloud size={16} />{uploading ? 'Uploading…' : 'Upload document'}</button>
      </div>
      {error && <div className="inline-error"><CircleAlert size={16} />{error}</div>}{message && <div className="success-banner"><Check size={15} />{message}</div>}

      <div className="documents-toolbar"><div className="document-filter-group"><div className="document-search"><Search size={16} /><input value={search} onChange={(event) => { setSearch(event.target.value); onQueryChange(event.target.value) }} placeholder="Search by filename" aria-label="Search documents by filename" />{search && <button aria-label="Clear search" onClick={() => { setSearch(''); onQueryChange('') }}><X size={14} /></button>}</div><select className="document-filter" aria-label="Filter documents by category" value={category} onChange={(event) => setCategory(event.target.value)}><option value="">All categories</option>{['UNCATEGORIZED', 'FINANCIAL', 'PURCHASE', 'IDENTITY', 'HOUSING', 'INSURANCE', 'PERSONAL', 'OTHER'].map((value) => <option key={value} value={value}>{titleCase(value)}</option>)}</select><select className="document-filter" aria-label="Filter documents by status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All statuses</option>{['UPLOADED', 'QUEUED', 'PROCESSING', 'PROCESSED', 'FAILED'].map((value) => <option key={value} value={value}>{titleCase(value)}</option>)}</select></div><span className="result-count">{loading ? 'Loading…' : `${documents.length} records`}</span></div>

      <div className={`documents-workspace ${selected ? 'documents-workspace--detail' : ''}`}>
        <section className="panel documents-table-panel">
          <div className="documents-table-head"><span>DOCUMENT</span><span>CATEGORY</span><span>STATUS</span><span>ADDED</span><span /></div>
          {loading ? <Skeleton /> : documents.length ? <div className="documents-table">{documents.map((document) => <button className={`document-table-row ${selected?.id === document.id ? 'document-table-row--selected' : ''}`} key={document.id} onClick={() => selectDocument(document)}><span className="file-name-cell"><span className="file-type-icon"><FileText size={17} /></span><span><strong>{document.filename}</strong><small>{formatBytes(document.sizeBytes)}</small></span></span><span className="category-tag">{document.category.replaceAll('_', ' ')}</span><Status status={document.processingStatus} /><span className="date-cell">{new Date(document.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</span><ChevronRight size={15} className="row-chevron" /></button>)}</div> : <div className="table-empty"><span className="empty-file-icon"><FileText size={20} /></span><strong>{search ? 'No matching documents' : 'Your source library is ready'}</strong><p>{search ? 'Try another filename.' : 'Add an invoice, policy, or another record to get started.'}</p>{!search && <button className="button button--quiet" onClick={() => fileInput.current?.click()}><Plus size={14} /> Add your first document</button>}</div>}
        </section>

        {selected && <aside className="panel document-detail-panel">
          <div className="document-detail-head"><span className="file-type-icon file-type-icon--large"><FileText size={19} /></span><button className="icon-button" aria-label="Close document" onClick={() => { setSelected(null); setExtraction(null); setJob(null) }}><X size={17} /></button><div><span className="eyebrow">SOURCE RECORD</span><h2>{selected.filename}</h2></div></div>
          <div className="document-meta-grid"><div><span>TYPE</span><strong>{selected.contentType.replace('application/', '').replace('image/', '').toUpperCase()}</strong></div><div><span>ADDED</span><strong>{new Date(selected.createdAt).toLocaleDateString()}</strong></div></div>
          <div className="document-detail-actions"><button className="button button--quiet button--full" onClick={() => openFile(selected)}><Eye size={15} /> View original</button><button className="button button--primary button--full" onClick={processDocument} disabled={job?.status === 'QUEUED' || job?.status === 'PROCESSING' || job?.status === 'PROCESSED'}><span className={job?.status === 'QUEUED' || job?.status === 'PROCESSING' ? 'spin' : ''}>{job?.status === 'QUEUED' || job?.status === 'PROCESSING' ? <LoaderCircle size={15} /> : <FileText size={15} />}</span>{job?.status === 'QUEUED' || job?.status === 'PROCESSING' ? 'Processing locally…' : job?.status === 'PROCESSED' ? 'Processed' : 'Extract & index'}</button></div>
          {job && <div className="job-status-line"><span className={`status-dot status-dot--${job.status.toLowerCase()}`} /><span>{job.status === 'FAILED' ? job.lastError ?? 'Processing failed' : `${titleCase(job.status)}${job.status === 'PROCESSING' || job.status === 'QUEUED' ? ` · attempt ${job.attemptCount} of ${job.maxAttempts}` : ''}`}</span></div>}
          <div className="facts-section"><div className="detail-section-title"><span>EXTRACTED CANDIDATES</span>{extraction && <span className="evidence-count">{extraction.candidates.length}</span>}</div>
            {extraction ? extraction.candidates.length ? <div className="fact-list">{extraction.candidates.map((fact) => <FactRow key={fact.id} fact={fact} entities={entities} onReview={reviewFact} onLink={linkFact} />)}</div> : <div className="facts-empty"><Check size={15} /><span>Text extracted. No date or amount candidates were found.</span></div> : <div className="facts-placeholder"><FileText size={18} /><span>Run local extraction to find reviewable dates and amounts.</span></div>}
          </div>
          <div className="local-processing-note"><ShieldCheck size={14} /><span>This background flow uses local PDF/OCR extraction and lexical indexing. AI and embeddings remain off.</span></div>
        </aside>}
      </div>
    </div>
  )
}

function FactRow({ fact, entities, onReview, onLink }: { fact: ExtractedFact; entities: Entity[]; onReview: (fact: ExtractedFact, status: 'CONFIRMED' | 'CORRECTED' | 'REJECTED', correctedValue?: string) => void; onLink: (fact: ExtractedFact, entityId: string) => Promise<boolean> }) {
  const [correcting, setCorrecting] = useState(false)
  const [value, setValue] = useState(fact.extractedValue)
  const [entityId, setEntityId] = useState('')
  const [linked, setLinked] = useState(false)
  const [linking, setLinking] = useState(false)
  async function link() { if (!entityId || linked || linking) return; setLinking(true); setLinked(await onLink(fact, entityId)); setLinking(false) }
  return <div className="fact-row"><div className="fact-row-top"><span className="fact-type">{fact.type.replaceAll('_', ' ')}</span><span className={`fact-review fact-review--${fact.reviewStatus.toLowerCase()}`}>{fact.reviewStatus}</span></div><strong>{fact.correctedValue ?? fact.extractedValue}</strong><blockquote>“{fact.sourceExcerpt}”</blockquote><small>Page {fact.sourcePage}{fact.confidence != null ? ` · ${Math.round(fact.confidence * 100)}% model confidence` : ''}</small>{fact.reviewStatus === 'PENDING' && <>{correcting && <label className="fact-correction">Corrected value<input value={value} onChange={(event) => setValue(event.target.value)} maxLength={1000} autoFocus /></label>}<div className="fact-actions"><button onClick={() => onReview(fact, 'REJECTED')}><X size={12} /> Reject</button>{correcting ? <><button onClick={() => setCorrecting(false)}>Cancel</button><button onClick={() => onReview(fact, 'CORRECTED', value.trim())} disabled={!value.trim() || value.trim() === fact.extractedValue}><Check size={12} /> Save correction</button></> : <><button onClick={() => setCorrecting(true)}>Correct</button><button onClick={() => onReview(fact, 'CONFIRMED')}><Check size={14} /> Confirm</button></>}</div></>}{fact.reviewStatus !== 'PENDING' && fact.reviewStatus !== 'REJECTED' && entities.length > 0 && <div className="fact-link-controls"><select aria-label={`Context record for ${fact.extractedValue}`} value={entityId} onChange={(event) => setEntityId(event.target.value)}><option value="">Link to a context record</option>{entities.map((entity) => <option key={entity.id} value={entity.id}>{entity.name}</option>)}</select><button className="button button--quiet" onClick={link} disabled={!entityId || linked || linking}>{linked ? 'Linked' : linking ? 'Linking…' : 'Link fact'}</button></div>}</div>
}

function Status({ status }: { status: string }) { return <span className={`status-pill status-pill--${status.toLowerCase()}`}><i />{status.replaceAll('_', ' ')}</span> }
function Skeleton() { return <div className="loading-rows table-skeleton">{[0, 1, 2, 3, 4].map((i) => <div className="loading-row" key={i}><span /><i /><i /></div>)}</div> }
function titleCase(text: string) { return text.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) }
function formatBytes(bytes: number) { return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB` }
