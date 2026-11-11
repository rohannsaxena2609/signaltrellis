import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { CircleAlert, GitBranch, Link2, Plus, Search, X } from 'lucide-react'
import { get, post, query } from '../api'
import type { Entity, Graph, Page } from '../types'

const entityTypes = ['PRODUCT', 'VENDOR', 'SUBSCRIPTION', 'ACCOUNT', 'PERSON', 'POLICY', 'PROPERTY', 'VEHICLE', 'ORGANIZATION', 'OTHER']
const relationshipTypes = ['RELATED_TO', 'OWNS', 'PURCHASED_FROM', 'SUBSCRIBES_TO', 'BILLED_BY', 'HAS_INVOICE', 'HAS_WARRANTY', 'COVERS', 'INSURES', 'OTHER']

export function ContextPage() {
  const [entities, setEntities] = useState<Entity[]>([])
  const [selected, setSelected] = useState<Entity | null>(null)
  const [graph, setGraph] = useState<Graph | null>(null)
  const [search, setSearch] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [type, setType] = useState('PRODUCT')
  const [description, setDescription] = useState('')
  const [targetId, setTargetId] = useState('')
  const [relationshipType, setRelationshipType] = useState('RELATED_TO')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    get<Page<Entity>>(`/context/entities${query({ page: 0, size: 100, query: search })}`)
      .then((page) => { if (active) { setEntities(page.content); if (!selected && page.content[0]) setSelected(page.content[0]) } })
      .catch((problem: unknown) => { if (active) setError(problem instanceof Error ? problem.message : 'Context could not be loaded.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [search])

  useEffect(() => {
    if (!selected) { setGraph(null); return }
    let active = true
    get<Graph>(`/context/entities/${selected.id}/connections${query({ depth: 2, limit: 50 })}`)
      .then((result) => { if (active) setGraph(result) })
      .catch((problem: unknown) => { if (active) setError(problem instanceof Error ? problem.message : 'Connections could not be loaded.') })
    return () => { active = false }
  }, [selected])

  const positions = useMemo(() => {
    if (!graph) return new Map<string, { x: number; y: number }>()
    const center = graph.nodes.find((node) => node.id === selected?.id) ?? graph.nodes[0]
    const others = graph.nodes.filter((node) => node.id !== center?.id)
    const map = new Map<string, { x: number; y: number }>()
    if (center) map.set(center.id, { x: 50, y: 50 })
    others.forEach((node, index) => {
      const angle = (index / Math.max(others.length, 1)) * Math.PI * 2 - Math.PI / 2
      map.set(node.id, { x: 50 + Math.cos(angle) * 34, y: 50 + Math.sin(angle) * 34 })
    })
    return map
  }, [graph, selected])

  async function createEntity(event: FormEvent) {
    event.preventDefault(); setError('')
    try {
      const created = await post<Entity>('/context/entities', { type, name, description: description || null })
      setEntities((all) => [created, ...all]); setSelected(created); setShowForm(false); setName(''); setDescription('')
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'Record could not be created.') }
  }

  async function connectEntities(event: FormEvent) {
    event.preventDefault()
    if (!selected || !targetId) return
    setError('')
    try {
      await post('/context/relationships', { sourceEntityId: selected.id, targetEntityId: targetId, type: relationshipType })
      setSelected({ ...selected })
    } catch (problem) { setError(problem instanceof Error ? problem.message : 'The records could not be connected.') }
  }

  return <div className="content-page context-page">
    <div className="page-lead-row"><div><p className="page-lead">See how the records in your workspace connect to each other.</p><div className="trust-line"><GitBranch size={14} /> Connections stay in your workspace and can be removed at any time.</div></div><button className="button button--primary" onClick={() => setShowForm(true)}><Plus size={16} /> Add record</button></div>
    {error && <div className="inline-error"><CircleAlert size={16} />{error}<button onClick={() => setError('')} aria-label="Dismiss error"><X size={14} /></button></div>}
    {showForm && <form className="panel entity-form" onSubmit={createEntity}><label className="field-label">Name<input value={name} onChange={(event) => setName(event.target.value)} maxLength={160} required placeholder="e.g. Home insurance" /></label><label className="field-label">Type<select value={type} onChange={(event) => setType(event.target.value)}>{entityTypes.map((value) => <option key={value} value={value}>{titleCase(value)}</option>)}</select></label><label className="field-label">Description <small>optional</small><input value={description} onChange={(event) => setDescription(event.target.value)} maxLength={2000} /></label><div className="form-actions"><button type="button" className="button button--quiet" onClick={() => setShowForm(false)}>Cancel</button><button className="button button--primary">Create record</button></div></form>}
    <div className="context-layout"><aside className="panel entity-sidebar"><div className="panel-heading"><div><span className="eyebrow">YOUR RECORDS</span><h3>Context</h3></div><span className="result-count">{entities.length}</span></div><label className="search-field"><Search size={15} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search records" /></label><div className="entity-list">{loading ? <p className="muted-copy">Loading records…</p> : entities.length ? entities.map((entity) => <button key={entity.id} className={`entity-list-item ${selected?.id === entity.id ? 'entity-list-item--selected' : ''}`} onClick={() => setSelected(entity)}><span className="entity-avatar">{entity.name.slice(0, 1).toUpperCase()}</span><span><strong>{entity.name}</strong><small>{titleCase(entity.type)}</small></span></button>) : <div className="table-empty"><strong>No records yet</strong><p>Add a record to start building your context.</p></div>}</div></aside>
      <section className="panel graph-panel"><div className="panel-heading"><div><span className="eyebrow">CONNECTED CONTEXT</span><h3>{selected?.name ?? 'Context graph'}</h3></div>{graph?.truncated && <span className="truncated-note">Showing a bounded view</span>}</div>{graph?.nodes.length ? <div className="graph-canvas" role="img" aria-label={`Context connections for ${selected?.name ?? 'selected record'}`}><svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">{graph.relationships.map((edge) => { const a = positions.get(edge.sourceEntityId); const b = positions.get(edge.targetEntityId); return a && b ? <line key={edge.id} x1={a.x} y1={a.y} x2={b.x} y2={b.y} /> : null })}</svg>{graph.nodes.map((node) => { const point = positions.get(node.id); return point ? <button key={node.id} className={`graph-node ${node.id === selected?.id ? 'graph-node--active' : ''}`} style={{ left: `${point.x}%`, top: `${point.y}%` }} onClick={() => setSelected(node)}><span>{node.name.slice(0, 1).toUpperCase()}</span><strong>{node.name}</strong><small>{titleCase(node.type)}</small></button> : null })}</div> : <div className="graph-empty"><span><GitBranch size={22} /></span><strong>{selected ? 'This record has no connections yet' : 'Your context graph starts here'}</strong><p>{selected ? 'Connections will appear as your workspace records relate to each other.' : 'Create records to keep important products, people, and accounts together.'}</p>{!selected && !entities.length && <button className="button button--quiet" onClick={() => setShowForm(true)}><Plus size={14} /> Add your first record</button>}</div>}{selected && entities.filter((entity) => entity.id !== selected.id).length > 0 && <form className="relationship-form" onSubmit={connectEntities}><span><Link2 size={14} /> Connect this record to</span><select aria-label="Record to connect" value={targetId} onChange={(event) => setTargetId(event.target.value)} required><option value="">Choose a record</option>{entities.filter((entity) => entity.id !== selected.id).map((entity) => <option key={entity.id} value={entity.id}>{entity.name}</option>)}</select><select aria-label="Relationship type" value={relationshipType} onChange={(event) => setRelationshipType(event.target.value)}>{relationshipTypes.map((value) => <option key={value} value={value}>{titleCase(value.replaceAll('_', ' '))}</option>)}</select><button className="button button--quiet"><Plus size={13} /> Connect</button></form>}</section>
    </div>
  </div>
}

function titleCase(value: string) { return value.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) }
