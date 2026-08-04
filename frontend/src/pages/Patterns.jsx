import { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';

export default function Patterns() {
    const [patterns, setPatterns] = useState(null);
    const [scanStatus, setScanStatus] = useState('');
    const [scanning, setScanning] = useState(false);

    useEffect(() => { loadPatterns(); }, []);

    function loadPatterns() {
        api.getPatterns().then(setPatterns).catch(() => setPatterns([]));
    }

    function scanNow() {
        setScanning(true);
        setScanStatus('Scanning...');
        api.scanPatterns()
            .then(newPatterns => {
                setScanStatus(
                    newPatterns.length
                        ? `Found ${newPatterns.length} new thing${newPatterns.length === 1 ? '' : 's'} worth a look.`
                        : "Nothing new right now — not enough evidence yet, or all caught up."
                );
                loadPatterns();
            })
            .finally(() => setScanning(false));
    }

    function respond(id, confirmed) {
        api.confirmPattern(id, confirmed).then(loadPatterns);
    }

    function dismiss(id) {
        api.dismissSuggestion(id).then(loadPatterns);
    }

    const stats = useMemo(() => {
        const list = patterns || [];
        return {
            total: list.length,
            confirmed: list.filter(p => p.confirmed === true).length,
            pending: list.filter(p => p.confirmed === null).length,
        };
    }, [patterns]);

    return (
        <div className="page">
            <Link className="back" to="/dashboard">&larr; Back to dashboard</Link>

            <div className="card glow-card" style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{ fontSize: '2.2rem' }}>🔍</div>
                    <div>
                        <h2 style={{ marginBottom: 2 }}>Patterns noticed</h2>
                        <div className="meta">Never from a single data point — always shown with evidence.</div>
                    </div>
                </div>
            </div>

            <div className="stat-grid">
                <div className="stat-card">
                    <div className="stat-icon">📊</div>
                    <div className="stat-value">{stats.total}</div>
                    <div className="stat-label">Total found</div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon">✅</div>
                    <div className="stat-value">{stats.confirmed}</div>
                    <div className="stat-label">Confirmed</div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon">⏳</div>
                    <div className="stat-value">{stats.pending}</div>
                    <div className="stat-label">Awaiting review</div>
                </div>
            </div>

            <button className="primary" onClick={scanNow} disabled={scanning}>
                {scanning ? 'Scanning…' : '🔎 Scan my evidence now'}
            </button>
            <div style={{ fontSize: '0.82rem', color: 'var(--amber-glow)', margin: '10px 0 4px', minHeight: '1em' }}>
                {scanStatus}
            </div>

            {patterns === null ? (
                <div className="empty">Loading...</div>
            ) : patterns.length === 0 ? (
                <div className="empty">
                    No patterns yet — they only show up once there's enough real evidence,
                    so this is normal early on. Try scanning after adding a few entries.
                </div>
            ) : (
                <div className="thread" style={{ marginTop: 16 }}>
                    {patterns.map(p => (
                        <div key={p.id} className={`thread-node ${p.confirmed === false ? 'muted' : ''}`}>
                            <div className="card">
                                <div style={{ fontSize: '0.97rem', lineHeight: 1.55, marginBottom: 12 }}>{p.description}</div>
                                <div style={{ marginBottom: 10 }}>
                                    {(p.evidenceEntryIds || []).map(id => (
                                        <span key={id} className="tag" style={{ marginRight: 6 }}>evidence · {id.slice(-6)}</span>
                                    ))}
                                </div>

                                {p.confirmed === null && (
                                    <div style={{ display: 'flex', gap: 8 }}>
                                        <button className="primary" onClick={() => respond(p.id, true)}>✓ That's accurate</button>
                                        <button className="ghost" onClick={() => respond(p.id, false)}>✗ Not accurate</button>
                                    </div>
                                )}
                                {p.confirmed === true && (
                                    <>
                                        <div className="tag" style={{ background: 'rgba(79,227,193,0.15)', color: 'var(--success)', borderColor: 'rgba(79,227,193,0.3)' }}>✓ Confirmed</div>
                                        {p.suggestion && !p.suggestionDismissed && (
                                            <div style={{
                                                marginTop: 12, padding: '14px 16px',
                                                background: 'rgba(255,180,84,0.08)',
                                                border: '1px solid rgba(255,180,84,0.25)',
                                                borderRadius: 10, fontSize: '0.89rem', lineHeight: 1.5
                                            }}>
                                                💡 {p.suggestion}<br /><br />
                                                <button className="ghost" onClick={() => dismiss(p.id)}>No thanks</button>
                                            </div>
                                        )}
                                    </>
                                )}
                                {p.confirmed === false && (
                                    <div className="tag" style={{ background: 'rgba(255,107,122,0.1)', color: 'var(--danger)', borderColor: 'rgba(255,107,122,0.25)' }}>
                                        Marked not accurate
                                    </div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
