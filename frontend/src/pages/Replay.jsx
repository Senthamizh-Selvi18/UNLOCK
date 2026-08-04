import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';

export default function Replay() {
    const [data, setData] = useState(null);
    const [error, setError] = useState(false);

    useEffect(() => {
        api.getReplay().then(setData).catch(() => setError(true));
    }, []);

    if (error) {
        return (
            <div className="page">
                <div className="empty">Not logged in. <Link to="/">Go back</Link>.</div>
            </div>
        );
    }

    const title = data
        ? `${data.student.displayName || data.student.username}'s journey so far`
        : 'Your journey so far';

    return (
        <div className="page">
            <Link className="back no-print" to="/dashboard">&larr; Back to dashboard</Link>

            <div className="card glow-card" style={{ textAlign: 'center', marginBottom: 24 }}>
                <div className="tag amber" style={{ marginBottom: 10 }}>🎬 Growth Replay</div>
                <h1 style={{ fontFamily: 'var(--display)', fontWeight: 800, fontSize: '1.9rem', margin: '4px 0 8px' }}>{title}</h1>
                <div className="meta">Generated only because you chose to — nothing here is shared automatically.</div>
            </div>

            <div className="no-print" style={{ display: 'flex', justifyContent: 'center', margin: '16px 0 24px' }}>
                <button className="primary" onClick={() => window.print()}>📄 Save as PDF</button>
            </div>

            {!data ? (
                <div className="empty">Loading your record...</div>
            ) : (
                <>
                    <div className="stat-grid" style={{ marginBottom: 24 }}>
                        <div className="stat-card">
                            <div className="stat-icon">📈</div>
                            <div className="stat-value">{data.entries.length}</div>
                            <div className="stat-label">Things built</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-icon">💭</div>
                            <div className="stat-value">{data.answeredReflections.length}</div>
                            <div className="stat-label">Reflections</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-icon">🔍</div>
                            <div className="stat-value">{data.confirmedPatterns.length}</div>
                            <div className="stat-label">Patterns confirmed</div>
                        </div>
                    </div>

                    <SectionLabel icon="🛠️">What you've built</SectionLabel>
                    {data.entries.length === 0 ? <div className="empty">No entries yet.</div> :
                        data.entries.map(e => (
                            <div className="card" key={e.id}>
                                <div className="entry-title">{e.source === 'GITHUB_REPO' ? '💻 ' : '✍️ '}{e.title}</div>
                                <div className="entry-desc">{e.description}</div>
                                <div className="tag">{new Date(e.date).toLocaleDateString()} · {e.source === 'GITHUB_REPO' ? 'GitHub' : 'Manual'}</div>
                            </div>
                        ))
                    }

                    <SectionLabel icon="💭">What you've said, in your own words</SectionLabel>
                    {data.answeredReflections.length === 0 ? <div className="empty">No answered reflections yet.</div> :
                        data.answeredReflections.map(r => (
                            <div className="card" key={r.id}>
                                <div className="meta" style={{ marginBottom: 6 }}>{r.question}</div>
                                <div style={{ fontFamily: 'var(--display)', fontWeight: 600 }}>{r.answer}</div>
                                <div className="tag" style={{ marginTop: 8 }}>{new Date(r.answeredAt).toLocaleDateString()}</div>
                            </div>
                        ))
                    }

                    <SectionLabel icon="🔍">Patterns you've confirmed about yourself</SectionLabel>
                    {data.confirmedPatterns.length === 0
                        ? <div className="empty">No confirmed patterns yet — these appear here only once you've reviewed and agreed with one.</div>
                        : data.confirmedPatterns.map(p => (
                            <div className="card" key={p.id}>
                                <div style={{ color: 'var(--ink)' }}>{p.description}</div>
                                <div className="tag" style={{ marginTop: 8 }}>Confirmed by you</div>
                            </div>
                        ))
                    }

                    <div className="no-print meta" style={{ textAlign: 'center', margin: '30px 0 10px' }}>
                        This page reflects your private evidence and confirmed patterns only.
                        Nothing is sent anywhere unless you choose to share this yourself.
                    </div>
                </>
            )}
        </div>
    );
}

function SectionLabel({ children, icon }) {
    return (
        <div style={{
            fontFamily: 'var(--mono)', fontSize: '0.75rem', letterSpacing: '0.08em',
            textTransform: 'uppercase', color: 'var(--ink-faint)', margin: '30px 0 12px',
            display: 'flex', alignItems: 'center', gap: 8
        }}>
            <span>{icon}</span> {children}
        </div>
    );
}
