import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Reflection from './pages/Reflection.jsx';
import Patterns from './pages/Patterns.jsx';
import Replay from './pages/Replay.jsx';
import Privacy from './pages/Privacy.jsx';

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Login />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/reflection" element={<Reflection />} />
                <Route path="/patterns" element={<Patterns />} />
                <Route path="/replay" element={<Replay />} />
                <Route path="/privacy" element={<Privacy />} />
            </Routes>
        </BrowserRouter>
    );
}
