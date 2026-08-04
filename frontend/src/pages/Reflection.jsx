import { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';

export default function Reflection() {
    const [current, setCurrent] = useState(undefined);
    const [answer, setAnswer] = useState('');
    const [history, setHistory] = useState([]);

    useEffect(() => { loadCurrent(); loadHistory(); }, []);

    function loadCurrent() {
        api.getCurrentReflection().then(setCurrent).catch(() => setCurrent(null));
    }
    function loadHistory() {
        api.getReflectionHistory().then(list => setHistory(list.filter(r => r.answer))).catch(() => setHistory([]));
    }

    function submitAnswer() {
        if (!answer.trim() || !current) return;
        api.answerReflection(current.id, answer.trim()).then(() => {
            setAnswer('');
            loadCurrent();
            loadHistory();
        });
    }

    function generateNow() {
        api.generateReflection().then(loadCurrent);
    }

    // Streak of answered reflections - a different kind of streak than the
    // dashboard's daily one: consecutive reflections answered, back to back.
    const reflectionStreak = useMemo(() => history.length, [history]);

    return (
        <div className="page">
            <Link className="back" to="/dashboard">&larr; Back to dashboard</Link>

            <div className="card glow-card" style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{ fontSize: '2.2rem' }}>💭</div>
                    <div>
                        <h2 style={{ marginBottom: 2 }}>Your reflection</h2>
                        <div className="meta">Grounded in what you've said before — never a generic prompt.</div>
                    </div>
                    <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
                        <div className="stat-value" style={{ fontSize: '1.6rem' }}>{reflectionStreak}</div>
                        <div className="stat-label">answered</div>
                    </div>
                </div>
            </div>

            {current === undefined ? (
                <div className="empty">Loading...</div>
            ) : current === null ? (
                <div className="empty">
                    Nothing due right now — check back in a bit.<br /><br />
                    <button className="ghost" onClick={generateNow}>🔧 (Dev only) Generate one now</button>
                </div>
            ) : (
                <div className="card glow-card">
                    <div className="tag amber" style={{ marginBottom: 12 }}>New question</div>
                    <div style={{ fontFamily: 'var(--display)', fontWeight: 600, fontSize: '1.2rem', lineHeight: 1.5, marginBottom: 16 }}>
                        {current.question}
                    </div>
                    <textarea
                        rows={3}
                        placeholder="Type your answer..."
                        value={answer}
                        onChange={e => setAnswer(e.target.value)}
                    />
                    <button className="primary" onClick={submitAnswer}>💾 Save answer</button>
                </div>
            )}

            <h3 style={{ margin: '28px 0 14px' }}>Past reflections</h3>
            {history.length === 0 ? (
                <div className="empty">No past reflections yet.</div>
            ) : (
                <div className="thread">
                    {history.map(r => (
                        <div key={r.id} className="thread-node">
                            <div className="card">
                                <div className="meta" style={{ marginBottom: 6 }}>{r.question}</div>
                                <div style={{ fontFamily: 'var(--display)', fontWeight: 600, fontSize: '1rem' }}>{r.answer}</div>
                                <div className="tag" style={{ marginTop: 10 }}>{new Date(r.answeredAt).toLocaleDateString()}</div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
