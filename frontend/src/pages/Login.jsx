import { api } from '../api.js';

export default function Login() {
    return (
        <div style={{
            height: '100vh',
            display: 'grid',
            gridTemplateColumns: '1.1fr 1fr',
            overflow: 'hidden'
        }}>
            {/* Left panel - brand + value prop */}
            <div style={{
                position: 'relative',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                padding: '0 60px',
                overflow: 'hidden',
                borderRight: '1px solid var(--border)'
            }}>
                <div style={{
                    position: 'absolute', top: '-15%', left: '-10%',
                    width: 420, height: 420, borderRadius: '50%',
                    background: 'radial-gradient(circle, rgba(157,123,255,0.28), transparent 70%)',
                    filter: 'blur(10px)'
                }} />
                <div style={{
                    position: 'absolute', bottom: '-20%', left: '30%',
                    width: 380, height: 380, borderRadius: '50%',
                    background: 'radial-gradient(circle, rgba(255,95,162,0.2), transparent 70%)',
                    filter: 'blur(10px)'
                }} />

                <div style={{ position: 'relative', maxWidth: 420 }}>
                    <div style={{
                        fontFamily: 'var(--mono)', fontSize: '0.72rem', letterSpacing: '0.18em',
                        textTransform: 'uppercase', color: 'var(--amber-glow)', marginBottom: 18
                    }}>
                        ✦ A private record, only for you
                    </div>

                    <h1 style={{
                        fontFamily: 'var(--display)', fontWeight: 800, fontSize: '3.2rem',
                        margin: '0 0 16px', letterSpacing: '-0.03em', lineHeight: 1,
                        background: 'var(--gradient-main)', WebkitBackgroundClip: 'text',
                        backgroundClip: 'text', WebkitTextFillColor: 'transparent'
                    }}>
                        UNLOCK
                    </h1>

                    <p style={{ color: 'var(--ink-dim)', fontSize: '1.05rem', lineHeight: 1.6, margin: '0 0 32px' }}>
                        Every app asks what you've done. UNLOCK notices what actually changed —
                        real evidence, real patterns, never a guess.
                    </p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                        <ValueRow icon="📈" text="Your GitHub activity, turned into an honest timeline" />
                        <ValueRow icon="🔍" text="Real patterns, never called out from a single data point" />
                        <ValueRow icon="🔒" text="Private by default — nothing shared unless you choose to" />
                    </div>
                </div>
            </div>

            {/* Right panel - the actual login action */}
            <div style={{
                display: 'flex', flexDirection: 'column',
                alignItems: 'center', justifyContent: 'center',
                padding: '0 40px'
            }}>
                <div className="card glow-card" style={{ maxWidth: 360, width: '100%', textAlign: 'center' }}>
                    <div style={{ fontSize: '2.4rem', marginBottom: 12 }}>👋</div>
                    <h2 style={{ marginBottom: 8 }}>Get started</h2>
                    <p className="meta" style={{ marginBottom: 24 }}>
                        Sign in with GitHub — we'll pull in your public repos automatically.
                    </p>
                    <a href={api.loginUrl} className="btn primary" style={{ width: '100%', justifyContent: 'center', fontSize: '1rem', padding: '14px' }}>
                        ⚡ Continue with GitHub
                    </a>
                    <div className="meta" style={{ marginTop: 18, fontSize: '0.76rem' }}>
                        You'll be redirected to github.com to approve access. We never see your password.
                    </div>
                </div>
            </div>
        </div>
    );
}

function ValueRow({ icon, text }) {
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
                width: 36, height: 36, borderRadius: 10, flexShrink: 0,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: 'var(--surface)', border: '1px solid var(--border)', fontSize: '1.1rem'
            }}>
                {icon}
            </div>
            <div style={{ fontSize: '0.9rem', color: 'var(--ink-dim)', lineHeight: 1.4 }}>{text}</div>
        </div>
    );
}
