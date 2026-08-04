import { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { api } from '../api.js';

// Turns raw entries into a "last 8 weeks of activity" dataset for the chart.
// Purely derived from data already returned by GET /api/entries - no backend change needed.
function buildWeeklyActivity(entries) {
    const weeks = [];
    const now = new Date();
    for (let i = 7; i >= 0; i--) {
        const weekStart = new Date(now);
        weekStart.setDate(now.getDate() - i * 7);
        weeks.push({ label: `W${8 - i}`, start: new Date(weekStart), count: 0 });
    }
    entries.forEach(e => {
        const d = new Date(e.date);
        for (let i = weeks.length - 1; i >= 0; i--) {
            if (d >= weeks[i].start) {
                weeks[i].count += 1;
                break;
            }
        }
    });
    return weeks.map(w => ({ name: w.label, activity: w.count }));
}

// Computes a "current streak" - consecutive days (from today backward) with at least one entry.
function computeStreak(entries) {
    const daySet = new Set(entries.map(e => new Date(e.date).toDateString()));
    let streak = 0;
    let cursor = new Date();
    while (daySet.has(cursor.toDateString())) {
        streak += 1;
        cursor.setDate(cursor.getDate() - 1);
    }
    return streak;
}

// Badge definitions - all computed client-side from real entry data, never invented.
function computeBadges(entries) {
    const githubCount = entries.filter(e => e.source === 'GITHUB_REPO').length;
    const manualCount = entries.filter(e => e.source === 'MANUAL').length;
    const total = entries.length;

    return [
        { icon: '🌱', label: 'First Sync', unlocked: githubCount >= 1 },
        { icon: '📚', label: '5 Entries', unlocked: total >= 5 },
        { icon: '🏗️', label: '10 Entries', unlocked: total >= 10 },
        { icon: '✍️', label: 'Manual Logger', unlocked: manualCount >= 1 },
        { icon: '🔥', label: '3-Day Streak', unlocked: computeStreak(entries) >= 3 },
    ];
}

export default function Dashboard() {
    const navigate = useNavigate();
    const [student, setStudent] = useState(null);
    const [loadError, setLoadError] = useState(false);
    const [entries, setEntries] = useState(null);
    const [syncStatus, setSyncStatus] = useState('');
    const [syncing, setSyncing] = useState(false);
    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');

    useEffect(() => {
        api.getMe().then(setStudent).catch(() => setLoadError(true));
        loadEntries();
    }, []);

    function loadEntries() {
        api.getEntries().then(setEntries).catch(() => setEntries([]));
    }

    function syncGitHub() {
        setSyncing(true);
        setSyncStatus('Syncing...');
        api.syncGitHub()
            .then(result => {
                setSyncStatus(result.message);
                loadEntries();
            })
            .catch(() => setSyncStatus('Something went wrong — try again.'))
            .finally(() => setSyncing(false));
    }

    function addManualEntry() {
        if (!title.trim()) return;
        api.addManualEntry(title.trim(), description.trim()).then(() => {
            setTitle('');
            setDescription('');
            loadEntries();
        });
    }

    function toggleComplete(entry) {
        api.markComplete(entry.id, !entry.completed).then(loadEntries);
    }

    function handleLogout() {
        api.logout().finally(() => navigate('/'));
    }

    const chartData = useMemo(() => buildWeeklyActivity(entries || []), [entries]);
    const streak = useMemo(() => computeStreak(entries || []), [entries]);
    const badges = useMemo(() => computeBadges(entries || []), [entries]);
    const unlockedCount = badges.filter(b => b.unlocked).length;
    const githubCount = (entries || []).filter(e => e.source === 'GITHUB_REPO').length;
    const manualCount = (entries || []).filter(e => e.source === 'MANUAL').length;

    if (loadError) {
        return (
            <div className="page">
                <div className="empty">Not logged in. <Link to="/">Go back</Link>.</div>
            </div>
        );
    }

    return (
        <div className="page">
            {student ? (
                <div className="card glow-card profile-row">
                    <img src={student.avatarUrl} alt="avatar" />
                    <div>
                        <div className="profile-name">{student.displayName || student.username}</div>
                        <div className="profile-handle">@{student.username}</div>
                    </div>
                    <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
                        <div className="stat-value" style={{ fontSize: '1.4rem' }}>🔥 {streak}</div>
                        <div className="stat-label">day streak</div>
                        <button className="ghost" style={{ marginTop: 10, fontSize: '0.78rem', padding: '6px 12px' }} onClick={handleLogout}>
                            🚪 Log out
                        </button>
                    </div>
                </div>
            ) : (
                <div className="empty">Loading...</div>
            )}

            {/* Stat grid - gamified overview */}
            <div className="stat-grid">
                <div className="stat-card">
                    <div className="stat-icon">📈</div>
                    <div className="stat-value">{entries?.length ?? '—'}</div>
                    <div className="stat-label">Total entries</div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon">💻</div>
                    <div className="stat-value">{githubCount}</div>
                    <div className="stat-label">From GitHub</div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon">✍️</div>
                    <div className="stat-value">{manualCount}</div>
                    <div className="stat-label">Manual</div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon">🏅</div>
                    <div className="stat-value">{unlockedCount}/{badges.length}</div>
                    <div className="stat-label">Badges</div>
                </div>
            </div>

            {/* Activity chart - real data, last 8 weeks */}
            <div className="chart-wrap">
                <h3 style={{ marginBottom: 4 }}>Activity, last 8 weeks</h3>
                <div className="meta" style={{ marginBottom: 8 }}>Built from your real entry dates — no guessing.</div>
                <ResponsiveContainer width="100%" height={180}>
                    <BarChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#29294F" vertical={false} />
                        <XAxis dataKey="name" stroke="#6E6D91" fontSize={11} tickLine={false} axisLine={false} />
                        <YAxis stroke="#6E6D91" fontSize={11} tickLine={false} axisLine={false} allowDecimals={false} width={24} />
                        <Tooltip
                            contentStyle={{ background: '#16162E', border: '1px solid #29294F', borderRadius: 10, fontSize: 12 }}
                            labelStyle={{ color: '#F4F3FA' }}
                            cursor={{ fill: 'rgba(157,123,255,0.08)' }}
                        />
                        <Bar dataKey="activity" radius={[6, 6, 0, 0]} fill="url(#barGradient)" />
                        <defs>
                            <linearGradient id="barGradient" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%" stopColor="#FFB454" />
                                <stop offset="100%" stopColor="#FF5FA2" />
                            </linearGradient>
                        </defs>
                    </BarChart>
                </ResponsiveContainer>
            </div>

            {/* Badges */}
            <div className="card">
                <h3 style={{ marginBottom: 12 }}>Badges</h3>
                <div className="badge-row">
                    {badges.map(b => (
                        <div key={b.label} className={`badge ${b.unlocked ? 'unlocked' : ''}`}>
                            <span className="badge-icon">{b.icon}</span> {b.label}
                        </div>
                    ))}
                </div>
            </div>

            {/* Nav */}
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '4px 0 6px' }}>
                <button className="primary" onClick={syncGitHub} disabled={syncing}>
                    {syncing ? 'Syncing…' : '⚡ Sync my GitHub activity'}
                </button>
                <Link className="btn" to="/reflection">💭 Reflection</Link>
                <Link className="btn" to="/patterns">🔍 Patterns</Link>
                <Link className="btn" to="/replay">🎬 Growth Replay</Link>
                <Link className="btn ghost" to="/privacy">🔒 Your data</Link>
            </div>
            <div style={{ fontSize: '0.82rem', color: 'var(--amber-glow)', margin: '6px 0 18px', minHeight: '1em' }}>
                {syncStatus}
            </div>

            <div className="card">
                <div className="meta" style={{ marginBottom: 10 }}>
                    Add something GitHub can't see — a club event, a competition, anything.
                </div>
                <input
                    placeholder="What did you do?"
                    value={title}
                    onChange={e => setTitle(e.target.value)}
                />
                <textarea
                    placeholder="A short description"
                    rows={2}
                    value={description}
                    onChange={e => setDescription(e.target.value)}
                />
                <button className="primary" onClick={addManualEntry}>Add to timeline</button>
            </div>

            <h3 style={{ margin: '24px 0 14px' }}>Your timeline</h3>
            {entries === null ? (
                <div className="empty">Loading...</div>
            ) : entries.length === 0 ? (
                <div className="empty">Nothing here yet — try "Sync my GitHub activity" above.</div>
            ) : (
                <div className="thread">
                    {entries.map(e => (
                        <div key={e.id} className={`thread-node ${e.source === 'MANUAL' ? 'muted' : ''}`}>
                            <div className="card" style={e.completed ? { borderColor: 'rgba(79,227,193,0.35)' } : undefined}>
                                <div className="entry-title">
                                    {e.source === 'GITHUB_REPO' ? '💻 ' : '✍️ '}{e.title}
                                    {e.completed && <span className="tag" style={{ marginLeft: 8, background: 'rgba(79,227,193,0.15)', color: 'var(--success)', borderColor: 'rgba(79,227,193,0.3)' }}>✓ Completed</span>}
                                </div>
                                <div className="entry-desc">{e.description}</div>
                                <div className="entry-meta">
                                    <span className={`tag ${e.source === 'GITHUB_REPO' ? 'amber' : ''}`}>
                                        {e.source === 'GITHUB_REPO' ? 'GitHub' : 'Manual'}
                                    </span>
                                    <span className="meta">{new Date(e.date).toLocaleDateString()}</span>
                                </div>
                                {e.source === 'GITHUB_REPO' && (
                                    <button
                                        className="ghost"
                                        style={{ marginTop: 10, fontSize: '0.78rem', padding: '6px 12px' }}
                                        onClick={() => toggleComplete(e)}
                                    >
                                        {e.completed ? 'Mark as still in progress' : '✓ Mark as complete'}
                                    </button>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
