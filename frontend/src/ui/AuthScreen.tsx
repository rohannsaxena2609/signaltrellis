import { useState, type FormEvent } from 'react'
import { ArrowRight, Check, Eye, EyeOff, LockKeyhole, ShieldCheck } from 'lucide-react'
import { BrandGlyph } from './AppShell'

type Props = { onSubmit: (email: string, password: string, displayName?: string) => Promise<void> }

export function AuthScreen({ onSubmit }: Props) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await onSubmit(email.trim(), password, mode === 'register' ? displayName.trim() : undefined)
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : 'We could not sign you in. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-story">
        <div className="auth-brand"><span className="brand-mark brand-mark--light"><BrandGlyph light /></span><span>SignalTrellis</span></div>
        <div className="auth-story-content">
          <span className="auth-kicker"><span className="signal-pulse" /> PERSONAL CONTEXT & ACTION INTELLIGENCE</span>
          <h1>Make sense of<br />what’s <em>next.</em></h1>
          <p>Your records are connected. See what matters, understand why, and choose what to do next.</p>
          <div className="auth-preview">
            <div className="preview-head"><span className="preview-dots"><i /><i /><i /></span><span>YOUR INTELLIGENCE BRIEFING</span><span className="preview-live">LIVE</span></div>
            <div className="preview-signal"><div className="preview-signal-icon"><LockKeyhole size={16} /></div><div><strong>Renewal coming up</strong><small>Insurance policy · 12 days</small></div><span className="preview-priority">REVIEW</span></div>
            <div className="preview-divider" />
            <div className="preview-evidence"><span className="evidence-line" /><span>Source evidence linked</span><Check size={13} /></div>
          </div>
        </div>
        <div className="auth-story-footer"><span>PRIVATE BY DESIGN</span><span>ONE PERSONAL WORKSPACE</span><span>YOU STAY IN CONTROL</span></div>
      </section>

      <section className="auth-form-side">
        <div className="auth-form-wrap">
          <div className="auth-mobile-brand"><span className="brand-mark"><BrandGlyph /></span><span>SignalTrellis</span></div>
          <div className="auth-heading"><div className="eyebrow">WELCOME TO YOUR WORKSPACE</div><h2>{mode === 'login' ? 'Sign in to continue' : 'Create your workspace'}</h2><p>{mode === 'login' ? 'Your personal context, ready when you are.' : 'A private place for the information that matters.'}</p></div>
          <form className="auth-form" onSubmit={submit}>
            {mode === 'register' && <label className="field-label">Your name<input autoComplete="name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="Rohan Saxena" required maxLength={120} /></label>}
            <label className="field-label">Email address<input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" required maxLength={320} /></label>
            <label className="field-label">Password<span className="password-field"><input type={showPassword ? 'text' : 'password'} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} placeholder={mode === 'register' ? 'At least 10 characters' : 'Enter your password'} required minLength={mode === 'register' ? 10 : 1} maxLength={72} /><button type="button" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword((value) => !value)}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></span></label>
            {error && <div className="form-error" role="alert">{error}</div>}
            <button className="button button--primary auth-submit" type="submit" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}<ArrowRight size={16} /></button>
          </form>
          <div className="auth-switch">{mode === 'login' ? 'New to SignalTrellis?' : 'Already have an account?'} <button onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError('') }}>{mode === 'login' ? 'Create an account' : 'Sign in'}</button></div>
          <div className="auth-security"><ShieldCheck size={15} /><span>Your workspace is private. Your information is only visible to you.</span></div>
        </div>
        <div className="auth-legal">By continuing, you agree to use this early access workspace responsibly.</div>
      </section>
    </main>
  )
}
