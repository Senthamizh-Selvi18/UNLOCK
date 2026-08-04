import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';

export default function Privacy() {
    const [confirmVisible, setConfirmVisible] = useState(false);
    const [confirmText, setConfirmText] = useState('');
    const [status, setStatus] = useState({ text: '', color: '' });
    const navigate = useNavigate();

    function downloadData() {
        api.exportData()
            .then(res => res.blob())
            .then(blob => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'unlock-my-data.json';
                a.click();
                URL.revokeObjectURL(url);
            });
    }

    function deleteAll() {
        if (confirmText !== 'DELETE') {
            setStatus({ text: 'Type DELETE exactly, in capital letters, to confirm.', color: 'var(--danger)' });
            return;
        }
        setStatus({ text: 'Deleting...', color: 'var(--ink-faint)' });
        api.deleteAllData()
            .then(res => {
                if (!res.ok) throw new Error();
                setStatus({ text: 'Everything has been deleted. Redirecting...', color: 'var(--success)' });
                setTimeout(() => navigate('/'), 1500);
            })
            .catch(() => setStatus({ text: 'Something went wrong — please try again.', color: 'var(--danger)' }));
    }

    return (
        <div className="page">
            <Link className="back" to="/dashboard">&larr; Back to dashboard</Link>

            <div className="card glow-card" style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{ fontSize: '2.2rem' }}>🔒</div>
                    <div>
                        <h2 style={{ marginBottom: 4 }}>Your data, your control</h2>
                        <div className="meta">
                            Nothing here is shared with your college, recruiters, or anyone else —
                            you always have a direct, unconditional way to take your data or remove it.
                        </div>
                    </div>
                </div>
            </div>

            <div className="card">
                <h3 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>📦 Download everything</h3>
                <p className="meta">Every entry, reflection, and pattern UNLOCK has for you, as one file you can keep for yourself.</p>
                <button className="primary" onClick={downloadData}>⬇ Download my data (JSON)</button>
            </div>

            <div className="card" style={{ borderColor: 'rgba(255,107,122,0.35)', background: 'rgba(255,107,122,0.05)' }}>
                <h3 style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--danger)' }}>⚠️ Delete everything</h3>
                <p className="meta">
                    This permanently removes your profile, timeline, reflections, and patterns.
                    There's no undo, and no waiting period — once you confirm, it's gone immediately.
                </p>
                <button
                    style={{ background: 'var(--danger)', color: '#2A0A0D', border: 'none' }}
                    onClick={() => setConfirmVisible(true)}
                >
                    🗑 Delete my account and all data
                </button>

                {confirmVisible && (
                    <div style={{ marginTop: 14 }}>
                        <p style={{ fontSize: '0.88rem' }}>
                            Type <strong>DELETE</strong> below to confirm. This cannot be undone.
                        </p>
                        <input
                            placeholder="Type DELETE to confirm"
                            value={confirmText}
                            onChange={e => setConfirmText(e.target.value)}
                        />
                        <div style={{ display: 'flex', gap: 8 }}>
                            <button
                                style={{ background: 'var(--danger)', color: '#2A0A0D', border: 'none' }}
                                onClick={deleteAll}
                            >
                                Yes, permanently delete everything
                            </button>
                            <button className="ghost" onClick={() => { setConfirmVisible(false); setConfirmText(''); }}>Cancel</button>
                        </div>
                    </div>
                )}
                {status.text && (
                    <div style={{ marginTop: 10, fontSize: '0.85rem', color: status.color }}>{status.text}</div>
                )}
            </div>
        </div>
    );
}
