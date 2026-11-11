import { useState, type FormEvent } from 'react'
import { Bot, CircleAlert, CornerDownLeft, ShieldCheck, Sparkles, UserRound } from 'lucide-react'
import { ApiError, post } from '../api'
import type { AssistantResponse } from '../types'

type Message = { role: 'user' | 'assistant'; text: string; model?: string }

export function AssistantPage() {
  const [messages, setMessages] = useState<Message[]>([])
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')

  async function send(event?: FormEvent) {
    event?.preventDefault()
    const message = draft.trim()
    if (!message || sending) return
    setDraft(''); setError(''); setMessages((all) => [...all, { role: 'user', text: message }]); setSending(true)
    try {
      const response = await post<AssistantResponse>('/assistant/chat', { message })
      setMessages((all) => [...all, { role: 'assistant', text: response.answer, model: response.model }])
    } catch (problem) {
      if (problem instanceof ApiError && problem.status === 503) setError('The assistant is disabled. Enable it in the backend environment to use the configured AI provider.')
      else setError(problem instanceof Error ? problem.message : 'The assistant could not respond. Try again later.')
    } finally { setSending(false) }
  }

  return <div className="content-page assistant-page">
    <div className="assistant-header"><span className="assistant-mark"><Sparkles size={20} /></span><div><span className="eyebrow">YOUR CONTEXT ASSISTANT</span><h2>Ask with your records in mind.</h2><p>Explore cited documents, upcoming commitments, and active signals.</p></div></div>
    <div className="assistant-policy"><ShieldCheck size={16} /><span>Assistant access is configured by the workspace operator. It can prepare a reminder proposal, but cannot approve it.</span><span className="policy-dot">AI is opt-in</span></div>
    <section className="assistant-conversation" aria-live="polite">
      {!messages.length && <div className="assistant-welcome"><span><Bot size={24} /></span><h3>What would you like to understand?</h3><p>Your messages are sent to the configured model only after you submit them. Retrieved evidence can be included with the request.</p><div className="suggestion-list">{['What commitments are coming up?', 'Find my documents about renewals', 'What needs my attention this week?'].map((suggestion) => <button key={suggestion} onClick={() => setDraft(suggestion)}>{suggestion}<CornerDownLeft size={14} /></button>)}</div></div>}
      {messages.map((message, index) => <article className={`chat-message chat-message--${message.role}`} key={`${index}-${message.role}`}><span className="chat-avatar">{message.role === 'user' ? <UserRound size={16} /> : <Bot size={16} />}</span><div className="chat-message-body"><div className="chat-message-label">{message.role === 'user' ? 'You' : 'Assistant'}{message.model && <small>{message.model}</small>}</div><p>{message.text}</p></div></article>)}
      {sending && <div className="assistant-typing"><span /><span /><span /> Thinking about your records…</div>}
    </section>
    {error && <div className="inline-error"><CircleAlert size={16} />{error}</div>}
    <form className="assistant-composer" onSubmit={send}><textarea value={draft} onChange={(event) => setDraft(event.target.value)} maxLength={4000} rows={2} placeholder="Ask about your commitments, documents, or signals…" aria-label="Message the assistant" /><button className="button button--primary" disabled={!draft.trim() || sending}><span>{sending ? 'Sending…' : 'Send'}</span><CornerDownLeft size={15} /></button><small><ShieldCheck size={12} /> AI calls happen only when you submit a message.</small></form>
  </div>
}
